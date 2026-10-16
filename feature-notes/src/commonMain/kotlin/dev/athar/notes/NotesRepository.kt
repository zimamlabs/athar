package dev.athar.notes

import dev.athar.core.Document
import dev.athar.core.Frontmatter
import dev.athar.core.VaultFiles
import dev.athar.core.newId
import dev.athar.core.slugOf
import okio.Path

/**
 * Notes as Markdown files in `notes/`, each with a small header (`id`, `title`, `created`, `updated`, `tags`, and `pinned`,
 * `favorite`, `archived`, `collection` when set). The first five keys are the same ones Thabit writes, so notes can
 * be copied between the apps. Deleting moves a note to the vault's trash, where it can be restored.
 */
class NotesRepository(private val files: VaultFiles) {
    private val fs get() = files.fs
    private val dir get() = files.dir("notes")

    init { fs.createDirectories(dir); fs.createDirectories(files.trash) }

    private companion object { val KNOWN = setOf("id", "title", "created", "updated", "tags", "pinned", "favorite", "archived", "collection") }

    private fun mdFiles(folder: Path): List<Path> = fs.listOrNull(folder).orEmpty().filter { it.name.endsWith(".md") }

    private fun order() = compareByDescending<Note> { it.pinned }.thenByDescending { it.updated }

    /** Every note that is not in the trash (archived ones included), pinned first, then newest. */
    fun notes(): List<Note> = mdFiles(dir).mapNotNull { parseNote(it) }.sortedWith(order())

    /** The notes in the trash, newest first. */
    fun trashed(): List<Note> = mdFiles(files.trash).mapNotNull { parseNote(it) }.sortedByDescending { it.updated }

    fun collections(): List<String> = notes().map { it.collection }.filter { it.isNotEmpty() }.distinct().sorted()

    private fun parseNote(path: Path): Note? {
        val doc = Frontmatter.parse(files.read(path) ?: return null)
        val id = doc.meta["id"] ?: path.name.removeSuffix(".md").substringAfterLast('-')
        val title = doc.meta["title"] ?: doc.body.lineSequence().firstOrNull { it.isNotBlank() }?.trimStart('#', ' ') ?: path.name.removeSuffix(".md")
        return Note(
            id, title, doc.body, doc.meta["created"].orEmpty(), doc.meta["updated"].orEmpty(), doc.list("tags"),
            pinned = doc.meta["pinned"] == "true", favorite = doc.meta["favorite"] == "true", archived = doc.meta["archived"] == "true",
            collection = doc.meta["collection"].orEmpty(),
            extra = doc.meta.filterKeys { it !in KNOWN },
        )
    }

    /**
     * Creates or updates a note, renaming its file when the title changes. A note that says the same as the file on
     * disk is not written at all (opening a note never changes it), header keys Athar does not know are kept, and
     * the new file is written before the old one is removed.
     */
    fun save(note: Note): Note {
        val id = note.id.ifEmpty { newId() }
        val existing = mdFiles(dir).filter { it.name.endsWith("-$id.md") }
        val current = existing.firstNotNullOfOrNull { parseNote(it) }
        if (current != null && current.sameContentAs(note)) return current
        val created = note.created.ifEmpty { files.stamp() }
        val saved = note.copy(id = id, created = created, updated = files.stamp(), extra = note.extra.ifEmpty { current?.extra.orEmpty() })
        val meta = linkedMapOf("id" to id, "title" to saved.title, "created" to created, "updated" to saved.updated, "tags" to Frontmatter.formatList(saved.tags))
        if (saved.pinned) meta["pinned"] = "true"
        if (saved.favorite) meta["favorite"] = "true"
        if (saved.archived) meta["archived"] = "true"
        if (saved.collection.isNotEmpty()) meta["collection"] = saved.collection
        saved.extra.forEach { (k, v) -> if (k !in meta) meta[k] = v }
        val target = dir / "${slugOf(saved.title)}-$id.md"
        files.write(target, Frontmatter.write(Document(meta, saved.body)))
        existing.filter { it != target }.forEach { fs.delete(it) }
        return saved
    }

    /** Moves a note to the vault's trash instead of deleting it. */
    fun trash(id: String) {
        mdFiles(dir).filter { it.name.endsWith("-$id.md") }.forEach { fs.atomicMove(it, files.trash / it.name) }
    }

    /** Puts a trashed note back; a note with the same name already there is never overwritten. */
    fun restore(id: String) {
        mdFiles(files.trash).filter { it.name.endsWith("-$id.md") }.forEach { f ->
            val back = dir / f.name
            if (fs.exists(back)) return@forEach
            fs.atomicMove(f, back)
        }
    }

    /** Removes one trashed note for good. */
    fun deleteForever(id: String) {
        mdFiles(files.trash).filter { it.name.endsWith("-$id.md") }.forEach { fs.delete(it) }
    }

    /** Removes everything in the trash for good; the screen asks first. */
    fun emptyTrash() { mdFiles(files.trash).forEach { fs.delete(it) } }
}
