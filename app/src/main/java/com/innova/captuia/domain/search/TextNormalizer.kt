package com.innova.captuia.domain.search

import java.text.Normalizer

/**
 * Normaliza texto para buscar sin distinguir mayúsculas ni tildes:
 * "Cálculo" y "calculo" se consideran iguales.
 */
object TextNormalizer {

    private val combiningMarks = Regex("\\p{Mn}+")
    private val whitespace = Regex("\\s+")

    /** Minúsculas, sin tildes y con los espacios (incluidos saltos de línea) reducidos a uno. */
    fun normalize(text: String): String =
        whitespace.replace(stripMarks(text.lowercase()), " ").trim()

    /** Palabras de una consulta del usuario, ya normalizadas. */
    fun terms(query: String): List<String> =
        normalize(query).split(' ').filter { it.isNotEmpty() }.distinct()

    /**
     * Normaliza un único carácter sin cambiar la cantidad de caracteres del texto
     * (cada carácter de entrada produce exactamente uno de salida). Permite
     * localizar en el texto original lo que se encontró en el normalizado.
     */
    fun normalizeChar(char: Char): Char {
        if (char.isWhitespace()) return ' '
        val base = stripMarks(char.lowercaseChar().toString())
        return if (base.length == 1) base[0] else char.lowercaseChar()
    }

    private fun stripMarks(text: String): String =
        combiningMarks.replace(Normalizer.normalize(text, Normalizer.Form.NFD), "")
}
