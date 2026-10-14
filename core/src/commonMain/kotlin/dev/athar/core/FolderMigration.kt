package dev.athar.core

import okio.FileSystem
import okio.Path
import okio.buffer

/**
 * Copies an old vault folder into the new one without ever losing anything: every file goes through a `.part` file and
 * a rename, a file already in the destination is never overwritten (when it differs, the old copy is kept beside it as
 * `name (from Old).ext`), and the result is checked by file count, size and SHA-256. The old folder is never touched
 * here; it stays as a backup until the user asks to remove it, and removal checks again first.
 */
object FolderMigration {
    data class Result(val ok: Boolean, val copied: Int, val kept: Int, val problems: List<String>)

    /** One marker per old folder, written only after its copy verified. */
    fun marker(target: Path, label: String): Path = target / ".migrated-$label"

    private fun files(fs: FileSystem, root: Path): List<Path> =
        fs.listRecursively(root).filter { fs.metadata(it).isRegularFile && !it.name.endsWith(".part") }.toList()

    // shortcut: each file is read whole to hash it, fine for notes, journal and settings, not for huge attachments.
    fun sha256(fs: FileSystem, file: Path): String = fs.read(file) { readByteString() }.sha256().hex()

    private fun sameContent(fs: FileSystem, a: Path, b: Path): Boolean =
        fs.exists(b) && fs.metadata(a).size == fs.metadata(b).size && sha256(fs, a) == sha256(fs, b)

    private fun rel(root: Path, file: Path): String = file.relativeTo(root).toString()

    private fun stemAndExt(p: Path): Pair<String, String> {
        val n = p.name; val dot = n.lastIndexOf('.')
        return if (dot > 0) n.substring(0, dot) to n.substring(dot) else n to ""
    }

    /** Where a clashing old file is kept: `settings.json` becomes `settings (from Athar).json`, then `(from Athar 2)` if taken. */
    internal fun clashName(fs: FileSystem, dest: Path, label: String): Path {
        val (stem, ext) = stemAndExt(dest)
        var n = 1
        while (true) {
            val c = dest.parent!! / "$stem (from $label${if (n > 1) " $n" else ""})$ext"
            if (!fs.exists(c)) return c
            n++
        }
    }

    /** Where [file] of [source] ended up under [target]: the same path, or a kept clash copy next to it. */
    private fun landing(fs: FileSystem, source: Path, file: Path, target: Path, label: String): Path? {
        val dest = target / rel(source, file)
        if (sameContent(fs, file, dest)) return dest
        val (stem, ext) = stemAndExt(dest)
        val dir = dest.parent ?: return null
        if (!fs.exists(dir)) return null
        return fs.list(dir).firstOrNull { it.name.startsWith("$stem (from $label") && it.name.endsWith(ext) && sameContent(fs, file, it) }
    }

    /** What is wrong, if anything: a file with no verified counterpart in [target]. Empty means the copy is complete. */
    fun verify(fs: FileSystem, source: Path, target: Path, label: String = source.name): List<String> =
        files(fs, source).mapNotNull { f -> if (landing(fs, source, f, target, label) == null) "missing or different: ${rel(source, f)}" else null }

    fun copyAndVerify(fs: FileSystem, source: Path, target: Path, label: String = source.name): Result {
        if (fs.metadataOrNull(source)?.isDirectory != true || source == target) return Result(true, 0, 0, emptyList())
        var copied = 0; var kept = 0
        val problems = mutableListOf<String>()
        try {
            fs.createDirectories(target)
            fs.listRecursively(source).filter { fs.metadata(it).isDirectory }.forEach { fs.createDirectories(target / rel(source, it)) }
            for (f in files(fs, source)) {
                if (landing(fs, source, f, target, label) != null) continue
                var dest = target / rel(source, f)
                if (fs.exists(dest)) { dest = clashName(fs, dest, label); kept++ }
                dest.parent?.let { fs.createDirectories(it) }
                val part = dest.parent!! / (dest.name + ".part")
                fs.copy(f, part)
                fs.atomicMove(part, dest)
                copied++
            }
        } catch (e: Exception) {
            problems += "copy stopped: ${e.message ?: e::class.simpleName}"
        }
        problems += verify(fs, source, target, label)
        if (problems.isEmpty()) runCatching { fs.appendingSink(marker(target, label)).buffer().use { it.writeUtf8("copied from $label: $copied files, $kept kept beside existing ones\n") } }
        return Result(problems.isEmpty(), copied, kept, problems)
    }

    /** True when [old] was verified before and nothing in it has changed since, so the copy can be skipped. */
    fun upToDate(fs: FileSystem, old: Path, target: Path, label: String = old.name): Boolean {
        val m = fs.metadataOrNull(marker(target, label))?.lastModifiedAtMillis ?: return false
        return files(fs, old).none { (fs.metadata(it).lastModifiedAtMillis ?: Long.MAX_VALUE) > m }
    }

    /** True only when every file of [old] is verified in [target] right now. */
    fun safeToRemove(fs: FileSystem, old: Path, target: Path, label: String = old.name): Boolean =
        fs.metadataOrNull(old)?.isDirectory == true && fs.metadataOrNull(target)?.isDirectory == true && old != target && verify(fs, old, target, label).isEmpty()
}

/** Chooses the folder the vault lives in, copying older folders into it first. Never deletes anything. */
object VaultLocation {
    /** [dir] is where to open the vault now; [message] says why the old folder is still in use, when it is. */
    data class Choice(val dir: Path, val message: String?)

    fun choose(fs: FileSystem, canUseShared: Boolean, shared: Path, olderShared: List<Path>, privateDir: Path): Choice {
        if (!canUseShared) return Choice(privateDir, null)
        runCatching { fs.createDirectories(shared) }
        if (fs.metadataOrNull(shared)?.isDirectory != true) return Choice(privateDir, null)
        // The private vault is only the source when no older visible folder exists (as before: it came across once).
        val olds = olderShared.filter { it != shared && fs.metadataOrNull(it)?.isDirectory == true }.ifEmpty { listOf(privateDir).filter { fs.metadataOrNull(it)?.isDirectory == true } }
        for (old in olds) {
            if (FolderMigration.upToDate(fs, old, shared, labelOf(old))) continue
            val r = FolderMigration.copyAndVerify(fs, old, shared, labelOf(old))
            if (!r.ok) return Choice(old, "Athar could not verify the copy of $old into $shared, so it keeps using the old folder and will try again later. Nothing was deleted.")
        }
        return Choice(shared, null)
    }

    private fun labelOf(old: Path) = old.name

    /** Old folders whose every file is verified in [shared] right now, so removing them loses nothing. */
    fun removable(fs: FileSystem, current: Path, shared: Path, olds: List<Path>): List<Path> =
        if (current != shared) emptyList() else olds.filter { fs.metadataOrNull(it)?.isDirectory == true && FolderMigration.safeToRemove(fs, it, shared, labelOf(it)) }

    /** Removes [old] only after checking again that all of it is in [shared]. */
    fun remove(fs: FileSystem, old: Path, shared: Path): Boolean {
        if (!FolderMigration.safeToRemove(fs, old, shared, labelOf(old))) return false
        fs.deleteRecursively(old)
        return true
    }
}
