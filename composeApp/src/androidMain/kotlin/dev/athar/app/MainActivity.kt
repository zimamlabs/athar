package dev.athar.app

import dev.athar.ui.*
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import dev.athar.core.LegacyNames
import dev.athar.core.Vault
import dev.athar.core.VaultLocation
import dev.athar.core.zipVault
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.buffer
import okio.sink
import java.io.File

class MainActivity : ComponentActivity() {
    private lateinit var store: AtharStore

    private val exportZip = registerForActivityResult(ActivityResultContracts.CreateDocument("application/zip")) { uri ->
        uri?.let { writeZip(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        store = openStore()
        val actions = PlatformActions(
            exportVault = { if (inVisibleFolder()) exportHere() else exportZip.launch("Athar-vault.zip") },
            allowVisibleFolder = if (Build.VERSION.SDK_INT >= 30) ({ openAllFilesAccess() }) else null,
            visibleFolderOn = Build.VERSION.SDK_INT < 30 || Environment.isExternalStorageManager(),
            removableOldFolders = { removable() },
            removeOldFolder = { removeOld(it) },
            migrationMessage = migrationMessage,
            importCandidates = LegacyNames.importFolders.map { File(Environment.getExternalStorageDirectory(), it).absolutePath },
        )
        setContent { CompositionLocalProvider(LocalPlatform provides actions) { AtharApp(store) } }
    }

    override fun onResume() {
        super.onResume()
        // Files may have changed under us (a sync, a copy from the laptop), or the day rolled over.
        if (::store.isInitialized) store.reload()
    }

    /** True when the vault is in the phone's visible storage (the Zimam folder), so exports can go beside it instead of asking where. */
    private fun inVisibleFolder() = store.location.startsWith(Environment.getExternalStorageDirectory().absolutePath)

    /** Saves the zip into the vault's own `exports` folder, so every visible file stays under Zimam/Athar. */
    private fun exportHere() {
        runCatching {
            val dir = File(store.location, "exports").also { it.mkdirs() }
            val file = File(dir, "Athar-vault-${store.today}.zip")
            file.sink().buffer().use { zipVault(FileSystem.SYSTEM, store.vault.root, it) }
            android.widget.Toast.makeText(this, "Saved to ${file.absolutePath}", android.widget.Toast.LENGTH_LONG).show()
        }.onFailure { android.widget.Toast.makeText(this, "Could not save the export: ${it.message}", android.widget.Toast.LENGTH_LONG).show() }
    }

    private fun writeZip(uri: Uri) {
        runCatching {
            contentResolver.openOutputStream(uri)?.sink()?.buffer()?.use { zipVault(FileSystem.SYSTEM, store.vault.root, it) }
        }
    }

    private fun openAllFilesAccess() {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, Uri.parse("package:$packageName"))
        runCatching { startActivity(intent) }.onFailure { startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)) }
    }

    private var removable: () -> List<String> = { emptyList() }
    private var removeOld: (String) -> Boolean = { false }
    private var migrationMessage: String? = null

    /**
     * The vault is `Zimam/Athar` on the phone when All files access is allowed, else a private folder. Older folders
     * (`Athar`, `Athar`, and the private vault when no visible one exists) are copied in and checked, then left where
     * they are as a backup; if a copy cannot be verified the old folder stays in use. Nothing is deleted here.
     */
    private fun openStore(): AtharStore {
        val private = File(filesDir, "Athar")
        val sd = Environment.getExternalStorageDirectory()
        val fs = FileSystem.SYSTEM
        val shared = (File(sd, "Zimam/Athar")).absolutePath.toPath()
        val olds = emptyList<okio.Path>()
        val canShare = Build.VERSION.SDK_INT >= 30 && Environment.isExternalStorageManager()
        val choice = VaultLocation.choose(fs, canShare, shared, olds, private.absolutePath.toPath())
        migrationMessage = choice.message
        val all = olds + private.absolutePath.toPath()
        removable = { VaultLocation.removable(fs, choice.dir, shared, all).map { it.toString() } }
        removeOld = { p -> VaultLocation.remove(fs, p.toPath(), shared) }
        File(choice.dir.toString()).mkdirs()
        return AtharStore(Vault(fs, choice.dir), choice.dir.toString(), dev.athar.clipboard.NoClipboard)
    }
}
