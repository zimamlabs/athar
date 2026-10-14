package dev.athar.notes

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** How the notes list is filtered: the side menu picks one. */
sealed interface NoteView {
    data object All : NoteView
    data object Pinned : NoteView
    data object Favorites : NoteView
    data object Recent : NoteView
    data object Archived : NoteView
    data object Trash : NoteView
    data class Collection(val name: String) : NoteView
    data class Tag(val name: String) : NoteView
}

/** The notes of a [view] out of [all]: archived ones only show in Archived (and by search), recent is the last 20 edits. */
fun notesIn(view: NoteView, all: List<Note>): List<Note> {
    val live = all.filter { !it.archived }
    return when (view) {
        NoteView.All -> live
        NoteView.Pinned -> live.filter { it.pinned }
        NoteView.Favorites -> live.filter { it.favorite }
        NoteView.Recent -> live.sortedByDescending { it.updated }.take(20)
        NoteView.Archived -> all.filter { it.archived }
        NoteView.Trash -> emptyList()
        is NoteView.Collection -> live.filter { it.collection == view.name }
        is NoteView.Tag -> live.filter { view.name in it.tags }
    }
}

/** The notes as Compose state, read again after each change. */
class NotesState(private val repo: NotesRepository) {
    var notes by mutableStateOf(repo.notes())
        private set
    var trashed by mutableStateOf(repo.trashed())
        private set

    fun reload() { notes = repo.notes(); trashed = repo.trashed() }

    /** Every note tag with how many notes carry it, most used first. */
    fun tags(): List<Pair<String, Int>> = notes.flatMap { it.tags }.groupingBy { it }.eachCount().toList().sortedByDescending { it.second }

    fun collections(): List<Pair<String, Int>> = notes.filter { it.collection.isNotEmpty() }.groupingBy { it.collection }.eachCount().toList().sortedBy { it.first }

    fun save(note: Note): Note = repo.save(note).also { reload() }

    fun trash(id: String) { repo.trash(id); reload() }
    fun restore(id: String) { repo.restore(id); reload() }
    fun deleteForever(id: String) { repo.deleteForever(id); reload() }
    fun emptyTrash() { repo.emptyTrash(); reload() }
}
