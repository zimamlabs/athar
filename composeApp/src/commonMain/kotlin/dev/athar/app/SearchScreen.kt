package dev.athar.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.athar.core.search
import dev.athar.ui.EmptyHint
import dev.athar.ui.Panel
import dev.athar.ui.t
import kotlinx.datetime.LocalDate

/** One search over notes, journal entries and clipboard items, best matches first. */
@Composable
fun SearchScreen(store: AtharStore, nav: Nav) {
    var query by remember { mutableStateOf("") }
    val hits = remember(query, store.notesState.notes, store.revision, store.clipboardState.items) {
        val entries = store.journalState.dates().mapNotNull { store.journalState.entry(it) }
        search(query, store.notesState.notes, entries, store.clipboardState.items)
    }
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            query, { query = it }, singleLine = true, placeholder = { Text(t("search.hint")) }, leadingIcon = { Icon(Icons.Rounded.Search, null) },
            modifier = Modifier.fillMaxWidth().padding(16.dp), shape = RoundedCornerShape(28.dp),
        )
        if (query.isNotBlank() && hits.isEmpty()) EmptyHint(t("notes.nothing"), t("search.none"))
        LazyColumn(contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(hits, key = { it.kind + it.id }) { h ->
                Panel(onClick = {
                    when (h.kind) {
                        "note" -> nav.openNote(h.id)
                        "journal" -> nav.go(Section.Journal)
                        else -> nav.go(Section.Clipboard)
                    }
                }) {
                    Text(t("search.kind." + h.kind), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    Text(h.title, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    if (h.snippet.isNotBlank()) Text(h.snippet, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
