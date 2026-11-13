package com.innova.captuia.domain.search

import org.junit.Assert.assertEquals
import org.junit.Test

class TextNormalizerTest {

    @Test
    fun `quita tildes y pasa a minusculas`() {
        assertEquals("calculo diferencial", TextNormalizer.normalize("Cálculo Diferencial"))
        assertEquals("ingenieria aplicada", TextNormalizer.normalize("INGENIERÍA APLICADA"))
    }

    @Test
    fun `reduce espacios y saltos de linea`() {
        assertEquals("tarea pendiente taller 3", TextNormalizer.normalize("  Tarea pendiente\n\nTaller   3 "))
    }

    @Test
    fun `la enie se busca igual que la ene`() {
        assertEquals("ano", TextNormalizer.normalize("Año"))
    }

    @Test
    fun `separa la consulta en terminos sin repetir`() {
        assertEquals(listOf("taller", "viernes"), TextNormalizer.terms("  Taller  VIERNES taller "))
        assertEquals(emptyList<String>(), TextNormalizer.terms("   "))
    }

    @Test
    fun `normalizar un caracter conserva la longitud`() {
        val original = "Física Mecánica\nSalón B-204"
        val porCaracter = original.map { TextNormalizer.normalizeChar(it) }.joinToString("")
        assertEquals(original.length, porCaracter.length)
        assertEquals("fisica mecanica salon b-204", porCaracter)
    }
}
