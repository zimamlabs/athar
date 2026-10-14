package dev.athar.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp

/** A small Markdown reader for notes: headings, bullets, checklists, quotes, code, and bold, italic and `code` inside a line. */
@Composable
fun MarkdownText(source: String, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        var inCode = false
        source.lines().forEach { raw ->
            val line = raw.trimEnd()
            if (line.startsWith("```")) { inCode = !inCode; return@forEach }
            when {
                inCode -> Text(line, fontFamily = FontFamily.Monospace, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                line.isBlank() -> {}
                line.startsWith("### ") -> Text(inline(line.drop(4)), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                line.startsWith("## ") -> Text(inline(line.drop(3)), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                line.startsWith("# ") -> Text(inline(line.drop(2)), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                line.startsWith("- [ ] ") -> Row { Text("☐  ", color = MaterialTheme.colorScheme.primary); Text(inline(line.drop(6)), style = MaterialTheme.typography.bodyLarge) }
                line.startsWith("- [x] ") || line.startsWith("- [X] ") -> Row { Text("☑  ", color = MaterialTheme.colorScheme.primary); Text(inline(line.drop(6)), style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                line.startsWith("- ") || line.startsWith("* ") -> Row { Text("•  ", color = MaterialTheme.colorScheme.primary); Text(inline(line.drop(2)), style = MaterialTheme.typography.bodyLarge) }
                line.startsWith("> ") -> Text(inline(line.drop(2)), Modifier.padding(start = 12.dp), style = MaterialTheme.typography.bodyLarge, fontStyle = FontStyle.Italic, color = MaterialTheme.colorScheme.onSurfaceVariant)
                else -> Text(inline(line), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

/** **bold**, *italic* and `code` inside one line; anything unmatched is shown as typed. */
private fun inline(text: String): AnnotatedString = buildAnnotatedString {
    var i = 0
    while (i < text.length) {
        val bold = text.startsWith("**", i).takeIf { it }?.let { text.indexOf("**", i + 2).takeIf { e -> e > i + 2 } }
        val code = if (text[i] == '`') text.indexOf('`', i + 1).takeIf { it > i + 1 } else null
        val italic = if (text[i] == '*' && !text.startsWith("**", i)) text.indexOf('*', i + 1).takeIf { it > i + 1 } else null
        when {
            bold != null -> { withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(text.substring(i + 2, bold)) }; i = bold + 2 }
            code != null -> { withStyle(SpanStyle(fontFamily = FontFamily.Monospace)) { append(text.substring(i + 1, code)) }; i = code + 1 }
            italic != null -> { withStyle(SpanStyle(fontStyle = FontStyle.Italic)) { append(text.substring(i + 1, italic)) }; i = italic + 1 }
            else -> { append(text[i]); i++ }
        }
    }
}
