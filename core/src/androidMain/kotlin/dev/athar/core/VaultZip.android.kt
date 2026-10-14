package dev.athar.core

import okio.BufferedSink
import okio.FileSystem
import okio.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

actual fun zipVault(fs: FileSystem, root: Path, out: BufferedSink) {
    ZipOutputStream(out.outputStream()).use { zip ->
        for (file in exportableFiles(fs, root)) {
            zip.putNextEntry(ZipEntry("Athar/" + file.relativeTo(root).toString()))
            fs.read(file) { zip.write(readByteArray()) }
            zip.closeEntry()
        }
    }
}
