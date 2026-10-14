package dev.athar.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate

/** The two languages the app speaks. Arabic lays everything out right to left. */
enum class Lang(val code: String, val rtl: Boolean) { En("en", false), Ar("ar", true) }

/** The language code of the device (`en`, `ar`, ...). */
expect fun systemLanguageCode(): String

/** [setting] is "system", "en" or "ar"; "system" follows [system], and any language but Arabic falls back to English. */
fun langOf(setting: String, system: String = systemLanguageCode()): Lang = when (setting) {
    "en" -> Lang.En
    "ar" -> Lang.Ar
    else -> if (system.lowercase().startsWith("ar")) Lang.Ar else Lang.En
}

val LocalLang = compositionLocalOf { Lang.En }

/** "western" (0123) or "arabic" (٠١٢٣). */
val LocalDigits = compositionLocalOf { "western" }

fun localizeDigits(text: String, digits: String): String =
    if (digits != "arabic") text else buildString { text.forEach { append(if (it in '0'..'9') '٠' + (it - '0') else it) } }

/** Looks up [key] in [lang], falling back to English and then to the key itself; `{0}`, `{1}` are replaced by [args]. */
fun translate(lang: Lang, key: String, vararg args: Any): String {
    var s = (if (lang == Lang.Ar) StringsAr[key] else null) ?: StringsEn[key] ?: key
    args.forEachIndexed { i, a -> s = s.replace("{$i}", a.toString()) }
    return s
}

/** The text for [key] in the app's language, digits in the chosen style. */
@Composable
fun t(key: String, vararg args: Any): String = localizeDigits(translate(LocalLang.current, key, *args), LocalDigits.current)

/** A number in the chosen digit style. */
@Composable
fun n(value: Int): String = localizeDigits(value.toString(), LocalDigits.current)

private val monthsEn = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
private val monthsAr = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
private val daysEn = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
private val daysAr = listOf("الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد")

fun monthName(n: Int, lang: Lang): String = (if (lang == Lang.Ar) monthsAr else monthsEn)[(n - 1).coerceIn(0, 11)]

fun dayName(d: DayOfWeek, lang: Lang): String = (if (lang == Lang.Ar) daysAr else daysEn)[d.ordinal]

/** "Wed 14 Oct" or "الأربعاء ١٤ أكتوبر". */
@Composable
fun prettyDate(d: LocalDate): String {
    val lang = LocalLang.current
    return localizeDigits("${dayName(d.dayOfWeek, lang)} ${d.dayOfMonth} ${monthName(d.monthNumber, lang)}", LocalDigits.current)
}

/** "Oct 2026" or "أكتوبر ٢٠٢٦". */
@Composable
fun monthTitle(year: Int, month: Int): String = localizeDigits("${monthName(month, LocalLang.current)} $year", LocalDigits.current)
