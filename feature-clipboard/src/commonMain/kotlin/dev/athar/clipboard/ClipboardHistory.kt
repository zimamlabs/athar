package dev.athar.clipboard

import dev.athar.core.VaultFiles
import dev.athar.core.newId
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer

@Serializable
data class ClipboardItem(val id: String, val text: String, val at: String, val pinned: Boolean = false)

/** What the system clipboard offers: reading and writing text. The desktop has one; a phone's clipboard cannot be watched in the background. */
interface ClipboardSource {
    val supported: Boolean
    fun read(): String?
    fun write(text: String)
}

/** A source that does nothing, for platforms without a watchable clipboard. */
object NoClipboard : ClipboardSource {
    override val supported = false
    override fun read(): String? = null
    override fun write(text: String) {}
}

/**
 * The saved copies, in `clipboard/history.json`: newest first, pinned ones kept at the top. Nothing is recorded for an
 * empty or very long text, for the same text as the latest copy, or for text that matches an exclusion pattern.
 */
class ClipboardHistory(private val files: VaultFiles, val cap: Int = 500, val maxChars: Int = 20_000) {
    private val path get() = files.dir("clipboard") / "history.json"

    private fun load(): List<ClipboardItem> =
        files.read(path)?.let { runCatching { files.json.decodeFromString(ListSerializer(ClipboardItem.serializer()), it) }.getOrNull() }.orEmpty()

    private fun store(items: List<ClipboardItem>) = files.write(path, files.json.encodeToString(ListSerializer(ClipboardItem.serializer()), items))

    /** Pinned items first, then the rest newest first. */
    fun items(): List<ClipboardItem> = load().let { all -> all.filter { it.pinned } + all.filter { !it.pinned } }

    /** Adds [text] when the rules allow it and returns the new item; null when it was skipped. */
    fun record(text: String, exclusions: List<Regex> = emptyList()): ClipboardItem? {
        if (text.isBlank() || text.length > maxChars) return null
        if (exclusions.any { runCatching { it.containsMatchIn(text) }.getOrDefault(false) }) return null
        val all = load()
        if (all.firstOrNull { !it.pinned }?.text == text) return null
        val item = ClipboardItem(newId(), text, files.stamp())
        val kept = (listOf(item) + all.filter { it.text != text || it.pinned }).let { l ->
            // over the cap: drop the oldest unpinned items
            val extra = l.size - cap
            if (extra <= 0) l else { var drop = extra; l.asReversed().filter { if (drop > 0 && !it.pinned) { drop--; false } else true }.asReversed() }
        }
        store(kept)
        return item
    }

    fun pin(id: String, pinned: Boolean) = store(load().map { if (it.id == id) it.copy(pinned = pinned) else it })

    fun delete(id: String) = store(load().filter { it.id != id })

    /** Removes every copy that is not pinned. */
    fun clear() = store(load().filter { it.pinned })
}

/**
 * Watches the clipboard while recording is on. The first look after switching on only notes what is there (it is never
 * recorded); after that a changed text is passed to the history. Nothing is read at all while recording is off.
 */
class ClipboardMonitor(
    private val source: ClipboardSource,
    private val history: ClipboardHistory,
    private val on: () -> Boolean,
    private val exclusions: () -> List<String>,
) {
    private var started = false
    private var last: String? = null
    private var skip = false

    /** The next copy is not recorded (the "skip this copy" action, and copying an item back). */
    fun skipNext() { skip = true }

    /** Looks at the clipboard once; returns the item that was recorded, if any. */
    fun poll(): ClipboardItem? {
        if (!source.supported || !on()) { started = false; skip = false; return null }
        val now = source.read()?.takeIf { it.isNotEmpty() }
        if (!started) { started = true; last = now; return null }
        if (now == null || now == last) return null
        last = now
        if (skip) { skip = false; return null }
        return history.record(now, exclusions().mapNotNull { runCatching { Regex(it) }.getOrNull() })
    }

    /** Puts an item back on the clipboard without recording it again. */
    fun copyBack(text: String) { last = text; source.write(text) }
}
