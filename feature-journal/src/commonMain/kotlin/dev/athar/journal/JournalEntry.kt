package dev.athar.journal

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus

/** One day's journal: free text, a mood from 1 (low) to 5 (great), and tags. */
data class JournalEntry(val date: LocalDate, val body: String, val mood: Int? = null, val tags: List<String> = emptyList())

/** Average mood over entries that have one, or null. */
fun averageMood(entries: List<JournalEntry>): Double? = entries.mapNotNull { it.mood }.let { m -> if (m.isEmpty()) null else m.sum().toDouble() / m.size }

/** Days in a row, ending today, that have a journal entry; today still empty does not break it. */
fun journalStreak(days: Set<LocalDate>, today: LocalDate): Int {
    var d = if (today in days) today else today.minus(1, DateTimeUnit.DAY)
    var n = 0
    while (d in days) { n++; d = d.minus(1, DateTimeUnit.DAY) }
    return n
}
