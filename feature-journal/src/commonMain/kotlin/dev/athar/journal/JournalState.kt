package dev.athar.journal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import kotlinx.datetime.LocalDate

/** The journal for the screens: reading an entry inside a composable re-reads it after any save. */
class JournalState(private val repo: JournalRepository) {
    private var revision by mutableIntStateOf(0)

    fun reload() { revision++ }

    fun entry(date: LocalDate): JournalEntry? { revision; return repo.entry(date) }

    fun dates(): List<LocalDate> { revision; return repo.dates() }

    fun save(entry: JournalEntry) { repo.save(entry); revision++ }
}
