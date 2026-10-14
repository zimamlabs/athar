package dev.athar.notes

import kotlinx.serialization.Serializable

@Serializable
data class Note(
    val id: String,
    val title: String,
    val body: String,
    val created: String,
    val updated: String,
    val tags: List<String> = emptyList(),
    val pinned: Boolean = false,
    val favorite: Boolean = false,
    val archived: Boolean = false,
    /** The collection (a folder) the note is filed in; empty for none. */
    val collection: String = "",
)

/** Words in the note's text, for the writing stats. */
fun Note.wordCount(): Int = body.split(Regex("\\s+")).count { it.isNotEmpty() }
