package dev.athar.core

import dev.athar.clipboard.ClipboardItem
import dev.athar.journal.JournalEntry
import dev.athar.notes.Note
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class VaultTest {
    private class Fixed(var at: String = "2026-10-14T09:30:00Z") : Clock {
        override fun now(): Instant = Instant.parse(at)
    }

    private fun vault(fs: FakeFileSystem = FakeFileSystem(), root: String = "/v") = Vault(fs, root.toPath(), Fixed(), TimeZone.UTC)

    @Test fun frontmatterRoundTrips() {
        val doc = Document(linkedMapOf("title" to "A: tricky \"title\"", "tags" to Frontmatter.formatList(listOf("a", "b c", "d,e"))), "Body\nline two\n")
        val back = Frontmatter.parse(Frontmatter.write(doc))
        assertEquals("A: tricky \"title\"", back.meta["title"]); assertEquals(listOf("a", "b c", "d,e"), back.list("tags")); assertEquals("Body\nline two\n", back.body)
    }

    @Test fun settingsSurviveAndAnUnknownKeyIsIgnored() {
        val v = vault()
        v.saveSettings(AppSettings(language = "ar", digits = "arabic", clipboardOn = true, clipboardExclusions = listOf("x")))
        assertEquals("ar", v.settings().language); assertTrue(v.settings().clipboardOn)
        v.files.write("/v/.athar/settings.json".toPath(), """{"language":"en","somethingNew":1}""")
        assertEquals("en", v.settings().language)
    }

    @Test fun searchRanksTitleThenTagThenBodyAndSearchesJournalAndClipboard() {
        val notes = listOf(
            Note("1", "Budget plan", "x", "", "2026-01-01"), Note("2", "Ideas", "the budget is mentioned here budget", "", "2026-01-02"),
            Note("3", "Misc", "x", "", "2026-01-03", tags = listOf("budget")), Note("4", "budget", "x", "", "2026-01-04"),
            Note("5", "Old", "budget", "", "2026-01-05", archived = true),
        )
        val hits = search("Budget", notes, listOf(JournalEntry(LocalDate(2026, 1, 1), "my budget day")), listOf(ClipboardItem("c", "budget=100", "2026-01-01T00:00:00")))
        assertEquals(listOf("4", "1", "3", "2"), hits.filter { it.kind == "note" }.map { it.id }.take(4))
        assertTrue(hits.any { it.kind == "journal" } && hits.any { it.kind == "clipboard" })
        assertTrue(hits.any { it.id == "5" }, "archived notes are searchable")
        assertEquals(emptyList(), search("  ", notes, emptyList(), emptyList()))
    }

    @Test fun writingSummaryCountsWordsStreakAndTheWeek() {
        val today = LocalDate(2026, 10, 14)
        val s = writingSummary(
            listOf(Note("1", "a", "one two three", "2026-10-13T10:00:00", "2026-10-13T10:00:00"), Note("2", "b", "four", "2026-01-01T10:00:00", "2026-01-01T10:00:00")),
            listOf(JournalEntry(LocalDate(2026, 10, 14), "x y"), JournalEntry(LocalDate(2026, 10, 13), "z")), today,
        )
        assertEquals(7, s.words); assertEquals(2, s.streak); assertEquals(3 + 2 + 1, s.weekWords); assertEquals(2, s.notes)
    }

    @Test fun onThisDayFindsEarlierYearsOnly() {
        val dates = listOf(LocalDate(2025, 10, 14), LocalDate(2024, 10, 14), LocalDate(2026, 10, 14), LocalDate(2025, 10, 15))
        assertEquals(listOf(LocalDate(2025, 10, 14), LocalDate(2024, 10, 14)), onThisDay(dates, LocalDate(2026, 10, 14)))
    }

    @Test fun greetingKeysByHour() { assertEquals("home.morning", greetingKey(8)); assertEquals("home.afternoon", greetingKey(14)); assertEquals("home.evening", greetingKey(22)); assertEquals("home.evening", greetingKey(3)) }

    @Test fun daySummariesAndTotals() {
        val v = vault()
        v.saveJournal(JournalEntry(LocalDate(2026, 10, 13), "one two", 4))
        val n = v.saveNote(Note("", "n", "b", "", ""))
        val days = daySummaries(LocalDate(2026, 10, 12), LocalDate(2026, 10, 14), { v.journal(it) }, v.notes())
        assertEquals(3, days.size)
        val t = totals(days)
        assertEquals(2, t.words); assertEquals(1, t.journalDays); assertEquals(4.0, t.moodAverage); assertEquals(1, t.notes)
        assertEquals(n.id, v.notes().single().id)
    }

    // ---- import from Thabit -----------------------------------------------------------

    private fun fakeThabit(fs: FakeFileSystem) {
        fs.createDirectories("/old/Thabit/notes".toPath()); fs.createDirectories("/old/Thabit/journal/2026".toPath()); fs.createDirectories("/old/Thabit/tasks".toPath())
        fs.write("/old/Thabit/notes/a-aaaa1111.md".toPath()) { writeUtf8("---\nid: aaaa1111\ntitle: A\n---\nbody a\n") }
        fs.write("/old/Thabit/notes/b-bbbb2222.md".toPath()) { writeUtf8("---\nid: bbbb2222\ntitle: B\n---\nbody b\n") }
        fs.write("/old/Thabit/journal/2026/2026-03-01.md".toPath()) { writeUtf8("---\ndate: 2026-03-01\nmood: 4\n---\nhello\n") }
        fs.write("/old/Thabit/tasks/Tasks.md".toPath()) { writeUtf8("- [ ] not imported\n") }
    }

    @Test fun importCopiesNotesAndJournalOnlyAndNeverTouchesTheSource() {
        val fs = FakeFileSystem(); fakeThabit(fs)
        val before = fs.listRecursively("/old".toPath()).map { it.toString() to (if (fs.metadata(it).isRegularFile) fs.read(it) { readUtf8() } else "") }.toList()
        val v = vault(fs)
        val r = VaultImport.import(fs, "/old/Thabit".toPath(), v.root)
        assertTrue(r.ok, r.problems.toString()); assertEquals(3, r.copied)
        assertEquals(setOf("A", "B"), v.notes().map { it.title }.toSet())
        assertEquals("hello", v.journal(LocalDate(2026, 3, 1))?.body); assertEquals(4, v.journal(LocalDate(2026, 3, 1))?.mood)
        assertTrue(!fs.exists("/v/tasks".toPath()))
        val after = fs.listRecursively("/old".toPath()).map { it.toString() to (if (fs.metadata(it).isRegularFile) fs.read(it) { readUtf8() } else "") }.toList()
        assertEquals(before, after, "the source vault is unchanged")
    }

    @Test fun importTwiceCopiesNothingNew() {
        val fs = FakeFileSystem(); fakeThabit(fs); val v = vault(fs)
        VaultImport.import(fs, "/old/Thabit".toPath(), v.root)
        val r = VaultImport.import(fs, "/old/Thabit".toPath(), v.root)
        assertTrue(r.ok); assertEquals(0, r.copied); assertEquals(2, v.notes().size)
    }

    @Test fun aDifferentNoteWithTheSameNameIsKeptBesideNeverOverwritten() {
        val fs = FakeFileSystem(); fakeThabit(fs); val v = vault(fs)
        v.files.write("/v/notes/a-aaaa1111.md".toPath(), "---\nid: aaaa1111\ntitle: A\n---\nmine, edited here\n")
        val r = VaultImport.import(fs, "/old/Thabit".toPath(), v.root)
        assertTrue(r.ok); assertEquals(1, r.kept)
        assertEquals("mine, edited here", v.notes().first { it.id == "aaaa1111" && !it.body.contains("body a") }.body.trim())
        assertTrue(fs.list("/v/notes".toPath()).any { it.name.contains("(from Thabit)") })
    }

    @Test fun findOnlyOffersFoldersThatLookLikeVaults() {
        val fs = FakeFileSystem(); fakeThabit(fs); fs.createDirectories("/empty".toPath()); fs.createDirectories("/v".toPath())
        val found = VaultImport.find(fs, listOf("/old/Thabit".toPath(), "/empty".toPath(), "/nowhere".toPath(), "/v".toPath(), "/old/Thabit".toPath()), "/v".toPath())
        assertEquals(listOf("/old/Thabit".toPath()), found)
    }
}
