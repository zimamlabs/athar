package dev.athar.app

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.FolderSpecial
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import dev.athar.core.VaultImport
import dev.athar.ui.*
import okio.Path.Companion.toPath

@Composable
fun SettingsScreen(store: AtharStore) {
    val platform = LocalPlatform.current
    val s = store.settings
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(bottom = 32.dp)) {
        Header(t("settings.appearance"))
        Chips(listOf("system" to t("settings.system"), "light" to t("settings.light"), "dark" to t("settings.dark")), s.mode) { store.saveSettings(s.copy(mode = it)) }
        Row(Modifier.padding(start = 24.dp, end = 24.dp, top = 12.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Palettes.forEach { p ->
                val on = s.palette == p.id
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.clickable { store.saveSettings(s.copy(palette = p.id)) }) {
                    Box(
                        Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF000000 or p.swatch)).then(if (on) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) { if (on) Icon(Icons.Rounded.Check, null, tint = Color.Black.copy(alpha = 0.7f)) }
                    Text(p.label, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(top = 4.dp))
                }
            }
        }
        Header(t("settings.language"))
        Chips(listOf("system" to t("settings.system"), "en" to "English", "ar" to "العربية"), s.language) { store.saveSettings(s.copy(language = it)) }
        Header(t("settings.digits"))
        Chips(listOf("western" to "0 1 2 3", "arabic" to "٠ ١ ٢ ٣"), s.digits) { store.saveSettings(s.copy(digits = it)) }

        if (store.clipboardState.supported) {
            Header(t("settings.clipboard"))
            Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(t("settings.clipRecord"), style = MaterialTheme.typography.bodyLarge)
                    Text(t("settings.clipRecordHint"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(s.clipboardOn, { store.saveSettings(s.copy(clipboardOn = it)) })
            }
            var text by remember(s.clipboardExclusions) { mutableStateOf(s.clipboardExclusions.joinToString("\n")) }
            OutlinedTextField(
                text, { text = it; store.saveSettings(s.copy(clipboardExclusions = it.lines().map { l -> l.trim() }.filter { l -> l.isNotEmpty() })) },
                label = { Text(t("settings.clipExclusions")) }, supportingText = { Text(t("settings.clipExclusionsHint")) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp), minLines = 2, maxLines = 5,
            )
        }

        Header(t("settings.vault"))
        Text(store.location, Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.bodyLarge)
        Text(t("settings.vaultHint", store.notesState.notes.size, store.journalState.dates().size), Modifier.padding(horizontal = 24.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (platform.allowVisibleFolder != null && !platform.visibleFolderOn) Action(Icons.Rounded.FolderSpecial, t("settings.visible"), t("settings.visibleHint")) { platform.allowVisibleFolder.invoke() }
        platform.migrationMessage?.let { Text(it, Modifier.padding(horizontal = 24.dp, vertical = 8.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error) }
        OldFolders(platform)

        Header(t("settings.import"))
        ImportSection(store, platform)

        Header(t("settings.move"))
        Action(Icons.Rounded.IosShare, t("settings.export"), t("settings.exportHint")) { platform.exportVault() }
        Action(Icons.Rounded.FolderSpecial, t("settings.tour"), t("settings.tourHint")) { store.tourOpen = true }
        Header(t("nav.about"))
        Text("${Brand.NAME} ${Brand.VERSION}  ·  by ${Brand.MAKER}", Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.bodyLarge)
        Text(t("settings.tagline"), Modifier.padding(horizontal = 24.dp, vertical = 4.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Import from Thabit: the vaults found in the usual places, or a folder path typed here. Copies only, with a report. */
@Composable
fun ImportSection(store: AtharStore, platform: PlatformActions) {
    val found = remember(store.revision) { VaultImport.find(store.vault.fs, platform.importCandidates.map { it.toPath() }, store.vault.root).map { it.toString() } }
    var path by remember { mutableStateOf("") }
    var report by remember { mutableStateOf<VaultImport.Report?>(null) }
    var busy by remember { mutableStateOf(false) }
    val run: (String) -> Unit = { p -> busy = true; report = store.importFrom(p); busy = false }
    Text(t("import.hint"), Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    if (found.isEmpty()) Text(t("import.none"), Modifier.padding(horizontal = 24.dp, vertical = 6.dp), style = MaterialTheme.typography.bodySmall)
    found.forEach { p ->
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(p, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
            Button(onClick = { run(p) }, enabled = !busy) { Icon(Icons.Rounded.Download, null); Text(t("import.do"), Modifier.padding(start = 6.dp)) }
        }
    }
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(path, { path = it }, singleLine = true, placeholder = { Text(t("import.path")) }, modifier = Modifier.weight(1f))
        OutlinedButton(onClick = { run(path.trim()) }, enabled = path.isNotBlank() && !busy) { Text(t("import.do")) }
    }
    report?.let { r ->
        Text(
            if (r.ok) t("import.done", r.copied, r.kept) else t("import.problems", r.problems.size),
            Modifier.padding(horizontal = 24.dp, vertical = 6.dp), style = MaterialTheme.typography.bodyMedium,
            color = if (r.ok) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        if (!r.ok) r.problems.take(4).forEach { Text(it, Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.bodySmall) }
        Text(t("import.safe"), Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Chips(options: List<Pair<String, String>>, selected: String, onPick: (String) -> Unit) {
    Row(Modifier.padding(horizontal = 24.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (id, label) -> FilterChip(selected == id, { onPick(id) }, { Text(label) }) }
    }
}

/** Old folders that are fully copied into the Zimam folder and checked; removing one is the user's choice, and checked again first. */
@Composable
private fun OldFolders(platform: PlatformActions) {
    var tick by remember { mutableStateOf(0) }
    val olds = remember(tick) { platform.removableOldFolders() }
    var asking by remember { mutableStateOf<String?>(null) }
    if (olds.isEmpty()) return
    Header(t("settings.oldFolders"))
    Text(t("settings.oldFoldersHint"), Modifier.padding(horizontal = 24.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    olds.forEach { path -> Action(Icons.Rounded.FolderSpecial, t("settings.removeFolder", path), t("settings.removeFolderHint")) { asking = path } }
    asking?.let { path -> ConfirmDialog(t("settings.removeOldTitle"), t("settings.removeOldAsk", path), t("settings.remove"), { asking = null }) { platform.removeOldFolder(path); tick++ } }
}

@Composable
private fun Header(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 24.dp, bottom = 6.dp))
}

@Composable
private fun Action(icon: ImageVector, label: String, note: String, onClick: () -> Unit) {
    Row(Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Column(Modifier.padding(start = 20.dp)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
