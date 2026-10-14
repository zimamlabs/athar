package dev.athar.journal

import dev.athar.core.Document
import dev.athar.core.Frontmatter
import dev.athar.core.VaultFiles
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/** The journal: one Markdown file a day in `journal/<year>/<date>.md`. */
class JournalRepository(private val files: VaultFiles) {
    private val fs get() = files.fs

    init { fs.createDirectories(files.dir("journal")) }

    private fun path(date: LocalDate) = files.dir("journal") / date.year.toString() / "$date.md"

    fun entry(date: LocalDate): JournalEntry? {
        val doc = Frontmatter.parse(files.read(path(date)) ?: return null)
        return JournalEntry(date, doc.body.trimEnd('\n'), doc.meta["mood"]?.toIntOrNull(), doc.list("tags"))
    }

    /** Saves the day's entry; an empty entry with no mood removes the file. */
    fun save(entry: JournalEntry) {
        val path = path(entry.date)
        if (entry.body.isBlank() && entry.mood == null && entry.tags.isEmpty()) { fs.delete(path); return }
        val meta = linkedMapOf("date" to entry.date.toString())
        entry.mood?.let { meta["mood"] = it.coerceIn(1, 5).toString() }
        if (entry.tags.isNotEmpty()) meta["tags"] = Frontmatter.formatList(entry.tags)
        files.write(path, Frontmatter.write(Document(meta, entry.body)))
    }

    /** Every day that has an entry, newest first. */
    fun dates(): List<LocalDate> = fs.listOrNull(files.dir("journal")).orEmpty().flatMap { year ->
        fs.listOrNull(year).orEmpty().mapNotNull { runCatching { LocalDate.parse(it.name.removeSuffix(".md")) }.getOrNull() }
    }.sortedDescending()

    fun entries(days: Int): List<JournalEntry> {
        val from = files.today().minus(days, DateTimeUnit.DAY)
        return dates().filter { it >= from }.mapNotNull { entry(it) }
    }
}
