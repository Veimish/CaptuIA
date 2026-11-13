package com.innova.captuia.domain.search

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SnippetBuilderTest {

    private fun resaltado(snippet: Snippet): List<String> =
        snippet.highlights.map { snippet.text.substring(it) }

    @Test
    fun `resalta la palabra aunque se busque sin tilde`() {
        val snippet = SnippetBuilder.build("Lunes 7:00\nCálculo Diferencial\nSalón B-204", listOf("calculo"))
        assertEquals("Lunes 7:00 Cálculo Diferencial Salón B-204", snippet.text)
        assertEquals(listOf("Cálculo"), resaltado(snippet))
    }

    @Test
    fun `resalta todos los terminos`() {
        val snippet = SnippetBuilder.build(
            "Entregar el taller resuelto antes del viernes 20 de noviembre",
            listOf("taller", "viernes"),
        )
        assertEquals(listOf("taller", "viernes"), resaltado(snippet))
    }

    @Test
    fun `en textos largos recorta alrededor de la coincidencia`() {
        val relleno = "palabra ".repeat(60)
        val snippet = SnippetBuilder.build(relleno + "sustentación del proyecto " + relleno, listOf("sustentacion"))
        assertTrue(snippet.text.startsWith("…"))
        assertTrue(snippet.text.endsWith("…"))
        assertTrue(snippet.text.length <= 142)
        assertEquals(listOf("sustentación"), resaltado(snippet))
    }

    @Test
    fun `sin coincidencias muestra el inicio del texto sin resaltar`() {
        val snippet = SnippetBuilder.build("Horario de clases", listOf("zzz"))
        assertEquals("Horario de clases", snippet.text)
        assertTrue(snippet.highlights.isEmpty())
    }

    @Test
    fun `une coincidencias que se solapan`() {
        val snippet = SnippetBuilder.build("programación", listOf("progra", "gramacion"))
        assertEquals(listOf("programación"), resaltado(snippet))
    }
}
