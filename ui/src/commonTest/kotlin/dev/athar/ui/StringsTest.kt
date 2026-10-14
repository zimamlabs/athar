package dev.athar.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class StringsTest {
    private fun holes(s: String) = Regex("\\{\\d}").findAll(s).map { it.value }.sorted().toList()

    @Test fun everyEnglishKeyHasAnArabicOneAndTheOtherWayRound() {
        assertEquals(StringsEn.keys.sorted(), StringsAr.keys.sorted(), "keys only in one language: " + (StringsEn.keys.toSet() - StringsAr.keys.toSet() + (StringsAr.keys.toSet() - StringsEn.keys.toSet())))
    }

    @Test fun theSamePlaceholdersAppearInBothLanguages() {
        StringsEn.forEach { (k, v) -> assertEquals(holes(v), holes(StringsAr.getValue(k)), "placeholders of $k") }
    }

    @Test fun noTextIsEmptyAndArabicIsReallyArabic() {
        StringsEn.forEach { (k, v) -> assertTrue(v.isNotBlank(), k) }
        val letters = Regex("[؀-ۿ]")
        StringsAr.forEach { (k, v) -> if (k != "home.captureHint") assertTrue(letters.containsMatchIn(v), "$k has no Arabic: $v") }
    }

    @Test fun translateFillsPlaceholdersAndFallsBack() {
        assertEquals("Welcome to Athar", translate(Lang.En, "tour.welcome", "Athar"))
        assertEquals("مرحباً بك في Athar", translate(Lang.Ar, "tour.welcome", "Athar"))
        assertEquals("no.such.key", translate(Lang.Ar, "no.such.key"))
    }

    @Test fun digitsFollowTheSetting() {
        assertEquals("٠١٢٣٤٥٦٧٨٩", localizeDigits("0123456789", "arabic"))
        assertEquals("12 Oct", localizeDigits("12 Oct", "western"))
        assertEquals("١٢ Oct", localizeDigits("12 Oct", "arabic"))
    }

    @Test fun theLanguageSettingWinsThenTheSystem() {
        assertEquals(Lang.Ar, langOf("ar", "en")); assertEquals(Lang.En, langOf("en", "ar"))
        assertEquals(Lang.Ar, langOf("system", "ar")); assertEquals(Lang.En, langOf("system", "fr")); assertTrue(Lang.Ar.rtl && !Lang.En.rtl)
    }

    @Test fun monthAndDayNamesExistInBothLanguages() {
        (1..12).forEach { assertTrue(monthName(it, Lang.Ar).isNotBlank() && monthName(it, Lang.En).isNotBlank()) }
        assertEquals("الاثنين", dayName(kotlinx.datetime.DayOfWeek.MONDAY, Lang.Ar))
    }
}
