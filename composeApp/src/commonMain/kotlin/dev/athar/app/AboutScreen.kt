package dev.athar.app

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.athar.core.ReleaseNotes
import dev.athar.ui.Glow
import dev.athar.ui.LocalLang
import dev.athar.ui.Panel
import dev.athar.ui.t

/** The mark on a glow, what the app is, the tour and the notes, a privacy card and the links. */
@Composable
fun AboutScreen(store: AtharStore) {
    val uri = LocalUriHandler.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Glow(Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp))) {
            Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                BrandMark(Modifier.size(112.dp))
                Text(Brand.NAME, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold)
                Text(Brand.ARABIC, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary)
                Text(t("about.tagline"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Text(t("about.version", Brand.VERSION), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text(t("about.what"), style = MaterialTheme.typography.bodyLarge)
        Panel {
            Text(t("about.tourTitle"), style = MaterialTheme.typography.titleSmall)
            Text(t("about.tourHint"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = { store.tourOpen = true }) { Text(t("about.tourStart")) }
        }
        Panel {
            Text(t("about.new", Brand.VERSION), style = MaterialTheme.typography.titleSmall)
            ReleaseNotes.forVersion(Brand.VERSION, LocalLang.current.code).forEach { Text("•  $it", style = MaterialTheme.typography.bodyMedium) }
        }
        Panel {
            Text(t("about.privacy"), style = MaterialTheme.typography.titleSmall)
            Text(t("about.privacyText", store.location), style = MaterialTheme.typography.bodyMedium)
            Text(t("about.clipPrivacy"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Panel {
            Text(t("about.links"), style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(onClick = { uri.openUri(Brand.REPO) }) { Text(t("about.repo")) }
                TextButton(onClick = { uri.openUri("${Brand.REPO}/releases") }) { Text(t("about.releases")) }
                TextButton(onClick = { uri.openUri("${Brand.REPO}/issues") }) { Text(t("about.issues")) }
                TextButton(onClick = { uri.openUri("${Brand.REPO}/blob/main/LICENSE") }) { Text(t("about.licence")) }
            }
        }
        Text("by Zimam Labs  ·  MIT", Modifier.fillMaxWidth().padding(vertical = 8.dp), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
