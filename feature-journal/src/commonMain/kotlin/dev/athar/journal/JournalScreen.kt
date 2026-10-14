package dev.athar.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.athar.ui.EmptyHint
import dev.athar.ui.LocalLang
import dev.athar.ui.Panel
import dev.athar.ui.dayName
import dev.athar.ui.monthTitle
import dev.athar.ui.n
import dev.athar.ui.prettyDate
import dev.athar.ui.t
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.minus
import kotlinx.datetime.plus

/** The journal: a month calendar (a dot marks the days you wrote), the picked day to read or write, previous and next entry, and a look back. */
@Composable
fun JournalScreen(state: JournalState, today: LocalDate) {
    var day by remember { mutableStateOf(today) }
    var month by remember { mutableStateOf(LocalDate(today.year, today.monthNumber, 1)) }
    val dates = state.dates()
    val written = dates.toSet()
    val lang = LocalLang.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Panel(diamond = false) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = month.minus(1, DateTimeUnit.MONTH) }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowLeft, t("journal.prevMonth")) }
                Text(monthTitle(month.year, month.monthNumber), Modifier.weight(1f), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                IconButton(onClick = { month = month.plus(1, DateTimeUnit.MONTH) }) { Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, t("journal.nextMonth")) }
            }
            Row(Modifier.fillMaxWidth()) {
                DayOfWeek.entries.forEach { Text(dayName(it, lang).take(if (lang.rtl) 3 else 2), Modifier.weight(1f), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center) }
            }
            monthGrid(month.year, month.monthNumber).chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth()) {
                    week.forEach { d ->
                        Box(Modifier.weight(1f).aspectRatio(1f).padding(2.dp), contentAlignment = Alignment.Center) {
                            if (d != null) {
                                val picked = d == day
                                Box(
                                    Modifier.fillMaxSize().clip(CircleShape).background(if (picked) MaterialTheme.colorScheme.primary else androidx.compose.ui.graphics.Color.Transparent).clickable { day = d },
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(n(d.dayOfMonth), style = MaterialTheme.typography.bodyMedium, color = if (picked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface, fontWeight = if (d == today) FontWeight.Bold else FontWeight.Normal)
                                    if (d in written) Box(Modifier.align(Alignment.BottomCenter).padding(bottom = 4.dp).size(5.dp).clip(CircleShape).background(if (picked) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary))
                                }
                            }
                        }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            val before = neighbourEntry(written, day, forward = false)
            val after = neighbourEntry(written, day, forward = true)
            TextButton(onClick = { before?.let { day = it; month = LocalDate(it.year, it.monthNumber, 1) } }, enabled = before != null) { Text(t("journal.prevEntry")) }
            Text(prettyDate(day) + " " + n(day.year), Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            TextButton(onClick = { after?.let { day = it; month = LocalDate(it.year, it.monthNumber, 1) } }, enabled = after != null) { Text(t("journal.nextEntry")) }
        }
        JournalPanel(state, day)
        if (dates.isEmpty()) EmptyHint(t("journal.empty"), t("journal.emptyHint"))
        else {
            Text(t("journal.lookBack"), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            dates.take(30).forEach { d ->
                val e = state.entry(d)
                Panel(onClick = { day = d; month = LocalDate(d.year, d.monthNumber, 1) }) {
                    Text(prettyDate(d) + " " + n(d.year), style = MaterialTheme.typography.titleSmall)
                    val text = e?.body.orEmpty()
                    if (text.isNotBlank()) Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}
