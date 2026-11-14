package com.innova.captuia.domain.classify

import com.innova.captuia.domain.model.SystemFolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureClassifierTest {

    private val horario = """
        9:41
        Horario de clases
        Semestre 2026-2 · Ingeniería de Sistemas
        Lunes 7:00 - 9:00 Cálculo Diferencial Salón B-204
        Martes 9:00 - 11:00 Física Mecánica Laboratorio 3
        Jueves 14:00 - 16:00 Bases de Datos Salón C-101
    """.trimIndent()

    private val tarea = """
        Aula virtual
        Tarea pendiente
        Programación Orientada a Objetos
        Taller 3: herencia y polimorfismo
        Entregar el taller resuelto en un archivo PDF antes del viernes 20 de noviembre.
        El trabajo es individual y vale el 15 % de la nota del corte.
        Subir entrega
        Fecha límite: 20/11/2026
    """.trimIndent()

    private val chat = """
        Grupo Ingeniería Aplicada
        Laura
        Muchachos, la sustentación del proyecto quedó para el viernes
        Andrés
        Listo, yo llevo las diapositivas de mercadeo
    """.trimIndent()

    @Test
    fun `un horario de clases va a Cursos`() {
        val result = CaptureClassifier.classify(horario)
        assertEquals(SystemFolder.COURSES, result.folder)
        assertTrue(result.matchedKeywords.containsAll(listOf("horario", "semestre", "clases")))
    }

    @Test
    fun `una tarea con fecha limite va a Tareas aunque mencione un PDF y un dia`() {
        val result = CaptureClassifier.classify(tarea)
        assertEquals(SystemFolder.TASKS, result.folder)
        assertTrue(result.matchedKeywords.containsAll(listOf("tarea", "taller", "fecha limite")))
    }

    @Test
    fun `un chat sobre la sustentacion va a Tareas`() {
        assertEquals(SystemFolder.TASKS, CaptureClassifier.classify(chat).folder)
    }

    @Test
    fun `una captura sin texto o casi sin texto va a Imagenes`() {
        assertEquals(SystemFolder.IMAGES, CaptureClassifier.classify(null).folder)
        assertEquals(SystemFolder.IMAGES, CaptureClassifier.classify("").folder)
        assertEquals(SystemFolder.IMAGES, CaptureClassifier.classify("9:41\n100 %").folder)
    }

    @Test
    fun `texto sin palabras clave va a Documentos`() {
        val result = CaptureClassifier.classify("Aún no encontramos capturas. Toma una y vuelve a la aplicación.")
        assertEquals(SystemFolder.DOCUMENTS, result.folder)
        assertTrue(result.matchedKeywords.isEmpty())
    }

    @Test
    fun `un recibo de pago va a Documentos`() {
        val result = CaptureClassifier.classify("Comprobante de pago. Factura No. 4821. Total a pagar: 85.000 pesos. NIT 900.123.456")
        assertEquals(SystemFolder.DOCUMENTS, result.folder)
    }

    @Test
    fun `solo cuentan palabras completas`() {
        // "clasemedia" y "tareaspendientes" no son las palabras clave "clase" ni "tareas".
        val result = CaptureClassifier.classify("clasemedia tareaspendientes examenfinal otra palabra cualquiera")
        assertEquals(SystemFolder.DOCUMENTS, result.folder)
    }

    @Test
    fun `no distingue tildes ni mayusculas`() {
        assertEquals(
            SystemFolder.TASKS,
            CaptureClassifier.classify("RECUERDEN LA SUSTENTACIÓN Y EL EXAMEN DE MAÑANA").folder,
        )
    }
}
