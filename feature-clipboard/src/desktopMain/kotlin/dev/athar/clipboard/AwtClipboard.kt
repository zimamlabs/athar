package dev.athar.clipboard

import java.awt.GraphicsEnvironment
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection

/** The desktop's system clipboard through AWT; unsupported when there is no display (a headless test run). */
object AwtClipboard : ClipboardSource {
    override val supported: Boolean get() = !GraphicsEnvironment.isHeadless()

    override fun read(): String? = runCatching {
        val cb = Toolkit.getDefaultToolkit().systemClipboard
        if (cb.isDataFlavorAvailable(DataFlavor.stringFlavor)) cb.getData(DataFlavor.stringFlavor) as? String else null
    }.getOrNull()

    override fun write(text: String) { runCatching { Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null) } }
}
