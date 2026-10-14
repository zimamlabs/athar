package dev.athar.journal

import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.until

/** The cells of a month grid, weeks from Monday: null pads the days before the 1st and after the last day. */
fun monthGrid(year: Int, month: Int): List<LocalDate?> {
    val first = LocalDate(year, month, 1)
    val next = first.plus(1, DateTimeUnit.MONTH)
    val days = (0 until first.until(next, DateTimeUnit.DAY)).map { first.plus(it, DateTimeUnit.DAY) }
    val cells = ArrayList<LocalDate?>()
    repeat(first.dayOfWeek.ordinal) { cells += null }
    cells += days
    while (cells.size % 7 != 0) cells += null
    return cells
}

/** The nearest day with an entry before ([forward] false) or after [day], or null. */
fun neighbourEntry(dates: Collection<LocalDate>, day: LocalDate, forward: Boolean): LocalDate? =
    if (forward) dates.filter { it > day }.minOrNull() else dates.filter { it < day }.maxOrNull()

/** Words in the text of an entry. */
fun wordsIn(text: String): Int = text.split(Regex("\\s+")).count { it.isNotEmpty() }

/** What the writing stats card shows. */
data class WritingStats(val entries: Int, val words: Int, val streak: Int, val longestStreak: Int, val moodAverage: Double?)

fun writingStats(entries: List<JournalEntry>, today: LocalDate): WritingStats {
    val days = entries.map { it.date }.toSet()
    var longest = 0
    var run = 0
    var prev: LocalDate? = null
    for (d in days.sorted()) {
        run = if (prev != null && prev.plus(1, DateTimeUnit.DAY) == d) run + 1 else 1
        longest = maxOf(longest, run); prev = d
    }
    return WritingStats(entries.size, entries.sumOf { wordsIn(it.body) }, journalStreak(days, today), longest, averageMood(entries))
}
