package dev.athar.core

import okio.FileSystem
import okio.Path

/**
 * Brings the notes and journal of another vault (Thabit, Dawem) into this one by COPYING them. The source is never
 * changed or deleted; every file is checked by size and SHA-256; a file already here is never overwritten (a different
 * file of the same name is kept beside it as `name (from Thabit).md`); running it twice copies nothing new.
 */
object VaultImport {
    /** What an import did. [ok] is false when something could not be verified; nothing is lost either way. */
    data class Report(val source: Path, val copied: Int, val kept: Int, val problems: List<String>) {
        val ok: Boolean get() = problems.isEmpty()
    }

    /** The folders in [candidates] that look like a vault with notes or a journal, other than [target] itself. */
    fun find(fs: FileSystem, candidates: List<Path>, target: Path): List<Path> =
        candidates.distinct().filter { it != target && (hasDir(fs, it / "notes") || hasDir(fs, it / "journal")) }

    private fun hasDir(fs: FileSystem, p: Path) = fs.metadataOrNull(p)?.isDirectory == true

    /** A label for clash names: the folder's own name, e.g. `Thabit`. */
    fun labelOf(source: Path): String = source.name.ifEmpty { "old vault" }

    fun import(fs: FileSystem, source: Path, target: Path): Report {
        val label = labelOf(source)
        var copied = 0; var kept = 0
        val problems = mutableListOf<String>()
        for (part in listOf("notes", "journal")) {
            if (!hasDir(fs, source / part)) continue
            val r = FolderMigration.copyAndVerify(fs, source / part, target / part, label)
            copied += r.copied; kept += r.kept; problems += r.problems
        }
        return Report(source, copied, kept, problems)
    }
}
