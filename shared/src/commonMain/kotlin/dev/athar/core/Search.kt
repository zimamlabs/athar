package dev.athar.core

import dev.athar.clipboard.ClipboardItem
import dev.athar.journal.JournalEntry
import dev.athar.notes.Note

/** One search result. [kind] is "note", "journal" or "clipboard". */
data class Hit(val kind: String, val title: String, val snippet: String, val id: String, val score: Int)

private fun snippetOf(text: String, q: String): String {
    val at = text.lowercase().indexOf(q).coerceAtLeast(0)
    return text.substring((at - 30).coerceAtLeast(0), (at + 70).coerceAtMost(text.length)).replace('\n', ' ').trim()
}

private fun occurrences(text: String, q: String): Int {
    var n = 0; var i = text.indexOf(q)
    while (i >= 0 && n < 20) { n++; i = text.indexOf(q, i + q.length) }
    return n
}

/**
 * Looks through notes (archived ones too), journal entries and clipboard items for [query], best first.
 * A title that is the query scores most, then a title that contains it, a tag, the collection, then how often the body says it;
 * pinned and favorite notes get a small lift, and the newest of equal scores comes first.
 */
fun search(query: String, notes: List<Note>, journal: List<JournalEntry>, clipboard: List<ClipboardItem>, limit: Int = 60): List<Hit> {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return emptyList()
    val hits = ArrayList<Pair<Hit, String>>()
    for (n in notes) {
        val title = n.title.lowercase(); val body = n.body.lowercase()
        var score = 0
        if (title == q) score += 100 else if (title.contains(q)) score += 60
        if (n.tags.any { it.lowercase() == q }) score += 50 else if (n.tags.any { it.lowercase().contains(q) }) score += 25
        if (n.collection.lowercase().contains(q)) score += 20
        score += occurrences(body, q).coerceAtMost(10) * 3
        if (score == 0) continue
        if (n.pinned) score += 5
        if (n.favorite) score += 3
        hits += Hit("note", n.title.ifBlank { n.id }, snippetOf(n.body, q), n.id, score) to n.updated
    }
    for (e in journal) {
        val c = occurrences(e.body.lowercase(), q)
        if (c == 0) continue
        hits += Hit("journal", e.date.toString(), snippetOf(e.body, q), e.date.toString(), 10 + c * 3) to e.date.toString()
    }
    for (c in clipboard) {
        val k = occurrences(c.text.lowercase(), q)
        if (k == 0) continue
        hits += Hit("clipboard", c.text.lineSequence().first().take(60), snippetOf(c.text, q), c.id, 5 + k * 2 + if (c.pinned) 5 else 0) to c.at
    }
    return hits.sortedWith(compareByDescending<Pair<Hit, String>> { it.first.score }.thenByDescending { it.second }).take(limit).map { it.first }
}
