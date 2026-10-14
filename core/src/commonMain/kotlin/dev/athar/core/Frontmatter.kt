package dev.athar.core

/** A Markdown file with a small `key: value` header between `---` lines, as Obsidian and many editors use. */
data class Document(val meta: Map<String, String>, val body: String) {
    fun list(key: String): List<String> = meta[key]?.let(::parseList).orEmpty()
}

object Frontmatter {
    fun parse(text: String): Document {
        val normal = text.replace("\r\n", "\n")
        if (!normal.startsWith("---\n")) return Document(emptyMap(), normal)
        val end = normal.indexOf("\n---", 4)
        if (end < 0) return Document(emptyMap(), normal)
        val meta = LinkedHashMap<String, String>()
        normal.substring(4, end).lines().forEach { line ->
            val colon = line.indexOf(':')
            if (colon > 0 && !line.startsWith(" ")) meta[line.substring(0, colon).trim()] = unquote(line.substring(colon + 1).trim())
        }
        val rest = normal.substring(end + 4).removePrefix("\n")
        return Document(meta, rest)
    }

    fun write(doc: Document): String = buildString {
        if (doc.meta.isNotEmpty()) {
            append("---\n")
            doc.meta.forEach { (k, v) -> append(k).append(": ").append(quoteIfNeeded(v)).append('\n') }
            append("---\n")
        }
        append(doc.body)
        if (!doc.body.endsWith("\n")) append('\n')
    }

    fun formatList(values: List<String>): String = values.joinToString(", ", "[", "]") { if (it.any { c -> c == ',' || c == ']' || c == '[' }) "\"$it\"" else it }

    private fun quoteIfNeeded(v: String): String =
        if (v.isEmpty() || v.first().isWhitespace() || v.last().isWhitespace() || v.contains('\n') || v.startsWith("\"") || v.startsWith("'")) "\"" + v.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"" else v

    private fun unquote(v: String): String =
        if (v.length >= 2 && v.first() == '"' && v.last() == '"') v.substring(1, v.length - 1).replace("\\n", "\n").replace("\\\"", "\"").replace("\\\\", "\\")
        else if (v.length >= 2 && v.first() == '\'' && v.last() == '\'') v.substring(1, v.length - 1)
        else v
}

internal fun parseList(value: String): List<String> {
    val inner = value.trim().removePrefix("[").removeSuffix("]")
    if (inner.isBlank()) return emptyList()
    val out = ArrayList<String>()
    val cur = StringBuilder()
    var quoted = false
    for (c in inner) {
        when {
            c == '"' -> quoted = !quoted
            c == ',' && !quoted -> { out += cur.toString().trim(); cur.clear() }
            else -> cur.append(c)
        }
    }
    out += cur.toString().trim()
    return out.filter { it.isNotEmpty() }
}
