package dev.athar.core

import kotlin.random.Random

/** Short random ids that are safe in file names and easy to read in plain-text files. */
fun newId(random: Random = Random.Default): String {
    val chars = "abcdefghjkmnpqrstuvwxyz23456789"
    return buildString { repeat(8) { append(chars[random.nextInt(chars.length)]) } }
}

/** A file-name-safe version of a title; never empty. */
fun slugOf(title: String): String {
    val s = title.trim().lowercase().map { c -> if (c.isLetterOrDigit()) c else '-' }.joinToString("").replace(Regex("-+"), "-").trim('-')
    return s.take(60).ifEmpty { "untitled" }
}
