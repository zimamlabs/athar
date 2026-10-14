package dev.athar.core

import okio.BufferedSink
import okio.FileSystem
import okio.Path

/** Writes every file under [root] (not the trash) into a zip on [out], with paths relative to the vault. */
expect fun zipVault(fs: FileSystem, root: Path, out: BufferedSink)

/** The files that go into an export: everything except the vault's own trash, temporary files and earlier exports (the `exports` folder). */
internal fun exportableFiles(fs: FileSystem, root: Path): List<Path> =
    fs.listRecursively(root).filter { p ->
        val rel = p.relativeTo(root).toString()
        !rel.startsWith(".athar/trash") && !rel.startsWith("exports/") && !rel.endsWith(".tmp") && fs.metadataOrNull(p)?.isRegularFile == true
    }.toList().sortedBy { it.toString() }
