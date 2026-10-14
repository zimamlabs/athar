package dev.athar.journal

import dev.athar.core.VaultFiles
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class JournalRepositoryTest {
    private val clock = object : Clock { override fun now(): Instant = Instant.parse("2026-03-10T08:00:00Z") }
    private val fs = FakeFileSystem()
    private val repo = JournalRepository(VaultFiles(fs, "/v".toPath(), clock, TimeZone.UTC))
    private val day = LocalDate(2026, 3, 9)

    @Test fun anEntryKeepsItsMoodAndTags() {
        repo.save(JournalEntry(day, "a long day", 4, listOf("work")))
        assertEquals(JournalEntry(day, "a long day", 4, listOf("work")), repo.entry(day))
    }

    @Test fun moodIsKeptInsideOneToFive() {
        repo.save(JournalEntry(day, "x", 9))
        assertEquals(5, repo.entry(day)?.mood)
    }

    @Test fun savingAnEmptyEntryRemovesTheFile() {
        repo.save(JournalEntry(day, "text"))
        repo.save(JournalEntry(day, "  "))
        assertNull(repo.entry(day))
        assertEquals(emptyList(), repo.dates())
    }

    @Test fun datesAreNewestFirstAcrossYears() {
        repo.save(JournalEntry(LocalDate(2025, 12, 31), "a"))
        repo.save(JournalEntry(LocalDate(2026, 1, 2), "b"))
        repo.save(JournalEntry(LocalDate(2026, 3, 9), "c"))
        assertEquals(listOf(LocalDate(2026, 3, 9), LocalDate(2026, 1, 2), LocalDate(2025, 12, 31)), repo.dates())
    }

    @Test fun entriesLooksBackFromTheVaultClocksToday() {
        repo.save(JournalEntry(LocalDate(2026, 3, 9), "recent"))
        repo.save(JournalEntry(LocalDate(2026, 1, 1), "old"))
        assertEquals(listOf("recent"), repo.entries(7).map { it.body })
    }

    @Test fun streakAndAverageMood() {
        val days = setOf(LocalDate(2026, 3, 8), LocalDate(2026, 3, 9))
        assertEquals(2, journalStreak(days, LocalDate(2026, 3, 10)))
        assertEquals(0, journalStreak(days, LocalDate(2026, 3, 12)))
        assertEquals(3.0, averageMood(listOf(JournalEntry(day, "", 2), JournalEntry(day, "", 4), JournalEntry(day, "", null))))
    }

    @Test fun aMonthGridStartsOnMondayAndPadsToWholeWeeks() {
        val cells = monthGrid(2026, 10) // 1 Oct 2026 is a Thursday
        assertEquals(0, cells.size % 7)
        assertEquals(3, cells.takeWhile { it == null }.size)
        assertEquals(31, cells.filterNotNull().size)
        assertEquals(LocalDate(2026, 10, 1), cells[3])
    }

    @Test fun previousAndNextEntrySkipEmptyDays() {
        val dates = listOf(LocalDate(2026, 3, 1), LocalDate(2026, 3, 5), LocalDate(2026, 3, 9))
        assertEquals(LocalDate(2026, 3, 1), neighbourEntry(dates, LocalDate(2026, 3, 5), forward = false))
        assertEquals(LocalDate(2026, 3, 9), neighbourEntry(dates, LocalDate(2026, 3, 5), forward = true))
        assertEquals(null, neighbourEntry(dates, LocalDate(2026, 3, 9), forward = true))
        assertEquals(LocalDate(2026, 3, 9), neighbourEntry(dates, LocalDate(2026, 3, 6), forward = true))
    }

    @Test fun writingStatsCountWordsStreaksAndMood() {
        val e = listOf(
            JournalEntry(LocalDate(2026, 3, 1), "one two three", 4), JournalEntry(LocalDate(2026, 3, 2), "four five", 2),
            JournalEntry(LocalDate(2026, 3, 3), "six", null), JournalEntry(LocalDate(2026, 3, 8), "seven eight", null),
        )
        val s = writingStats(e, LocalDate(2026, 3, 3))
        assertEquals(4, s.entries); assertEquals(8, s.words); assertEquals(3, s.streak); assertEquals(3, s.longestStreak); assertEquals(3.0, s.moodAverage)
        assertEquals(0, writingStats(e, LocalDate(2026, 3, 20)).streak)
    }
}
