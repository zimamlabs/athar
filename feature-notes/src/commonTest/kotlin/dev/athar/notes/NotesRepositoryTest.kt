package dev.athar.notes

import dev.athar.core.VaultFiles
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NotesRepositoryTest {
    private class Fixed(var at: String = "2026-03-01T10:00:00Z") : Clock {
        override fun now(): Instant = Instant.parse(at)
    }

    private val fs = FakeFileSystem()
    private val clock = Fixed()
    private val repo = NotesRepository(VaultFiles(fs, "/v".toPath(), clock, TimeZone.UTC))

    @Test fun aSavedNoteComesBackWithItsTagsAndBody() {
        val saved = repo.save(Note("", "Plan", "line one\n\nline two", "", "", listOf("work", "ideas")))
        val read = repo.notes().single()
        assertEquals(saved.id, read.id)
        assertEquals(listOf("work", "ideas"), read.tags)
        assertEquals("line one\n\nline two", read.body.trim())
    }

    @Test fun renamingKeepsOneFileAndTheSameId() {
        val first = repo.save(Note("", "Old name", "x", "", ""))
        repo.save(first.copy(title = "New name"))
        val files = fs.list("/v/notes".toPath()).map { it.name }
        assertEquals(1, files.size, files.toString())
        assertTrue(files.single().startsWith("new-name-"), files.toString())
        assertEquals(first.id, repo.notes().single().id)
    }

    @Test fun trashMovesTheFileInsteadOfDeletingIt() {
        val n = repo.save(Note("", "Bin me", "x", "", ""))
        repo.trash(n.id)
        assertEquals(emptyList(), repo.notes())
        assertEquals(1, fs.list("/v/.athar/trash".toPath()).size)
    }

    @Test fun pinnedFirstThenNewestUpdated() {
        repo.save(Note("", "Old", "x", "", ""))
        clock.at = "2026-03-02T10:00:00Z"
        repo.save(Note("", "New", "x", "", ""))
        clock.at = "2026-03-03T10:00:00Z"
        repo.save(Note("", "Pinned but old", "x", "", "", pinned = true))
        assertEquals(listOf("Pinned but old", "New", "Old"), repo.notes().map { it.title })
    }

    @Test fun aFileWithoutAHeaderStillLoadsWithTitleFromItsFirstLine() {
        fs.createDirectories("/v/notes".toPath())
        fs.write("/v/notes/hello-abc12345.md".toPath()) { writeUtf8("# Hello there\n\nbody") }
        val n = repo.notes().single()
        assertEquals("Hello there", n.title)
        assertEquals("abc12345", n.id)
    }

    @Test fun trashedNotesCanBeRestoredAndEmptied() {
        val a = repo.save(Note("", "Restore me", "x", "", ""))
        val b = repo.save(Note("", "Lose me", "y", "", ""))
        repo.trash(a.id); repo.trash(b.id)
        assertEquals(setOf(a.id, b.id), repo.trashed().map { it.id }.toSet())
        repo.restore(a.id)
        assertEquals(listOf(a.id), repo.notes().map { it.id })
        repo.emptyTrash()
        assertEquals(emptyList(), repo.trashed())
        assertEquals(listOf(a.id), repo.notes().map { it.id })
    }

    @Test fun restoreNeverOverwritesANoteWithTheSameName() {
        val a = repo.save(Note("", "Same", "first", "", ""))
        repo.trash(a.id)
        fs.write("/v/notes/${fs.list("/v/.athar/trash".toPath()).single().name}".toPath()) { writeUtf8("---\nid: ${a.id}\ntitle: Same\n---\nkept") }
        repo.restore(a.id)
        assertEquals("kept", repo.notes().single().body.trim())
        assertEquals(1, repo.trashed().size)
    }

    @Test fun favoriteArchivedAndCollectionSurviveTheFile() {
        val n = repo.save(Note("", "Filed", "x", "", "", favorite = true, archived = true, collection = "Work"))
        val read = repo.notes().single()
        assertTrue(read.favorite && read.archived)
        assertEquals("Work", read.collection)
        assertEquals(listOf("Work"), repo.collections())
        assertEquals(n.id, read.id)
    }

    @Test fun theHeaderKeysThabitAndDawemWriteStayReadable() {
        fs.createDirectories("/v/notes".toPath())
        fs.write("/v/notes/old-abcd1234.md".toPath()) { writeUtf8("---\nid: abcd1234\ntitle: Old\ncreated: 2026-01-01T09:00:00\nupdated: 2026-01-02T09:00:00\ntags: [a, b]\npinned: true\n---\nbody\n") }
        val n = repo.notes().single()
        assertEquals(listOf("a", "b"), n.tags); assertTrue(n.pinned); assertEquals("2026-01-02T09:00:00", n.updated)
        // saving it back keeps every one of those keys
        repo.save(n)
        val text = fs.read(fs.list("/v/notes".toPath()).single()) { readUtf8() }
        listOf("id: abcd1234", "title: Old", "created: 2026-01-01T09:00:00", "tags: [a, b]", "pinned: true").forEach { assertTrue(text.contains(it), it) }
    }

    @Test fun viewsFilterTheList() {
        val all = listOf(
            Note("1", "a", "", "", "2026-01-01", pinned = true), Note("2", "b", "", "", "2026-01-02", favorite = true, tags = listOf("t")),
            Note("3", "c", "", "", "2026-01-03", archived = true), Note("4", "d", "", "", "2026-01-04", collection = "Work"),
        )
        assertEquals(listOf("1", "2", "4"), notesIn(NoteView.All, all).map { it.id })
        assertEquals(listOf("1"), notesIn(NoteView.Pinned, all).map { it.id })
        assertEquals(listOf("2"), notesIn(NoteView.Favorites, all).map { it.id })
        assertEquals(listOf("3"), notesIn(NoteView.Archived, all).map { it.id })
        assertEquals(listOf("4"), notesIn(NoteView.Collection("Work"), all).map { it.id })
        assertEquals(listOf("2"), notesIn(NoteView.Tag("t"), all).map { it.id })
        assertEquals(listOf("4", "2", "1"), notesIn(NoteView.Recent, all).map { it.id })
    }
}
