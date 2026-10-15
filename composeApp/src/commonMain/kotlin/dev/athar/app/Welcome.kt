package dev.athar.app

import dev.athar.ui.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.athar.core.ReleaseNotes
import dev.athar.ui.LocalLang
import kotlinx.coroutines.launch

/** The first-install tour: what Athar is, where the data lives, bringing notes from Thabit, a look to pick, and a start. Skip or Done both end it. */
@Composable
fun TourScreen(store: AtharStore, onDone: () -> Unit) {
    val platform = LocalPlatform.current
    val pages = 5
    val pager = rememberPagerState { pages }
    val scope = rememberCoroutineScope()
    Column(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background).padding(24.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) { TextButton(onClick = onDone) { Text(t("tour.skip")) } }
        HorizontalPager(pager, Modifier.weight(1f).fillMaxWidth()) { page ->
            Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                when (page) {
                    0 -> TourPage(t("tour.welcome", Brand.NAME), t("tour.welcomeText"))
                    1 -> {
                        TourPage(t("tour.dataTitle"), t("tour.dataText", store.location))
                        if (platform.allowVisibleFolder != null && !platform.visibleFolderOn) {
                            Spacer(Modifier.height(16.dp))
                            OutlinedButton(onClick = { platform.allowVisibleFolder.invoke() }) { Text(t("tour.visible")) }
                            Text(t("tour.visibleHint"), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))
                        }
                    }
                    2 -> {
                        TourPage(t("tour.importTitle"), t("tour.importText"))
                        Spacer(Modifier.height(12.dp))
                        ImportSection(store, platform)
                    }
                    3 -> {
                        TourPage(t("tour.lookTitle"), t("tour.lookText"))
                        Spacer(Modifier.height(16.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("system" to t("settings.system"), "light" to t("settings.light"), "dark" to t("settings.dark")).forEach { (id, label) -> FilterChip(store.settings.mode == id, { store.saveSettings(store.settings.copy(mode = id)) }, { Text(label) }) }
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf("system" to t("settings.system"), "en" to "English", "ar" to "العربية").forEach { (id, label) -> FilterChip(store.settings.language == id, { store.saveSettings(store.settings.copy(language = id)) }, { Text(label) }) }
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Palettes.forEach { p ->
                                val on = store.settings.palette == p.id
                                Box(
                                    Modifier.size(40.dp).clip(CircleShape).background(Color(0xFF000000 or p.swatch)).clickable { store.saveSettings(store.settings.copy(palette = p.id)) }.then(if (on) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier),
                                    contentAlignment = Alignment.Center,
                                ) { if (on) Icon(Icons.Rounded.Check, null, tint = Color.Black.copy(alpha = 0.7f)) }
                            }
                        }
                    }
                    else -> TourPage(t("tour.readyTitle"), t("tour.readyText"))
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(pages) { i -> Box(Modifier.size(8.dp).clip(CircleShape).background(if (i == pager.currentPage) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)) }
            }
            if (pager.currentPage < pages - 1) Button(onClick = { scope.launch { pager.animateScrollToPage(pager.currentPage + 1) } }) { Text(t("tour.next")) }
            else Button(onClick = onDone) { Text(t("tour.done")) }
        }
    }
}

@Composable
private fun TourPage(title: String, text: String) {
    Text(title, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
    Spacer(Modifier.height(12.dp))
    Text(text, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
}

/** The notes for this version, shown once after an update. */
@Composable
fun WhatsNewDialog(version: String, onClose: () -> Unit) {
    val notes = ReleaseNotes.forVersion(version, LocalLang.current.code)
    AlertDialog(
        onDismissRequest = onClose,
        title = { Text(t("whatsnew.title", version)) },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) { notes.forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium) } } },
        confirmButton = { TextButton(onClick = onClose) { Text(t("close")) } },
    )
}
