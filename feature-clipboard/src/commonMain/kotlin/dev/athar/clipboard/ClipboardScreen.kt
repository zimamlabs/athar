package dev.athar.clipboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.athar.ui.ConfirmDialog
import dev.athar.ui.EmptyHint
import dev.athar.ui.Panel
import dev.athar.ui.n
import dev.athar.ui.t

/** The clipboard history: a visible recording state, search, copy back, pin, delete and clear. Desktop only. */
@Composable
fun ClipboardScreen(state: ClipboardState, recording: Boolean, onRecording: (Boolean) -> Unit) {
    if (!state.supported) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Panel { Text(t("clip.desktopOnly"), style = MaterialTheme.typography.titleMedium); Text(t("clip.desktopOnlyHint"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        }
        return
    }
    var query by remember { mutableStateOf("") }
    var confirmClear by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf<ClipboardItem?>(null) }
    val shown = state.items.filter { query.isBlank() || it.text.contains(query, true) }
    Column(Modifier.fillMaxSize()) {
        Panel(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), diamond = false) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text(if (recording) t("clip.recording") else t("clip.paused"), style = MaterialTheme.typography.titleMedium, color = if (recording) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(if (recording) t("clip.recordingHint") else t("clip.pausedHint"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Switch(recording, onRecording)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (recording) AssistChip(onClick = { state.skipNext() }, label = { Text(t("clip.skipNext")) })
                if (state.items.any { !it.pinned }) AssistChip(onClick = { confirmClear = true }, label = { Text(t("clip.clear")) })
            }
        }
        OutlinedTextField(
            query, { query = it }, singleLine = true, placeholder = { Text(t("clip.search")) }, leadingIcon = { Icon(Icons.Rounded.Search, null) },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), shape = RoundedCornerShape(28.dp),
        )
        if (shown.isEmpty()) EmptyHint(t("clip.empty"), t("clip.emptyHint"))
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(shown, key = { it.id }) { item ->
                Panel(diamond = false) {
                    Text(item.text, style = MaterialTheme.typography.bodyMedium, fontFamily = if (item.text.contains('\n') || item.text.contains("  ")) FontFamily.Monospace else null, maxLines = 6, overflow = TextOverflow.Ellipsis)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(item.at.replace('T', ' ').take(16), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        IconButton(onClick = { state.copyBack(item.text) }) { Icon(Icons.Rounded.ContentCopy, t("clip.copy")) }
                        IconButton(onClick = { state.pin(item.id, !item.pinned) }) { Icon(Icons.Rounded.PushPin, t("clip.pin"), tint = if (item.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                        IconButton(onClick = { confirmDelete = item }) { Icon(Icons.Rounded.Delete, t("clip.delete")) }
                    }
                }
            }
        }
    }
    if (confirmClear) ConfirmDialog(t("clip.clear"), t("clip.clearAsk", state.items.count { !it.pinned }), t("clip.clear"), { confirmClear = false }) { state.clear() }
    confirmDelete?.let { item -> ConfirmDialog(t("clip.delete"), t("clip.deleteAsk"), t("clip.delete"), { confirmDelete = null }) { state.delete(item.id) } }
}
