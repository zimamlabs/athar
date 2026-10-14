package dev.athar.app

import dev.athar.ui.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.unit.Density
import dev.athar.core.Vault
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.todayIn
import okio.Path.Companion.toPath
import okio.fakefilesystem.FakeFileSystem
import org.jetbrains.skia.EncodedImageFormat
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import dev.athar.journal.JournalEntry
import dev.athar.notes.Note

/**
 * Draws each screen offscreen into build/screenshots so the layout can be looked at without a device.
 * It also proves every screen composes with realistic data without throwing.
 */
@OptIn(ExperimentalComposeUiApi::class)
class ScreenshotTest {
    private object Fake : dev.athar.clipboard.ClipboardSource {
        override val supported = true
        override fun read(): String? = null
        override fun write(text: String) {}
    }

    private fun sampleStore(): AtharStore {
        val vault = Vault(FakeFileSystem(), "/v".toPath())
        val today = Clock.System.todayIn(TimeZone.currentSystemDefault())
        vault.saveJournal(JournalEntry(today, "Slow morning, long walk, finished the chapter I was stuck on.", 4))
        vault.saveJournal(JournalEntry(today.minus(1, DateTimeUnit.DAY), "Busy day at work.", 3))
        vault.saveJournal(JournalEntry(today.minus(1, DateTimeUnit.YEAR), "A year ago: first draft of the plan.", 4))
        vault.saveNote(Note("", "Trip to the mountains", "# Trip\n\nPack warm clothes, check the bus times.", "", "", listOf("travel"), pinned = true, collection = "Plans"))
        vault.saveNote(Note("", "Reading list", "Books to read this winter.", "", "", listOf("books"), favorite = true))
        vault.saveNote(Note("", "ملاحظة بالعربية", "هذه ملاحظة تجريبية لاختبار الاتجاه من اليمين إلى اليسار.", "", "", listOf("عربي")))
        vault.clipboardHistory.record("https://example.com/article")
        vault.clipboardHistory.record("val x = listOf(1, 2, 3)\nprintln(x)")
        return AtharStore(vault, "/v", Fake)
    }

    private fun shot(name: String, width: Int, height: Int, settings: dev.athar.core.AppSettings? = null, nav: (Nav) -> Unit) {
        val store = sampleStore()
        // The tour opens on a fresh install; the screenshots want the app itself.
        store.saveSettings((settings ?: dev.athar.core.AppSettings()).copy(tourDone = true, lastSeenVersion = Brand.VERSION))
        val scene = ImageComposeScene(width * 2, height * 2, Density(2f)) { AtharApp(store, Nav().also(nav), showSplash = false) }
        try {
            scene.render(0)
            val data = scene.render(100_000_000).encodeToData(EncodedImageFormat.PNG)!!
            val out = File("build/screenshots").apply { mkdirs() }
            File(out, "$name.png").writeBytes(data.bytes)
            assertTrue(data.bytes.size > 1_000)
        } finally {
            scene.close()
        }
    }

    @Test fun phone() {
        for (s in Section.entries) shot("phone-${s.name.lowercase()}", 412, 860) { it.go(s) }
        shot("phone-insights", 412, 860) { it.screen = Screen.Insights }
        shot("phone-settings", 412, 860) { it.screen = Screen.Settings }
        shot("phone-search", 412, 860) { it.screen = Screen.Search }
        shot("phone-about", 412, 860) { it.screen = Screen.About }
    }

    @Test fun arabicRightToLeft() {
        val ar = dev.athar.core.AppSettings(language = "ar", digits = "arabic")
        for (s in Section.entries) shot("ar-${s.name.lowercase()}", 412, 860, ar) { it.go(s) }
        shot("ar-settings", 412, 860, ar) { it.screen = Screen.Settings }
        shot("ar-about", 412, 860, ar) { it.screen = Screen.About }
    }

    @Test fun themes() {
        val s = { palette: String, mode: String -> dev.athar.core.AppSettings(palette, mode) }
        shot("theme-zimam-dark-home", 412, 860, s("zimam", "dark")) { it.go(Section.Home) }
        shot("theme-zimam-light-home", 412, 860, s("zimam", "light")) { it.go(Section.Home) }
        shot("theme-forest-light-notes", 412, 860, s("forest", "light")) { it.go(Section.Notes) }
    }

    @Test fun desktop() {
        shot("desktop-home", 1000, 760) { it.go(Section.Home) }
        shot("desktop-clipboard", 1000, 760) { it.go(Section.Clipboard) }
        shot("desktop-notes-trash", 1000, 760) { it.go(Section.Notes, dev.athar.notes.NoteView.Trash) }
    }
}
