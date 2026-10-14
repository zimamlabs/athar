package dev.athar.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/** The card every screen uses: warm surface, big corners, a hairline border and the small orange diamond in the corner. */
@Composable
fun Panel(modifier: Modifier = Modifier, onClick: (() -> Unit)? = null, diamond: Boolean = true, content: @Composable () -> Unit) {
    val border = if (MaterialTheme.colorScheme.background.luminance() < 0.5f) Color.White.copy(alpha = 0.08f) else Color.Black.copy(alpha = 0.08f)
    Card(
        modifier = modifier.fillMaxWidth().let { if (onClick != null) it.clip(RoundedCornerShape(24.dp)).clickable(onClick = onClick) else it },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        border = BorderStroke(1.dp, border),
        elevation = CardDefaults.cardElevation(0.dp),
    ) {
        Box {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) { content() }
            if (diamond) Diamond(Modifier.align(Alignment.TopEnd).padding(12.dp).size(8.dp))
        }
    }
}

/** The signature accent: a small rounded square turned on its corner, in the theme's accent. */
@Composable
fun Diamond(modifier: Modifier = Modifier) {
    Box(modifier.rotate(45f).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.primary))
}

/** A soft accent glow behind [content], for hero areas. */
@Composable
fun Glow(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val glow = MaterialTheme.colorScheme.primary
    Box(modifier.drawBehind {
        drawRect(Brush.radialGradient(listOf(glow.copy(alpha = 0.26f), Color.Transparent), center = Offset(size.width * 0.5f, size.height * 0.35f), radius = size.maxDimension * 0.7f))
    }) { content() }
}

@Composable
fun SectionTitle(text: String, action: (@Composable () -> Unit)? = null) {
    Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(text, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f))
        action?.invoke()
    }
}

@Composable
fun EmptyHint(title: String, hint: String) {
    Column(Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** A one-field dialog: a title, a text box and a confirm button. */
@Composable
fun NameDialog(title: String, initial: String, confirm: String, onDismiss: () -> Unit, onDone: (String) -> Unit) {
    var name by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(initial) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { androidx.compose.material3.OutlinedTextField(name, { name = it }, singleLine = true, placeholder = { Text(t("name")) }) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { onDone(name) }, enabled = name.isNotBlank()) { Text(confirm) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(t("cancel")) } },
    )
}

/** Asks before anything is cleared, deleted or removed. */
@Composable
fun ConfirmDialog(title: String, text: String, confirm: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { androidx.compose.material3.TextButton(onClick = { onConfirm(); onDismiss() }) { Text(confirm, color = MaterialTheme.colorScheme.error) } },
        dismissButton = { androidx.compose.material3.TextButton(onClick = onDismiss) { Text(t("cancel")) } },
    )
}
