package dev.athar.core

import dev.athar.journal.JournalEntry
import dev.athar.journal.averageMood
import dev.athar.journal.journalStreak
import dev.athar.journal.wordsIn
import dev.athar.notes.Note
import dev.athar.notes.wordCount
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** "morning", "afternoon" or "evening" for an hour 0..23 (the key of the greeting). */
fun greetingKey(hour: Int): String = when (hour) { in 5..11 -> "home.morning"; in 12..16 -> "home.afternoon"; else -> "home.evening" }

/** Earlier years' entries for today's month and day, newest year first. */
fun onThisDay(entryDates: Collection<LocalDate>, today: LocalDate): List<LocalDate> =
    entryDates.filter { it.monthNumber == today.monthNumber && it.dayOfMonth == today.dayOfMonth && it.year < today.year }.sortedDescending()

/** The numbers on the Home writing card. */
data class WritingSummary(val words: Int, val noteWords: Int, val journalWords: Int, val notes: Int, val entries: Int, val streak: Int, val weekWords: Int)

/** Words across notes and journal, the journal streak, and the words written in the last 7 days (journal by date, notes by last edit). */
fun writingSummary(notes: List<Note>, entries: List<JournalEntry>, today: LocalDate): WritingSummary {
    val noteWords = notes.sumOf { it.wordCount() }
    val journalWords = entries.sumOf { wordsIn(it.body) }
    val from = today.minus(6, DateTimeUnit.DAY)
    val weekJournal = entries.filter { it.date in from..today }.sumOf { wordsIn(it.body) }
    val weekNotes = notes.filter { it.updated.take(10).let { d -> d >= from.toString() && d <= today.toString() } }.sumOf { it.wordCount() }
    return WritingSummary(noteWords + journalWords, noteWords, journalWords, notes.size, entries.size, journalStreak(entries.map { it.date }.toSet(), today), weekJournal + weekNotes)
}

/** What happened on one day, for the writing insights. */
data class DaySummary(val date: LocalDate, val journalWords: Int, val mood: Int?, val notesCreated: Int)

/** One summary per day from [from] to [to], oldest first. */
fun daySummaries(from: LocalDate, to: LocalDate, journal: (LocalDate) -> JournalEntry?, notes: List<Note>): List<DaySummary> {
    val byDay = notes.groupingBy { it.created.take(10) }.eachCount()
    val out = ArrayList<DaySummary>()
    var d = from; var guard = 0
    while (d <= to && guard++ < 4000) {
        val j = journal(d)
        out += DaySummary(d, j?.body?.let(::wordsIn) ?: 0, j?.mood, byDay[d.toString()] ?: 0)
        d = d.plus(1, DateTimeUnit.DAY)
    }
    return out
}

data class PeriodTotals(val days: Int, val journalDays: Int, val words: Int, val moodAverage: Double?, val notes: Int)

fun totals(days: List<DaySummary>): PeriodTotals {
    val moods = days.mapNotNull { it.mood }
    return PeriodTotals(days.size, days.count { it.journalWords > 0 || it.mood != null }, days.sumOf { it.journalWords }, if (moods.isEmpty()) null else moods.sum().toDouble() / moods.size, days.sumOf { it.notesCreated })
}
