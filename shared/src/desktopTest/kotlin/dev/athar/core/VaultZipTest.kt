package dev.athar.core

import okio.Buffer
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import java.util.zip.ZipInputStream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import dev.athar.notes.Note

class VaultZipTest {
    @Test fun exportHasTheVaultFilesButNotTheTrash() {
        val fs = FakeFileSystem()
        val vault = Vault(fs, "/v".toPath())
        vault.saveNote(Note("", "Keep me", "body", "", ""))
        vault.saveJournal(dev.athar.journal.JournalEntry(kotlinx.datetime.LocalDate(2026, 1, 1), "hi"))
        val trashed = vault.saveNote(Note("", "Bin me", "body", "", ""))
        vault.trashNote(trashed.id)

        val buffer = Buffer()
        zipVault(fs, "/v".toPath(), buffer)
        val names = ArrayList<String>()
        ZipInputStream(buffer.inputStream()).use { zip -> generateSequence { zip.nextEntry }.forEach { names += it.name } }

        assertTrue(names.any { it.startsWith("Athar/notes/keep-me-") }, names.toString())
        assertTrue("Athar/journal/2026/2026-01-01.md" in names, names.toString())
        assertEquals(0, names.count { it.contains("bin-me") }, "the trash stays out of an export")
    }

    @Test fun earlierExportsAreNotZippedIntoTheNextOne() {
        val fs = FakeFileSystem()
        val vault = Vault(fs, "/v".toPath())
        vault.saveNote(Note("", "Keep me", "body", "", ""))
        fs.createDirectories("/v/exports".toPath()); fs.write("/v/exports/Athar-vault-2026-10-14.zip".toPath()) { writeUtf8("zip") }
        val buffer = Buffer()
        zipVault(fs, "/v".toPath(), buffer)
        val names = ArrayList<String>()
        ZipInputStream(buffer.inputStream()).use { zip -> generateSequence { zip.nextEntry }.forEach { names += it.name } }
        assertTrue(names.any { it.startsWith("Athar/notes/keep-me-") }, names.toString())
        assertEquals(0, names.count { it.contains("exports") }, names.toString())
    }
}
