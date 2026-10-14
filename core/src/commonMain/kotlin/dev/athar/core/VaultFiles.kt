package dev.athar.core

import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.json.Json
import okio.FileSystem
import okio.Path

/** The vault folder and the plain file handling every feature shares: reading, atomic writing, the clock and the JSON settings. */
class VaultFiles(
    val fs: FileSystem,
    val root: Path,
    private val clock: Clock = Clock.System,
    private val zone: TimeZone = TimeZone.currentSystemDefault(),
) {
    val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    fun today(): LocalDate = clock.now().toLocalDateTime(zone).date

    fun stamp(): String = clock.now().toLocalDateTime(zone).toString().take(19)

    fun read(path: Path): String? = if (fs.exists(path)) fs.read(path) { readUtf8() } else null

    /** Writes through a temporary file and renames it, so a crash never leaves half a file. */
    fun write(path: Path, text: String) {
        path.parent?.let { fs.createDirectories(it) }
        val tmp = path.parent!! / (path.name + ".tmp")
        fs.write(tmp) { writeUtf8(text) }
        fs.atomicMove(tmp, path)
    }

    fun dir(name: String): Path = root / name

    /** The vault's own folder for settings and the trash. */
    val trash: Path get() = root / ".athar" / "trash"
}
