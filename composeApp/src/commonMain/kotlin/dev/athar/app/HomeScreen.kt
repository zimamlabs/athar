package dev.athar.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoStories
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.athar.core.greetingKey
import dev.athar.core.onThisDay
import dev.athar.core.writingSummary
import dev.athar.journal.JournalEntry
import dev.athar.notes.Note
import dev.athar.notes.NoteCard
import dev.athar.ui.Glow
import dev.athar.ui.Panel
import dev.athar.ui.SectionTitle
import dev.athar.ui.n
import dev.athar.ui.prettyDate
import dev.athar.ui.t

/** The title a captured text gets: its first line, trimmed of Markdown marks and cut short. */
fun captureTitle(text: String): String = text.lineSequence().firstOrNull { it.isNotBlank() }.orEmpty().trimStart('#', ' ', '-', '*').trim().take(60)

/** Home: a greeting, the capture box, your writing numbers, pinned and recent notes, today's journal and what you copied lately. */
@Composable
fun HomeScreen(store: AtharStore, nav: Nav) {
    val notes = store.notesState.notes
    val entries = store.journalState.dates().mapNotNull { store.journalState.entry(it) }
    val summary = writingSummary(notes.filter { !it.archived }, entries, store.today)
    val focus = remember { FocusRequester() }
    LaunchedEffect(nav.captureTick) { if (nav.captureTick > 0) runCatching { focus.requestFocus() } }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Glow(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(t(greetingKey(store.hour)), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Text(prettyDate(store.today) + " " + n(store.today.year), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                var text by remember { mutableStateOf("") }
                val save = {
                    if (text.isNotBlank()) {
                        store.notesState.save(Note("", captureTitle(text), text.trim(), "", ""))
                        text = ""
                    }
                }
                OutlinedTextField(
                    text, { text = it }, placeholder = { Text(t("home.capture")) }, modifier = Modifier.fillMaxWidth().focusRequester(focus),
                    minLines = 2, maxLines = 6, shape = RoundedCornerShape(20.dp),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (dev.athar.app.LocalPlatform.current.shortcutHint) Text(t("home.captureHint"), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant) else androidx.compose.foundation.layout.Spacer(Modifier.weight(1f))
                    Button(onClick = save, enabled = text.isNotBlank()) { Icon(Icons.Rounded.Check, null); Text(t("home.save"), Modifier.padding(start = 6.dp)) }
                }
            }
        }
        Panel {
            Text(t("home.writing"), style = MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Stat(n(summary.words), t("home.words"))
                Stat(n(summary.weekWords), t("home.thisWeek"))
                Stat(n(summary.streak), t("home.streak"))
                Stat(n(summary.notes), t("home.notes"))
            }
        }
        val pinned = notes.filter { it.pinned && !it.archived }.take(3)
        if (pinned.isNotEmpty()) { SectionTitle(t("nav.pinned")); pinned.forEach { nt -> NoteCard(nt) { nav.openNote(nt.id) } } }
        val recent = notes.filter { !it.archived && !it.pinned }.sortedByDescending { it.updated }.take(4)
        SectionTitle(t("nav.recent")) { AssistChip(onClick = { nav.go(Section.Notes) }, label = { Text(t("home.all")) }) }
        if (recent.isEmpty() && pinned.isEmpty()) Text(t("home.noNotes"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        recent.forEach { nt -> NoteCard(nt) { nav.openNote(nt.id) } }
        val todayEntry: JournalEntry? = store.journalState.entry(store.today)
        Panel(onClick = { nav.go(Section.Journal) }) {
            Row(verticalAlignment = Alignment.CenterVertically) { Icon(Icons.Rounded.AutoStories, null, tint = MaterialTheme.colorScheme.primary); Text(t("home.journalToday"), Modifier.padding(start = 10.dp), style = MaterialTheme.typography.titleSmall) }
            Text(todayEntry?.body?.takeIf { it.isNotBlank() } ?: t("home.journalEmpty"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        val back = onThisDay(store.journalState.dates(), store.today).firstOrNull()
        if (back != null) Panel(onClick = { nav.go(Section.Journal) }) {
            Text(t("home.onThisDay", back.year), style = MaterialTheme.typography.titleSmall)
            Text(store.journalState.entry(back)?.body.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
        if (store.clipboardState.supported && store.settings.clipboardOn) {
            val copies = store.clipboardState.items.take(3)
            Panel(onClick = { nav.go(Section.Clipboard) }) {
                Text(t("home.clipboard"), style = MaterialTheme.typography.titleSmall)
                if (copies.isEmpty()) Text(t("clip.emptyHint"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                copies.forEach { Text(it.text.lineSequence().first().take(80), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
