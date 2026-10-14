package dev.athar.core

/** What to show when Athar opens: the first-install tour, the one-time notes after an update, or nothing. */
enum class Welcome { Tour, WhatsNew, Nothing }

object Onboarding {
    /** `0.7.0` > `0.6.1`; missing or non-numeric parts count as 0. */
    fun compare(a: String, b: String): Int {
        val x = a.split('.').map { it.toIntOrNull() ?: 0 }
        val y = b.split('.').map { it.toIntOrNull() ?: 0 }
        for (i in 0 until maxOf(x.size, y.size)) {
            val d = x.getOrElse(i) { 0 }.compareTo(y.getOrElse(i) { 0 })
            if (d != 0) return d
        }
        return 0
    }

    /**
     * [lastSeen] is the version whose notes were closed last (empty when never), [tourDone] whether the tour was finished
     * or skipped, [hasData] whether the vault already holds anything (an update from a version that did not store these
     * two values), [current] the installed version.
     */
    fun decide(lastSeen: String, tourDone: Boolean, hasData: Boolean, current: String): Welcome = when {
        !tourDone && lastSeen.isEmpty() && !hasData -> Welcome.Tour
        lastSeen.isNotEmpty() && compare(current, lastSeen) <= 0 -> Welcome.Nothing // same version, or a downgrade
        else -> Welcome.WhatsNew
    }
}

/** Short notes per version (English and Arabic); shown once after an update. */
object ReleaseNotes {
    private val notes: Map<String, Map<String, List<String>>> = mapOf(
        "0.1.0" to mapOf(
            "en" to listOf(
                "Athar is here: quick capture, Markdown notes, a daily journal and (on the desktop) clipboard history, all in plain files.",
                "Bring your notes and journal from Thabit or Dawem: Settings, Import. Nothing is moved or changed in the old folder.",
                "English and Arabic, with right-to-left layout.",
            ),
            "ar" to listOf(
                "أثر هنا: التقاط سريع، ملاحظات بصيغة Markdown، يوميات، وسجل الحافظة على سطح المكتب، كلها في ملفات عادية.",
                "أحضر ملاحظاتك ويومياتك من ثابت أو داوم: الإعدادات ثم الاستيراد. لا يُنقل شيء ولا يتغير في المجلد القديم.",
                "العربية والإنجليزية مع اتجاه الكتابة من اليمين إلى اليسار.",
            ),
        ),
    )

    /** The notes for [version] in [lang], or for the newest older version that has some. */
    fun forVersion(version: String, lang: String = "en"): List<String> {
        val key = notes.keys.filter { Onboarding.compare(it, version) <= 0 }.maxWithOrNull { a, b -> Onboarding.compare(a, b) } ?: return emptyList()
        return notes.getValue(key)[lang] ?: notes.getValue(key).getValue("en")
    }
}
