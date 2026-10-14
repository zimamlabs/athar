package dev.athar.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.athar.core.AppSettings

/** The colours of one palette in light and dark. Values are 0xRRGGBB. */
class Palette(val id: String, val label: String, val swatch: Long, private val light: LongArray, private val dark: LongArray) {
    private fun c(a: LongArray, i: Int) = Color(0xFF000000 or a[i])

    fun scheme(isDark: Boolean): ColorScheme {
        val a = if (isDark) dark else light
        // order: primary, onPrimary, primaryContainer, onPrimaryContainer, secondary, tertiary,
        //        background, surfaceContainer, surfaceContainerHigh, onSurface, onSurfaceVariant, outlineVariant
        return run {
            if (isDark) darkColorScheme(
                primary = c(a, 0), onPrimary = c(a, 1), primaryContainer = c(a, 2), onPrimaryContainer = c(a, 3), secondary = c(a, 4), tertiary = c(a, 5),
                background = c(a, 6), surface = c(a, 6), surfaceContainer = c(a, 7), surfaceContainerHigh = c(a, 8), onSurface = c(a, 9), onSurfaceVariant = c(a, 10), outlineVariant = c(a, 11),
            ) else lightColorScheme(
                primary = c(a, 0), onPrimary = c(a, 1), primaryContainer = c(a, 2), onPrimaryContainer = c(a, 3), secondary = c(a, 4), tertiary = c(a, 5),
                background = c(a, 6), surface = c(a, 6), surfaceContainer = c(a, 7), surfaceContainerHigh = c(a, 8), onSurface = c(a, 9), onSurfaceVariant = c(a, 10), outlineVariant = c(a, 11),
            )
        }
    }
}

val Palettes = listOf(
    Palette(
        "zimam", "Zimam", 0xFF5A1F,
        longArrayOf(0xE5481A, 0xFFFFFF, 0xFFDDD0, 0x3A0E00, 0x6B5B4B, 0x4F5F7A, 0xFFF8F0, 0xFFFFFF, 0xF6ECDF, 0x1A1613, 0x5E5A52, 0xE2D6C4),
        longArrayOf(0xFF5A1F, 0x1A0A02, 0x4A1D0B, 0xFFDDD0, 0xD9CDB8, 0xA9B8D0, 0x0F0F0F, 0x1A1613, 0x241E19, 0xF5F1E8, 0xA9A59B, 0x3A332D),
    ),
    Palette(
        "amber", "Amber", 0xFFB84D,
        longArrayOf(0x9A5B00, 0xFFFFFF, 0xFFDDB0, 0x301A00, 0x4F6B5B, 0x3F5F8A, 0xFFF8F0, 0xFFFFFF, 0xF6ECDF, 0x211A12, 0x5E5547, 0xE2D6C4),
        longArrayOf(0xFFB84D, 0x4A2A00, 0x6B3F00, 0xFFDDB0, 0xA7C8B3, 0xA9C7F5, 0x141720, 0x1C202B, 0x262B38, 0xEDE6DA, 0xB7AE9F, 0x3A4152),
    ),
    Palette(
        "ocean", "Ocean", 0x6ACBEB,
        longArrayOf(0x0B6E8A, 0xFFFFFF, 0xC7ECF7, 0x00212B, 0x3F6B7A, 0x5B5FA6, 0xF3FAFC, 0xFFFFFF, 0xE1EFF3, 0x14262C, 0x4A6068, 0xCBDDE3),
        longArrayOf(0x6ACBEB, 0x003644, 0x0B4F63, 0xC7ECF7, 0x9CCBDA, 0xB7B9F5, 0x0F1A1F, 0x16252B, 0x1F323A, 0xE3EEF2, 0xA5BCC4, 0x2F4650),
    ),
    Palette(
        "forest", "Forest", 0x7ED6A0,
        longArrayOf(0x2E7D4F, 0xFFFFFF, 0xCDEBD8, 0x00210F, 0x5A7B64, 0x8A6A2F, 0xF4FAF5, 0xFFFFFF, 0xE3EFE6, 0x15251B, 0x4C6354, 0xCFE0D3),
        longArrayOf(0x7ED6A0, 0x00391C, 0x1C5A37, 0xCDEBD8, 0xA7CFB3, 0xE0C58A, 0x0F1A14, 0x16251C, 0x20342A, 0xE1EEE5, 0xA6BFAF, 0x2E4637),
    ),
    Palette(
        "rose", "Rose", 0xFF8FAA,
        longArrayOf(0xB03A5B, 0xFFFFFF, 0xFFD9E1, 0x3F0017, 0x8B5A66, 0x7A5AA6, 0xFFF7F8, 0xFFFFFF, 0xF8E7EA, 0x2A1519, 0x6B4D54, 0xEBD3D8),
        longArrayOf(0xFF8FAA, 0x5E0027, 0x862041, 0xFFD9E1, 0xE7B4C0, 0xD2B8F5, 0x1C1215, 0x281A1E, 0x342329, 0xF3E3E7, 0xC4A8B0, 0x4A353B),
    ),
    Palette(
        "slate", "Slate", 0xB8C4D6,
        longArrayOf(0x3F4A5A, 0xFFFFFF, 0xDCE2EC, 0x111A27, 0x5F6B7D, 0x6B5F7D, 0xF5F6F8, 0xFFFFFF, 0xE7E9EE, 0x1A1D23, 0x5A606B, 0xD4D7DE),
        longArrayOf(0xB8C4D6, 0x1D2735, 0x36445A, 0xDCE2EC, 0xC0C9D8, 0xD6C9E6, 0x12141A, 0x1A1D25, 0x252932, 0xE6E8EE, 0xA9AFBB, 0x383D49),
    ),
)

fun paletteOf(id: String): Palette = Palettes.firstOrNull { it.id == id } ?: Palettes.first()

@Composable
fun AtharTheme(settings: AppSettings = AppSettings(), content: @Composable () -> Unit) {
    val dark = when (settings.mode) { "dark" -> true; "light" -> false; else -> isSystemInDarkTheme() }
    val lang = langOf(settings.language)
    androidx.compose.runtime.CompositionLocalProvider(
        LocalLang provides lang,
        LocalDigits provides settings.digits,
        androidx.compose.ui.platform.LocalLayoutDirection provides if (lang.rtl) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr,
    ) {
        MaterialTheme(
            colorScheme = paletteOf(settings.palette).scheme(dark),
            shapes = Shapes(small = RoundedCornerShape(10.dp), medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(28.dp)),
            content = content,
        )
    }
}
