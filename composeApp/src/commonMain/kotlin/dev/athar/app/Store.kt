package dev.athar.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.athar.clipboard.ClipboardMonitor
import dev.athar.clipboard.ClipboardSource
import dev.athar.clipboard.ClipboardState
import dev.athar.core.AppSettings
import dev.athar.core.Vault
import dev.athar.core.VaultImport
import dev.athar.journal.JournalState
import dev.athar.notes.NotesState
import okio.Path.Companion.toPath

/** What the app-level screens read: the vault, the settings and the date, plus each feature's own state. */
class AtharStore(val vault: Vault, val location: String, source: ClipboardSource) {
    val notesState = NotesState(vault.notesRepo)
    val journalState = JournalState(vault.journalRepo)
    var settings by mutableStateOf(vault.settings())
        private set
    val clipboardState = ClipboardState(vault.clipboardHistory, ClipboardMonitor(source, vault.clipboardHistory, { settings.clipboardOn }, { settings.clipboardExclusions }), source)
    var today by mutableStateOf(vault.today())
        private set
    /** The hour of the day (0..23), for the greeting. */
    val hour: Int get() = vault.files.stamp().substring(11, 13).toInt()
    /** Bumped when the vault changed on disk so lists that read it directly refresh. */
    var revision by mutableIntStateOf(0)
        private set

    /** Reads everything again (the vault may have changed on disk, or the date rolled over). */
    fun reload() {
        notesState.reload(); journalState.reload(); clipboardState.reload()
        today = vault.today(); settings = vault.settings(); revision++
    }

    /** True when the vault already holds anything, so an update from a version without the tour flags is not treated as a fresh install. */
    val hasData: Boolean get() = notesState.notes.isNotEmpty() || journalState.dates().isNotEmpty()

    /** Set to open the first-install tour again from Settings or About. */
    var tourOpen by mutableStateOf(false)

    fun saveSettings(value: AppSettings) { vault.saveSettings(value); settings = value }

    /** Copies the notes and journal of the vault at [path] into this one (see [VaultImport]) and reads everything again. */
    fun importFrom(path: String): VaultImport.Report = VaultImport.import(vault.fs, path.toPath(), vault.root).also { reload() }
}
