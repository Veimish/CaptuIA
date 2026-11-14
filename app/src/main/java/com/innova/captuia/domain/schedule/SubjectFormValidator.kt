package com.innova.captuia.domain.schedule

import com.innova.captuia.domain.folder.FolderNameCheck
import com.innova.captuia.domain.folder.FolderNameValidator
import com.innova.captuia.domain.model.ClassSession
import com.innova.captuia.domain.model.Subject
import java.time.DayOfWeek

/** Una franja tal como está en el formulario: puede estar incompleta o mal escrita. */
data class SessionInput(
    val dayOfWeek: DayOfWeek?,
    val start: String,
    val end: String,
)

sealed interface SubjectFormResult {
    data class Valid(val name: String, val sessions: List<ClassSession>) : SubjectFormResult

    data object NameEmpty : SubjectFormResult
    data object NameTooLong : SubjectFormResult
    data object NameDuplicate : SubjectFormResult

    /** Problemas de una franja; [row] es su posición en el formulario, desde 0. */
    data class DayMissing(val row: Int) : SubjectFormResult
    data class TimeInvalid(val row: Int) : SubjectFormResult
    data class EndNotAfterStart(val row: Int) : SubjectFormResult

    /** La franja se cruza con otra de [otherSubject] (o de esta misma materia si es null). */
    data class Overlap(val row: Int, val otherSubject: String?) : SubjectFormResult
}

object SubjectFormValidator {

    /**
     * Revisa el formulario de una materia.
     *
     * @param existingFolderNames nombres de todas las carpetas (una materia es una carpeta).
     * @param otherSubjects las demás materias del horario, sin incluir la que se edita.
     * @param currentName nombre actual de la materia cuando se está editando.
     */
    fun validate(
        name: String,
        sessions: List<SessionInput>,
        existingFolderNames: Collection<String>,
        otherSubjects: List<Subject>,
        currentName: String? = null,
    ): SubjectFormResult {
        val cleanName = when (val check = FolderNameValidator.check(name, existingFolderNames, currentName)) {
            is FolderNameCheck.Valid -> check.name
            FolderNameCheck.Empty -> return SubjectFormResult.NameEmpty
            FolderNameCheck.TooLong -> return SubjectFormResult.NameTooLong
            FolderNameCheck.Duplicate -> return SubjectFormResult.NameDuplicate
        }

        val parsed = mutableListOf<ClassSession>()
        sessions.forEachIndexed { row, input ->
            val day = input.dayOfWeek ?: return SubjectFormResult.DayMissing(row)
            val start = TimeOfDay.parse(input.start) ?: return SubjectFormResult.TimeInvalid(row)
            val end = TimeOfDay.parse(input.end) ?: return SubjectFormResult.TimeInvalid(row)
            if (end <= start) return SubjectFormResult.EndNotAfterStart(row)
            val session = ClassSession(day, start, end)

            if (parsed.any { overlaps(it, session) }) {
                return SubjectFormResult.Overlap(row, otherSubject = null)
            }
            otherSubjects.forEach { other ->
                if (other.sessions.any { overlaps(it, session) }) {
                    return SubjectFormResult.Overlap(row, otherSubject = other.name)
                }
            }
            parsed += session
        }

        return SubjectFormResult.Valid(cleanName, parsed)
    }

    /** Dos franjas se cruzan si son el mismo día y comparten algún minuto. Terminar a las 9:00 y empezar a las 9:00 no es cruce. */
    private fun overlaps(a: ClassSession, b: ClassSession): Boolean =
        a.dayOfWeek == b.dayOfWeek && a.startMinutes < b.endMinutes && b.startMinutes < a.endMinutes
}
