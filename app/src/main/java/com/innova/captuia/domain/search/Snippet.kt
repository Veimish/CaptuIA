package com.innova.captuia.domain.search

/**
 * Fragmento del texto de una captura alrededor de lo que el usuario buscó.
 * [highlights] son los tramos de [text] que coinciden con la búsqueda.
 */
data class Snippet(
    val text: String,
    val highlights: List<IntRange>,
)

object SnippetBuilder {

    private const val CONTEXT_BEFORE = 30
    private const val MAX_LENGTH = 140
    private const val ELLIPSIS = "…"

    /**
     * Construye el fragmento a mostrar para [original] y los términos ya
     * normalizados de la búsqueda ([terms], ver [TextNormalizer.terms]).
     */
    fun build(original: String, terms: List<String>): Snippet {
        // Mismo largo que el original: los índices coinciden carácter a carácter.
        val searchable = buildString(original.length) {
            original.forEach { append(TextNormalizer.normalizeChar(it)) }
        }

        val firstMatch = terms
            .map { searchable.indexOf(it) }
            .filter { it >= 0 }
            .minOrNull()

        var start = if (firstMatch == null) 0 else (firstMatch - CONTEXT_BEFORE).coerceAtLeast(0)
        // No cortar una palabra por la mitad al inicio del fragmento.
        if (start > 0) {
            val nextSpace = searchable.indexOf(' ', start)
            if (nextSpace in start until (firstMatch ?: 0)) start = nextSpace + 1
        }
        val end = (start + MAX_LENGTH).coerceAtMost(original.length)

        val prefix = if (start > 0) ELLIPSIS else ""
        val suffix = if (end < original.length) ELLIPSIS else ""
        val body = buildString(end - start) {
            for (index in start until end) {
                val char = original[index]
                append(if (char.isWhitespace()) ' ' else char)
            }
        }
        val window = searchable.substring(start, end)

        val ranges = mutableListOf<IntRange>()
        terms.forEach { term ->
            var from = window.indexOf(term)
            while (from >= 0) {
                ranges += (prefix.length + from) until (prefix.length + from + term.length)
                from = window.indexOf(term, from + term.length)
            }
        }

        return Snippet(
            text = prefix + body + suffix,
            highlights = merge(ranges),
        )
    }

    /** Une los tramos que se solapan o se tocan, ordenados por posición. */
    private fun merge(ranges: List<IntRange>): List<IntRange> {
        val merged = mutableListOf<IntRange>()
        ranges.sortedBy { it.first }.forEach { range ->
            val last = merged.lastOrNull()
            if (last != null && range.first <= last.last + 1) {
                merged[merged.lastIndex] = last.first..maxOf(last.last, range.last)
            } else {
                merged += range
            }
        }
        return merged
    }
}
