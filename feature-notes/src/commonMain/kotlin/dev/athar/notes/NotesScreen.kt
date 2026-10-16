package dev.athar.notes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Unarchive
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.RestoreFromTrash
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.athar.ui.ConfirmDialog
import dev.athar.ui.EmptyHint
import dev.athar.ui.MarkdownText
import dev.athar.ui.Panel
import dev.athar.ui.PlatformBackHandler
import dev.athar.ui.t
import kotlinx.coroutines.delay

/** The notes of one [view]; [openId] opens a note straight away (from Home or search), and "" starts a new one. */
@Composable
fun NotesScreen(state: NotesState, view: NoteView, openId: String? = null, onOpenConsumed: () -> Unit = {}) {
    var open by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(openId) { if (openId != null) { open = openId; onOpenConsumed() } }
    val current = open
    if (current != null) {
        NoteEditor(state, state.notes.firstOrNull { it.id == current } ?: Note("", "", "", "", ""), onClose = { open = null })
        return
    }
    if (view == NoteView.Trash) { TrashList(state); return }
    var query by remember { mutableStateOf("") }
    val shown = notesIn(view, state.notes).filter { query.isBlank() || it.title.contains(query, true) || it.body.contains(query, true) || it.tags.any { x -> x.contains(query, true) } }
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        floatingActionButton = { FloatingActionButton(onClick = { open = "" }) { Icon(Icons.Rounded.Add, t("notes.new")) } },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                query, { query = it }, singleLine = true, placeholder = { Text(t("notes.search")) },
                leadingIcon = { Icon(Icons.Rounded.Search, null) },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(28.dp),
            )
            if (shown.isEmpty()) EmptyHint(if (state.notes.isEmpty()) t("notes.empty") else t("notes.nothing"), t("notes.emptyHint"))
            LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(shown, key = { it.id }) { n -> NoteCard(n) { open = n.id } }
            }
        }
    }
}

