package dev.athar.notes

import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val id: String,
    val title: String,
    val body: String,
    val created: String,
    val updated: String,
    val tags: List<String> = emptyList(),
    val pinned: Boolean = false,
    val favorite: Boolean = false,
    val archived: Boolean = false,
    /** The collection (a folder) the note is filed in; empty for none. */
    val collection: String = "",
    /** Header keys Athar does not know (Obsidian aliases, fields of other apps); written back unchanged. */
    val extra: Map<String, String> = emptyMap(),
)

/** Words in the note's text, for the writing stats. */
fun Note.wordCount(): Int = body.split(Regex("\\s+")).count { it.isNotEmpty() }

/** True when [other] says the same thing: same text and flags, whatever the stamps. Used to skip saves that change nothing. */
fun Note.sameContentAs(other: Note): Boolean =
    title == other.title && body.trimEnd('\n') == other.body.trimEnd('\n') && tags == other.tags && pinned == other.pinned &&
        favorite == other.favorite && archived == other.archived && collection == other.collection

/**
 * The note being edited and the last version written. [flush] writes when something changed (and the note is not
 * still empty), so the screen can call it after a pause, on back and when it leaves, and the last words are never lost.
 */
class NoteDraft(private val save: (Note) -> Note, start: Note) {
    var note: Note = start
        private set
    private var saved: Note = start

    val dirty: Boolean get() = (note.title.isNotBlank() || note.body.isNotBlank()) && (note.id.isEmpty() || !note.sameContentAs(saved))

    fun edit(n: Note) { note = n }

    fun flush(): Note {
        if (dirty) {
            val s = save(note)
            note = note.copy(id = s.id, created = s.created, updated = s.updated, extra = s.extra)
            saved = note
        }
        return note
    }
}
