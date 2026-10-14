package dev.athar.core

import dev.athar.clipboard.ClipboardHistory
import dev.athar.journal.JournalEntry
import dev.athar.journal.JournalRepository
import dev.athar.notes.Note
import dev.athar.notes.NotesRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import okio.FileSystem
import okio.Path

/**
 * An Athar vault: one folder of plain files, readable in any editor.
 *
 * ```
 * notes/<title>-<id>.md            Markdown with a small header (id, title, tags, dates; pinned, favorite, archived, collection)
 * journal/<year>/<date>.md         one file a day: mood in the header, text below
 * clipboard/history.json           the copies Athar recorded (desktop)
 * .athar/                          settings and the trash
 * ```
 * `notes/` and `journal/` use the same format as Thabit and Dawem.
 */
class Vault(
    val fs: FileSystem,
    val root: Path,
    clock: Clock = Clock.System,
    zone: TimeZone = TimeZone.currentSystemDefault(),
) {
    val files = VaultFiles(fs, root, clock, zone)
    val notesRepo = NotesRepository(files)
    val journalRepo = JournalRepository(files)
    val clipboardHistory = ClipboardHistory(files)

    init { fs.createDirectories(files.trash) }

    fun today(): LocalDate = files.today()

    private val settingsPath get() = root / ".athar" / "settings.json"

    fun settings(): AppSettings = files.read(settingsPath)?.let { runCatching { files.json.decodeFromString(AppSettings.serializer(), it) }.getOrNull() } ?: AppSettings()

    fun saveSettings(settings: AppSettings) = files.write(settingsPath, files.json.encodeToString(AppSettings.serializer(), settings))

    fun notes(): List<Note> = notesRepo.notes()
    fun saveNote(note: Note): Note = notesRepo.save(note)
    fun trashNote(id: String) = notesRepo.trash(id)

    fun journal(date: LocalDate): JournalEntry? = journalRepo.entry(date)
    fun saveJournal(entry: JournalEntry) = journalRepo.save(entry)
    fun journalDates(): List<LocalDate> = journalRepo.dates()
}