/** One note as a card: title with its pin and star, a short preview, the collection and tags. */
@Composable
fun NoteCard(n: Note, onClick: () -> Unit) {
    Panel(onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(n.title.ifBlank { t("notes.untitled") }, Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (n.favorite) Icon(Icons.Rounded.Star, t("notes.favorite"), tint = MaterialTheme.colorScheme.primary)
            if (n.pinned) Icon(Icons.Rounded.PushPin, t("notes.pinned"), tint = MaterialTheme.colorScheme.primary)
        }
        val preview = n.body.lineSequence().filter { it.isNotBlank() && !it.startsWith("#") }.take(3).joinToString(" ")
        if (preview.isNotBlank()) Text(preview, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
        val meta = listOfNotNull(n.collection.takeIf { it.isNotEmpty() }) + n.tags.map { "#$it" }
        if (meta.isNotEmpty()) Text(meta.joinToString("  "), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun TrashList(state: NotesState) {
    var confirmEmpty by remember { mutableStateOf(false) }
    var confirmOne by remember { mutableStateOf<Note?>(null) }
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(t("notes.trashHint"), Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (state.trashed.isNotEmpty()) TextButton(onClick = { confirmEmpty = true }) { Text(t("notes.emptyTrash"), color = MaterialTheme.colorScheme.error) }
        }
        if (state.trashed.isEmpty()) EmptyHint(t("notes.trashEmpty"), t("notes.trashEmptyHint"))
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.trashed, key = { it.id }) { n ->
                Panel {
                    Text(n.title.ifBlank { t("notes.untitled") }, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { state.restore(n.id) }) { Icon(Icons.Rounded.RestoreFromTrash, null); Text(t("notes.restore"), Modifier.padding(start = 6.dp)) }
                        TextButton(onClick = { confirmOne = n }) { Text(t("notes.deleteForever"), color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
    if (confirmEmpty) ConfirmDialog(t("notes.emptyTrash"), t("notes.emptyTrashAsk", state.trashed.size), t("notes.emptyTrash"), { confirmEmpty = false }) { state.emptyTrash() }
    confirmOne?.let { n -> ConfirmDialog(t("notes.deleteForever"), t("notes.deleteForeverAsk", n.title.ifBlank { t("notes.untitled") }), t("notes.deleteForever"), { confirmOne = null }) { state.deleteForever(n.id) } }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NoteEditor(state: NotesState, start: Note, onClose: () -> Unit) {
    var note by remember(start.id) { mutableStateOf(start) }
    var tags by remember(start.id) { mutableStateOf(start.tags.joinToString(" ")) }
    // A note you open is read first; a new one starts in edit mode. The pencil switches.
    var reading by remember(start.id) { mutableStateOf(start.id.isNotEmpty()) }
    var confirmTrash by remember { mutableStateOf(false) }
    // Typing is saved shortly after it stops, and at once when the editor closes; a note that is still empty is never written.
    val draft = remember(start.id) { NoteDraft({ state.save(it) }, start) }
    val withTags = { note.copy(tags = tags.split(' ', ',').map { it.trim().trimStart('#') }.filter { it.isNotEmpty() }) }
    LaunchedEffect(note, tags) {
        draft.edit(withTags())
        delay(600)
        draft.flush().let { if (note.id.isEmpty() && it.id.isNotEmpty()) note = note.copy(id = it.id, created = it.created) }
    }
    DisposableEffect(draft) { onDispose { draft.edit(withTags()); draft.flush() } }
    PlatformBackHandler(true, onClose)
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (note.title.isBlank()) t("notes.new") else note.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, t("back")) } },
                actions = {
                    IconButton(onClick = { reading = !reading }) { Icon(if (reading) Icons.Rounded.Edit else Icons.Rounded.Visibility, if (reading) t("edit") else t("read")) }
                    IconButton(onClick = { note = note.copy(favorite = !note.favorite) }) { Icon(Icons.Rounded.Star, t("notes.favorite"), tint = if (note.favorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                    IconButton(onClick = { note = note.copy(pinned = !note.pinned) }) { Icon(Icons.Rounded.PushPin, t("notes.pin"), tint = if (note.pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                    IconButton(onClick = { note = note.copy(archived = !note.archived) }) { Icon(if (note.archived) Icons.Rounded.Unarchive else Icons.Rounded.Archive, if (note.archived) t("notes.unarchive") else t("notes.archive")) }
                    IconButton(onClick = { if (note.id.isEmpty()) onClose() else confirmTrash = true }) { Icon(Icons.Rounded.Delete, t("notes.trash")) }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background),
            )
        },
    ) { padding ->
        if (reading) Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 20.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(note.title.ifBlank { t("notes.untitled") }, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            val meta = listOfNotNull(note.collection.takeIf { it.isNotEmpty() }) + tags.split(' ', ',').filter { it.isNotBlank() }.map { "#" + it.trimStart('#') }
            if (meta.isNotEmpty()) Text(meta.joinToString("  "), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
            MarkdownText(note.body, Modifier.padding(bottom = 32.dp))
        } else Column(Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(note.title, { note = note.copy(title = it) }, singleLine = true, placeholder = { Text(t("notes.title")) }, modifier = Modifier.fillMaxWidth(), textStyle = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(tags, { tags = it }, singleLine = true, placeholder = { Text(t("notes.tags")) }, modifier = Modifier.weight(1f))
                OutlinedTextField(note.collection, { note = note.copy(collection = it.take(40)) }, singleLine = true, placeholder = { Text(t("notes.collection")) }, modifier = Modifier.weight(1f))
            }
            OutlinedTextField(note.body, { note = note.copy(body = it) }, placeholder = { Text(t("notes.write")) }, modifier = Modifier.fillMaxWidth().weight(1f).padding(bottom = 12.dp))
        }
    }
    if (confirmTrash) ConfirmDialog(t("notes.trash"), t("notes.trashAsk"), t("notes.trash"), { confirmTrash = false }) { state.trash(note.id); onClose() }
}
