package com.marshall.pyerite.regionMarketModule.watchlist.model

/**
 * Clipboard text: one `name quantity` line per type.
 * The separator is a tab or other whitespace. Quantity is a positive integer.
 * Blank lines are ignored.
 */
internal object MarketWatchlistImportExport {
    /** Android's regex engine rejects the Java-only `(?U)` flag. */
    private val linePattern = Regex("""^(.+?)[\s\u3000]+(\d+)$""")

    fun format(lines: List<Pair<String, Long>>): String {
        if (lines.isEmpty()) return ""
        return buildString {
            for ((name, quantity) in lines) {
                val safeName = name.replace(MarketWatchlistConfig.IMPORT_SEPARATOR, ' ').trim()
                if (safeName.isEmpty() || quantity <= 0L) continue
                append(safeName)
                append(MarketWatchlistConfig.IMPORT_SEPARATOR)
                append(quantity)
                append('\n')
            }
        }.trimEnd()
    }

    fun parse(text: String): List<ParsedLine> {
        val lines = text
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
        if (lines.isEmpty()) throw MarketWatchlistImportFormatException()

        val parsed = ArrayList<ParsedLine>(lines.size)
        for (line in lines) {
            val match = linePattern.matchEntire(line) ?: throw MarketWatchlistImportFormatException()
            val name = match.groupValues[1].trim()
            val quantityText = match.groupValues[2]
            val quantity = quantityText.toLongOrNull()
            if (
                name.isEmpty() ||
                quantity == null ||
                quantity <= 0L ||
                quantityText.length > MarketWatchlistConfig.QUANTITY_MAX_DIGITS
            ) {
                throw MarketWatchlistImportFormatException()
            }
            parsed += ParsedLine(name = name, quantity = quantity)
        }
        return parsed
    }

    internal data class ParsedLine(
        val name: String,
        val quantity: Long,
    )
}
