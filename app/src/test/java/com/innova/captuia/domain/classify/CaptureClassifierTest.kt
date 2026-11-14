package com.innova.captuia.domain.classify

import com.innova.captuia.domain.model.ClassSession
import com.innova.captuia.domain.model.Folder
import com.innova.captuia.domain.model.FolderKind
import com.innova.captuia.domain.model.Subject
import com.innova.captuia.domain.model.SystemFolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDateTime

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

    private fun system(folder: SystemFolder) = ClassificationTarget.System(folder)

    private fun keywordsOf(result: Classification): List<String> =
        (result.reason as ClassificationReason.Keywords).words

    private fun subject(id: Long, name: String, vararg sessions: ClassSession) = Subject(
        folder = Folder(id = id, name = name, kind = FolderKind.SUBJECT),
        sessions = sessions.toList(),
    )

    private val calculo = subject(10, "Cálculo Diferencial", ClassSession(DayOfWeek.MONDAY, 7 * 60, 9 * 60))
    private val fisica = subject(11, "Física Mecánica", ClassSession(DayOfWeek.TUESDAY, 9 * 60, 11 * 60))
    private val basesDeDatos = subject(12, "Bases de Datos", ClassSession(DayOfWeek.THURSDAY, 14 * 60, 16 * 60))
    private val poo = subject(13, "Programación Orientada a Objetos", ClassSession(DayOfWeek.WEDNESDAY, 7 * 60, 9 * 60))
    private val materias = listOf(calculo, fisica, basesDeDatos, poo)

    /** Un jueves a las 3:00 p. m.: en clase de Bases de Datos. */
    private val enClaseDeBases: LocalDateTime = LocalDateTime.of(2026, 11, 12, 15, 0)

    /** Un sábado a las 3:00 p. m.: sin clase. */
    private val fueraDeClase: LocalDateTime = LocalDateTime.of(2026, 11, 14, 15, 0)

    // ----- Sin horario registrado -----

    @Test
    fun `un horario de clases va a Cursos`() {
        val result = CaptureClassifier.classify(horario)
        assertEquals(system(SystemFolder.COURSES), result.target)
        assertTrue(keywordsOf(result).containsAll(listOf("horario", "semestre", "clases")))
    }

    @Test
    fun `una tarea con fecha limite va a Tareas aunque mencione un PDF y un dia`() {
        val result = CaptureClassifier.classify(tarea)
        assertEquals(system(SystemFolder.TASKS), result.target)
        assertTrue(keywordsOf(result).containsAll(listOf("tarea", "taller", "fecha limite")))
    }

    @Test
    fun `un chat sobre la sustentacion va a Tareas`() {
        assertEquals(system(SystemFolder.TASKS), CaptureClassifier.classify(chat).target)
    }

    @Test
    fun `una captura sin texto o casi sin texto va a Imagenes`() {
        listOf(null, "", "9:41\n100 %").forEach { text ->
            val result = CaptureClassifier.classify(text)
            assertEquals(system(SystemFolder.IMAGES), result.target)
            assertEquals(ClassificationReason.NoText, result.reason)
        }
    }

    @Test
    fun `texto sin palabras clave va a Documentos`() {
        val result = CaptureClassifier.classify("Aún no encontramos capturas. Toma una y vuelve a la aplicación.")
        assertEquals(system(SystemFolder.DOCUMENTS), result.target)
        assertEquals(ClassificationReason.NoKeywords, result.reason)
    }

    @Test
    fun `un recibo de pago va a Documentos`() {
        val result = CaptureClassifier.classify("Comprobante de pago. Factura No. 4821. Total a pagar: 85.000 pesos. NIT 900.123.456")
        assertEquals(system(SystemFolder.DOCUMENTS), result.target)
    }

    @Test
    fun `solo cuentan palabras completas`() {
        // "clasemedia" y "tareaspendientes" no son las palabras clave "clase" ni "tareas".
        val result = CaptureClassifier.classify("clasemedia tareaspendientes examenfinal otra palabra cualquiera")
        assertEquals(system(SystemFolder.DOCUMENTS), result.target)
    }

    @Test
    fun `no distingue tildes ni mayusculas`() {
        assertEquals(
            system(SystemFolder.TASKS),
            CaptureClassifier.classify("RECUERDEN LA SUSTENTACIÓN Y EL EXAMEN DE MAÑANA").target,
        )
    }

    // ----- Con horario registrado -----

    @Test
    fun `si el texto menciona una materia va a la carpeta de esa materia`() {
        val result = CaptureClassifier.classify(tarea, fueraDeClase, materias)
        assertEquals(ClassificationTarget.SubjectFolder(poo.folder.id), result.target)
        assertEquals(ClassificationReason.SubjectMentioned("Programación Orientada a Objetos"), result.reason)
    }

    @Test
    fun `la mencion de la materia no distingue tildes ni mayusculas`() {
        val result = CaptureClassifier.classify("apuntes de CALCULO DIFERENCIAL: derivadas e integrales", null, materias)
        assertEquals(ClassificationTarget.SubjectFolder(calculo.folder.id), result.target)
    }

    @Test
    fun `una captura del horario completo menciona varias materias y se queda en Cursos`() {
        val result = CaptureClassifier.classify(horario, enClaseDeBases, materias)
        assertEquals(system(SystemFolder.COURSES), result.target)
    }

    @Test
    fun `si menciona varias materias gana la que mas aparece`() {
        val text = "Bases de Datos: taller de consultas. Recuerden que Bases de Datos se cruza con Física Mecánica."
        val result = CaptureClassifier.classify(text, null, materias)
        assertEquals(ClassificationTarget.SubjectFolder(basesDeDatos.folder.id), result.target)
    }

    @Test
    fun `contenido academico tomado durante una clase va a esa materia`() {
        val result = CaptureClassifier.classify(chat, enClaseDeBases, materias)
        assertEquals(ClassificationTarget.SubjectFolder(basesDeDatos.folder.id), result.target)
        assertEquals(ClassificationReason.DuringClass("Bases de Datos"), result.reason)
    }

    @Test
    fun `el mismo contenido fuera de clase sigue las palabras clave`() {
        assertEquals(system(SystemFolder.TASKS), CaptureClassifier.classify(chat, fueraDeClase, materias).target)
    }

    @Test
    fun `contenido no academico tomado durante una clase no va a la materia`() {
        val recibo = "Comprobante de pago. Factura No. 4821. Total a pagar: 85.000 pesos."
        assertEquals(system(SystemFolder.DOCUMENTS), CaptureClassifier.classify(recibo, enClaseDeBases, materias).target)

        val sinPalabrasClave = "Aún no encontramos capturas. Toma una y vuelve a la aplicación."
        assertEquals(
            system(SystemFolder.DOCUMENTS),
            CaptureClassifier.classify(sinPalabrasClave, enClaseDeBases, materias).target,
        )
    }

    @Test
    fun `una imagen sin texto tomada durante una clase va a Imagenes`() {
        assertEquals(system(SystemFolder.IMAGES), CaptureClassifier.classify("", enClaseDeBases, materias).target)
    }

    @Test
    fun `los limites de la franja de clase cuentan como en clase`() {
        val alEmpezar = LocalDateTime.of(2026, 11, 12, 14, 0)
        val alTerminar = LocalDateTime.of(2026, 11, 12, 16, 0)
        val unMinutoDespues = LocalDateTime.of(2026, 11, 12, 16, 1)
        val enClase = ClassificationTarget.SubjectFolder(basesDeDatos.folder.id)
        assertEquals(enClase, CaptureClassifier.classify(chat, alEmpezar, materias).target)
        assertEquals(enClase, CaptureClassifier.classify(chat, alTerminar, materias).target)
        assertEquals(system(SystemFolder.TASKS), CaptureClassifier.classify(chat, unMinutoDespues, materias).target)
    }
}
