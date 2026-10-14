package dev.athar.clipboard

import dev.athar.core.VaultFiles
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ClipboardHistoryTest {
    private class FakeClipboard(var text: String? = null, override val supported: Boolean = true) : ClipboardSource {
        override fun read() = text
        override fun write(text: String) { this.text = text }
    }

    private val fs = FakeFileSystem()
    private val files = VaultFiles(fs, "/v".toPath(), Clock.System, TimeZone.UTC)
    private val history = ClipboardHistory(files, cap = 5)

    private fun monitor(cb: FakeClipboard, on: () -> Boolean = { true }, ex: () -> List<String> = { emptyList() }) = ClipboardMonitor(cb, history, on, ex)

    @Test fun aCopyIsRecordedAndTheSameTextAgainIsNot() {
        assertNotNull(history.record("hello")); assertNull(history.record("hello"))
        assertEquals(listOf("hello"), history.items().map { it.text })
    }

    @Test fun blankAndHugeTextsAreSkipped() {
        assertNull(history.record("   ")); assertNull(history.record("x".repeat(history.maxChars + 1)))
        assertEquals(emptyList(), history.items())
    }

    @Test fun anOlderEqualCopyMovesToTheTopInsteadOfRepeating() {
        history.record("a"); history.record("b"); history.record("a")
        assertEquals(listOf("a", "b"), history.items().map { it.text })
    }

    @Test fun theCapDropsTheOldestButNeverAPinnedItem() {
        history.record("keep")
        history.pin(history.items().single().id, true)
        (1..8).forEach { history.record("item $it") }
        val items = history.items()
        assertEquals(5, items.size)
        assertEquals("keep", items.first().text); assertTrue(items.first().pinned)
        assertEquals("item 8", items[1].text)
    }

    @Test fun clearKeepsPinnedAndDeleteRemovesOne() {
        history.record("a"); history.record("b"); history.record("c")
        history.pin(history.items().first { it.text == "a" }.id, true)
        history.delete(history.items().first { it.text == "b" }.id)
        assertEquals(listOf("a", "c"), history.items().map { it.text })
        history.clear()
        assertEquals(listOf("a"), history.items().map { it.text })
    }

    @Test fun exclusionPatternsStopTheCopy() {
        val cb = FakeClipboard("start")
        val m = monitor(cb, ex = { listOf("(?i)password", "^\\d{16}$") })
        assertNull(m.poll())            // first look only notes what is there
        cb.text = "my Password is x"; assertNull(m.poll())
        cb.text = "4111111111111111"; assertNull(m.poll())
        cb.text = "fine"; assertNotNull(m.poll())
        assertEquals(listOf("fine"), history.items().map { it.text })
    }

    @Test fun nothingIsReadOrRecordedWhilePausedAndTheBaselineIsNeverRecorded() {
        var on = false
        val cb = FakeClipboard("secret from before")
        val m = monitor(cb, on = { on })
        cb.text = "while paused"; assertNull(m.poll())
        on = true
        assertNull(m.poll())            // baseline: "while paused" is what was there when recording began
        cb.text = "after"; assertNotNull(m.poll())
        on = false; cb.text = "paused again"; assertNull(m.poll())
        on = true; assertNull(m.poll()) // a new baseline, "paused again" is not recorded
        assertEquals(listOf("after"), history.items().map { it.text })
    }

    @Test fun skipNextAndCopyBackAreNotRecorded() {
        val cb = FakeClipboard("x")
        val m = monitor(cb)
        m.poll()
        m.skipNext(); cb.text = "private"; assertNull(m.poll())
        cb.text = "real"; assertNotNull(m.poll())
        m.copyBack("real"); assertNull(m.poll())
        assertEquals(listOf("real"), history.items().map { it.text })
    }

    @Test fun anUnsupportedClipboardNeverRecords() {
        val m = monitor(FakeClipboard("a", supported = false))
        assertNull(m.poll()); assertEquals(emptyList(), history.items())
    }

    @Test fun aBrokenPatternIsIgnoredNotFatal() {
        val cb = FakeClipboard("a")
        val m = monitor(cb, ex = { listOf("([unclosed") })
        m.poll(); cb.text = "b"; assertNotNull(m.poll())
    }
}
