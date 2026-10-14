package dev.athar.core

import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FolderMigrationTest {
    private val fs = FakeFileSystem()
    private val old = "/sd/Athar".toPath()
    private val new = "/sd/Zimam/Athar".toPath()

    private fun put(path: String, text: String) { val p = path.toPath(); fs.createDirectories(p.parent!!); fs.write(p) { writeUtf8(text) } }
    private fun read(path: String) = fs.read(path.toPath()) { readUtf8() }

    @Test fun `copies every file, verifies, leaves the old folder and writes a marker`() {
        put("/sd/Athar/notes/a.md", "A"); put("/sd/Athar/.athar/settings.json", "{}"); put("/sd/Athar/journal/2026/2026-10-14.md", "J")
        val r = FolderMigration.copyAndVerify(fs, old, new)
        assertTrue(r.ok); assertEquals(3, r.copied)
        assertEquals("A", read("/sd/Zimam/Athar/notes/a.md")); assertEquals("J", read("/sd/Zimam/Athar/journal/2026/2026-10-14.md"))
        assertTrue(fs.exists("/sd/Athar/notes/a.md".toPath())); assertTrue(fs.exists(FolderMigration.marker(new, "Athar")))
        assertTrue(FolderMigration.safeToRemove(fs, old, new))
    }

    @Test fun `a clashing file is never overwritten and the old copy is kept beside it`() {
        put("/sd/Athar/tasks/Tasks.md", "old"); put("/sd/Zimam/Athar/tasks/Tasks.md", "new")
        val r = FolderMigration.copyAndVerify(fs, old, new)
        assertTrue(r.ok); assertEquals(1, r.kept)
        assertEquals("new", read("/sd/Zimam/Athar/tasks/Tasks.md")); assertEquals("old", read("/sd/Zimam/Athar/tasks/Tasks (from Athar).md"))
    }

    @Test fun `running twice copies nothing more and keeps no extra files`() {
        put("/sd/Athar/notes/a.md", "A"); put("/sd/Zimam/Athar/notes/a.md", "other")
        FolderMigration.copyAndVerify(fs, old, new)
        val again = FolderMigration.copyAndVerify(fs, old, new)
        assertTrue(again.ok); assertEquals(0, again.copied); assertEquals(0, again.kept)
        assertEquals(2, fs.list("/sd/Zimam/Athar/notes".toPath()).size)
    }

    @Test fun `a partial copy is detected and finished`() {
        put("/sd/Athar/notes/a.md", "A"); put("/sd/Athar/notes/b.md", "B"); put("/sd/Zimam/Athar/notes/a.md", "A")
        assertEquals(listOf("missing or different: notes/b.md"), FolderMigration.verify(fs, old, new))
        assertFalse(FolderMigration.safeToRemove(fs, old, new))
        assertTrue(FolderMigration.copyAndVerify(fs, old, new).ok)
        assertTrue(FolderMigration.safeToRemove(fs, old, new))
    }

    @Test fun `a changed destination file is a verify failure`() {
        put("/sd/Athar/notes/a.md", "A"); FolderMigration.copyAndVerify(fs, old, new)
        put("/sd/Athar/notes/a.md", "A changed")
        assertFalse(FolderMigration.safeToRemove(fs, old, new))
    }

    @Test fun `a leftover part file is ignored and the real file is copied`() {
        put("/sd/Athar/notes/a.md", "A"); put("/sd/Zimam/Athar/notes/a.md.part", "half")
        assertTrue(FolderMigration.copyAndVerify(fs, old, new).ok)
        assertEquals("A", read("/sd/Zimam/Athar/notes/a.md"))
    }

    @Test fun `choose keeps the old folder when the shared one cannot be used and uses the private vault only when no visible one exists`() {
        val private = "/data/Athar".toPath()
        put("/data/Athar/notes/p.md", "P")
        assertEquals(private, VaultLocation.choose(fs, false, new, listOf(old), private).dir)
        val c = VaultLocation.choose(fs, true, new, listOf(old), private)
        assertEquals(new, c.dir); assertEquals("P", read("/sd/Zimam/Athar/notes/p.md"))
        put("/sd/Athar/notes/v.md", "V")
        VaultLocation.choose(fs, true, "/sd/Zimam/Other".toPath(), listOf(old), private)
        assertFalse(fs.exists("/sd/Zimam/Other/notes/p.md".toPath())); assertEquals("V", read("/sd/Zimam/Other/notes/v.md"))
    }

    @Test fun `removal needs the shared folder in use and a fresh verify, and then removes only the old folder`() {
        put("/sd/Athar/notes/a.md", "A"); VaultLocation.choose(fs, true, new, listOf(old), "/data/Athar".toPath())
        assertEquals(listOf(old), VaultLocation.removable(fs, new, new, listOf(old)))
        assertEquals(emptyList(), VaultLocation.removable(fs, old, new, listOf(old)))
        put("/sd/Athar/notes/new.md", "added later")
        assertFalse(VaultLocation.remove(fs, old, new)); assertTrue(fs.exists("/sd/Athar/notes/a.md".toPath()))
        FolderMigration.copyAndVerify(fs, old, new)
        assertTrue(VaultLocation.remove(fs, old, new)); assertFalse(fs.exists(old)); assertEquals("A", read("/sd/Zimam/Athar/notes/a.md"))
    }
}
