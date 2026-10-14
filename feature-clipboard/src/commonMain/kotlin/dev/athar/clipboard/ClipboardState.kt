package dev.athar.clipboard

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** The clipboard history as Compose state, plus the monitor the desktop window drives with a timer. */
class ClipboardState(private val history: ClipboardHistory, val monitor: ClipboardMonitor, val source: ClipboardSource) {
    var items by mutableStateOf(history.items())
        private set

    val supported: Boolean get() = source.supported

    fun reload() { items = history.items() }

    /** One look at the clipboard; refreshes the list when something was recorded. */
    fun poll() { if (monitor.poll() != null) reload() }

    fun pin(id: String, pinned: Boolean) { history.pin(id, pinned); reload() }
    fun delete(id: String) { history.delete(id); reload() }
    fun clear() { history.clear(); reload() }
    fun copyBack(text: String) = monitor.copyBack(text)
    fun skipNext() = monitor.skipNext()
}
