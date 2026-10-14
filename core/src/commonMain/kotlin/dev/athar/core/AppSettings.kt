package dev.athar.core

import kotlinx.serialization.Serializable

/** How the app looks and behaves; stored in the vault's `.athar/settings.json` so it travels with the data. */
@Serializable
data class AppSettings(
    /** One of the palette ids the app knows; an unknown id falls back to the default. */
    val palette: String = "zimam",
    /** "system", "light" or "dark". */
    val mode: String = "system",
    /** "system", "en" or "ar". */
    val language: String = "system",
    /** "western" (0123) or "arabic" (٠١٢٣). */
    val digits: String = "western",
    /** The version whose "what's new" notes were closed last; empty when never. */
    val lastSeenVersion: String = "",
    /** True once the first-install tour was finished or skipped. */
    val tourDone: Boolean = false,
    /** Desktop only: whether the clipboard history is recording. Off until the user turns it on. */
    val clipboardOn: Boolean = false,
    /** Regular expressions; a copied text that matches one is never recorded. */
    val clipboardExclusions: List<String> = emptyList(),
)
