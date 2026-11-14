package com.innova.captuia.domain.schedule

import com.innova.captuia.domain.model.ClassSession
import com.innova.captuia.domain.model.Folder
import com.innova.captuia.domain.model.FolderKind
import com.innova.captuia.domain.model.Subject
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.DayOfWeek

class SubjectFormValidatorTest {

    private val calculo = Subject(
        folder = Folder(id = 10, name = "Cálculo Diferencial", kind = FolderKind.SUBJECT),
        sessions = listOf(ClassSession(DayOfWeek.MONDAY, 7 * 60, 9 * 60)),
    )
    private val carpetas = listOf("Documentos", "Tareas", "Cursos", "Imágenes", "Cálculo Diferencial")

    private fun validar(
        nombre: String,
        vararg franjas: SessionInput,
        nombreActual: String? = null,
        otras: List<Subject> = listOf(calculo),
    ) = SubjectFormValidator.validate(nombre, franjas.toList(), carpetas, otras, nombreActual)

    @Test
    fun `acepta una materia nueva con sus franjas`() {
        val result = validar(
            "  Física  Mecánica ",
            SessionInput(DayOfWeek.TUESDAY, "9:00", "11:00"),
            SessionInput(DayOfWeek.THURSDAY, "14:00", "16:00"),
        )
        assertEquals(
            SubjectFormResult.Valid(
                "Física Mecánica",
                listOf(
                    ClassSession(DayOfWeek.TUESDAY, 540, 660),
                    ClassSession(DayOfWeek.THURSDAY, 840, 960),
                ),
            ),
            result,
        )
    }

    @Test
    fun `acepta una materia sin franjas`() {
        assertEquals(SubjectFormResult.Valid("Electiva", emptyList()), validar("Electiva"))
    }

    @Test
    fun `valida el nombre`() {
        assertEquals(SubjectFormResult.NameEmpty, validar("  "))
        assertEquals(SubjectFormResult.NameTooLong, validar("a".repeat(41)))
        assertEquals(SubjectFormResult.NameDuplicate, validar("calculo diferencial"))
        assertEquals(SubjectFormResult.NameDuplicate, validar("Tareas"))
    }

    @Test
    fun `al editar puede conservar su nombre`() {
        val result = validar(
            "Cálculo Diferencial",
            SessionInput(DayOfWeek.MONDAY, "7:00", "9:00"),
            nombreActual = "Cálculo Diferencial",
            otras = emptyList(),
        )
        assertEquals(
            SubjectFormResult.Valid("Cálculo Diferencial", listOf(ClassSession(DayOfWeek.MONDAY, 420, 540))),
            result,
        )
    }

    @Test
    fun `indica cual franja tiene el problema`() {
        assertEquals(SubjectFormResult.DayMissing(0), validar("Física", SessionInput(null, "9:00", "11:00")))
        assertEquals(
            SubjectFormResult.TimeInvalid(1),
            validar(
                "Física",
                SessionInput(DayOfWeek.TUESDAY, "9:00", "11:00"),
                SessionInput(DayOfWeek.FRIDAY, "9", "11:00"),
            ),
        )
        assertEquals(
            SubjectFormResult.EndNotAfterStart(0),
            validar("Física", SessionInput(DayOfWeek.TUESDAY, "9:00", "9:00")),
        )
        assertEquals(
            SubjectFormResult.EndNotAfterStart(0),
            validar("Física", SessionInput(DayOfWeek.TUESDAY, "9:00", "7:00")),
        )
    }

    @Test
    fun `rechaza una franja que se cruza con otra materia`() {
        assertEquals(
            SubjectFormResult.Overlap(0, "Cálculo Diferencial"),
            validar("Física", SessionInput(DayOfWeek.MONDAY, "8:00", "10:00")),
        )
    }

    @Test
    fun `rechaza franjas de la misma materia que se cruzan entre si`() {
        assertEquals(
            SubjectFormResult.Overlap(1, null),
            validar(
                "Física",
                SessionInput(DayOfWeek.TUESDAY, "9:00", "11:00"),
                SessionInput(DayOfWeek.TUESDAY, "10:00", "12:00"),
            ),
        )
    }

    @Test
    fun `una clase puede empezar justo cuando termina otra`() {
        assertEquals(
            SubjectFormResult.Valid("Física", listOf(ClassSession(DayOfWeek.MONDAY, 540, 660))),
            validar("Física", SessionInput(DayOfWeek.MONDAY, "9:00", "11:00")),
        )
    }

    @Test
    fun `el mismo horario en otro dia no es cruce`() {
        assertEquals(
            SubjectFormResult.Valid("Física", listOf(ClassSession(DayOfWeek.TUESDAY, 420, 540))),
            validar("Física", SessionInput(DayOfWeek.TUESDAY, "7:00", "9:00")),
        )
    }
}
