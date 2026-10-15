package dev.athar.core

/** Old names, kept only to find and migrate existing data. Nothing else in the code or the interface uses them. */
object LegacyNames {
    /** Folders (relative to the storage root or home) where a Thabit vault may be, including those of earlier versions. */
    val importFolders = listOf("Zimam/Thabit", "Zimam/Dawem", "Thabit", "Dawem")
}
