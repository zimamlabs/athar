package dev.athar.app

import dev.athar.ui.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import dev.athar.core.LegacyNames
import dev.athar.core.Vault
import dev.athar.core.VaultLocation
import dev.athar.core.zipVault
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.sink
import java.awt.FileDialog
import java.awt.Frame
import java.io.File

fun main() = application {
    val home = System.getProperty("user.home")
    // The vault is ~/Zimam/Athar, one folder for everything.
    val fs = FileSystem.SYSTEM
    val shared = File(home, "Zimam/Athar").absolutePath.toPath()
    val olds = emptyList<okio.Path>()
    val choice = VaultLocation.choose(fs, true, shared, olds, shared)
    val dir = File(choice.dir.toString()).also { it.mkdirs() }
    val store = AtharStore(Vault(fs, dir.absolutePath.toPath()), dir.absolutePath, dev.athar.clipboard.AwtClipboard)
    val actions = PlatformActions(removableOldFolders = { VaultLocation.removable(fs, choice.dir, shared, olds).map { it.toString() } }, removeOldFolder = { VaultLocation.remove(fs, it.toPath(), shared) }, migrationMessage = choice.message, shortcutHint = true, importCandidates = LegacyNames.importFolders.map { File(home, it).absolutePath }, exportVault = {
        val dialog = FileDialog(null as Frame?, "Save the vault as a zip", FileDialog.SAVE).apply { directory = File(dir, "exports").also { it.mkdirs() }.absolutePath; file = "Athar-vault.zip"; isVisible = true }
        val name = dialog.file
        if (name != null) File(dialog.directory, name).sink().buffer().use { zipVault(FileSystem.SYSTEM, store.vault.root, it) }
    })
    val icon = androidx.compose.ui.res.painterResource("icon.png")
    Window(onCloseRequest = ::exitApplication, title = "Athar", icon = icon, state = rememberWindowState(width = 1000.dp, height = 760.dp)) {
        CompositionLocalProvider(LocalPlatform provides actions) { AtharApp(store) }
    }
}
