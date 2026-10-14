package dev.athar.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.athar.core.daySummaries
import dev.athar.core.totals
import dev.athar.core.writingSummary
import dev.athar.journal.writingStats
import dev.athar.ui.Panel
import dev.athar.ui.n
import dev.athar.ui.t
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.minus

/** Writing insights: words, notes, journal days and mood over 7, 30 or 90 days, a bar a day, and the streaks. */
@Composable
fun InsightsScreen(store: AtharStore) {
    var days by remember { mutableStateOf(30) }
    val today = store.today
    val summaries = remember(days, store.revision, store.notesState.notes, today) {
        daySummaries(today.minus(days - 1, DateTimeUnit.DAY), today, { store.journalState.entry(it) }, store.notesState.notes)
    }
    val total = totals(summaries)
    val entries = store.journalState.dates().mapNotNull { store.journalState.entry(it) }
    val stats = writingStats(entries, today)
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(7, 30, 90).forEach { d -> FilterChip(days == d, { days = d }, { Text(t("insights.days", n(d))) }) }
        }
        Panel {
            Text(t("insights.period"), style = MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Tile(n(total.words), t("insights.journalWords"))
                Tile(n(total.notes), t("insights.notesMade"))
                Tile(n(total.journalDays), t("insights.journalDays"))
                Tile(total.moodAverage?.let { n((it * 10).toInt() / 10) + "." + n(((it * 10).toInt() % 10)) } ?: "–", t("insights.mood"))
            }
        }
        Panel {
            Text(t("insights.perDay"), style = MaterialTheme.typography.titleSmall)
            val max = (summaries.maxOfOrNull { it.journalWords + it.notesCreated * 20 } ?: 0).coerceAtLeast(1)
            Row(Modifier.fillMaxWidth().height(96.dp), horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.Bottom) {
                summaries.forEach { s ->
                    val v = s.journalWords + s.notesCreated * 20
                    Box(Modifier.weight(1f).fillMaxHeight(if (v == 0) 0.03f else (v.toFloat() / max).coerceIn(0.06f, 1f)).clip(RoundedCornerShape(3.dp)).background(if (v == 0) MaterialTheme.colorScheme.outlineVariant else MaterialTheme.colorScheme.primary))
                }
            }
        }
        Panel {
            Text(t("insights.streaks"), style = MaterialTheme.typography.titleSmall)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Tile(n(stats.streak), t("insights.streak"))
                Tile(n(stats.longestStreak), t("insights.longest"))
                Tile(n(stats.entries), t("insights.entries"))
                Tile(n(stats.words), t("insights.totalWords"))
            }
        }
        val all = writingSummary(store.notesState.notes.filter { !it.archived }, entries, today)
        Text(t("insights.footer", all.noteWords, all.journalWords).let { it }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Tile(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
