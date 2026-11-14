package com.innova.captuia.domain.classify

import com.innova.captuia.domain.model.Subject
import com.innova.captuia.domain.model.SystemFolder
import com.innova.captuia.domain.search.TextNormalizer
import java.time.LocalDateTime

/** A dónde va una captura. */
sealed interface ClassificationTarget {
    data class System(val folder: SystemFolder) : ClassificationTarget

    /** La carpeta de una materia del horario. */
    data class SubjectFolder(val folderId: Long) : ClassificationTarget
}

/** Por qué se eligió esa carpeta; la app lo muestra al usuario. */
sealed interface ClassificationReason {
    /** El texto contiene estas palabras clave de la carpeta. */
    data class Keywords(val words: List<String>) : ClassificationReason

    /** La captura casi no tiene texto. */
    data object NoText : ClassificationReason

    /** Tiene texto, pero ninguna palabra clave. */
    data object NoKeywords : ClassificationReason

    /** El texto menciona la materia por su nombre. */
    data class SubjectMentioned(val subjectName: String) : ClassificationReason

    /** Contenido académico capturado durante la clase de la materia. */
    data class DuringClass(val subjectName: String) : ClassificationReason
}

data class Classification(
    val target: ClassificationTarget,
    val reason: ClassificationReason,
)

/**
 * Clasificación por reglas sobre el texto leído de la captura y, si el usuario
 * registró su horario, sobre sus materias.
 *
 * Orden de las reglas:
 *  1. Casi sin texto -> Imágenes.
 *  2. El texto menciona una materia por su nombre -> carpeta de esa materia.
 *     Si menciona varias por igual (por ejemplo, una captura del horario
 *     completo), no se decide por ninguna.
 *  3. Contenido académico (palabras de Tareas o Cursos) capturado durante la
 *     clase de una materia -> carpeta de esa materia.
 *  4. Palabras clave: gana la carpeta con mayor puntaje; en empate se prefiere
 *     Tareas, luego Cursos, luego Documentos.
 *  5. Texto sin palabras clave -> Documentos.
 */
object CaptureClassifier {

    /** Con menos palabras que estas se considera que la captura es una imagen. */
    private const val MIN_WORDS_FOR_TEXT = 4

    private val nonWordCharacters = Regex("[^a-z0-9 ]")
    private val spaces = Regex(" +")

    private val keywords: List<Pair<SystemFolder, Map<String, Int>>> = listOf(
        SystemFolder.TASKS to mapOf(
            "tarea" to 3, "tareas" to 3, "taller" to 3, "talleres" to 2,
            "entrega" to 3, "entregar" to 3, "entregable" to 3, "subir entrega" to 3,
            "fecha limite" to 3, "plazo" to 2, "vence" to 2,
            "pendiente" to 2, "pendientes" to 2,
            "parcial" to 3, "parciales" to 3, "quiz" to 3, "examen" to 3, "examenes" to 3,
            "evaluacion" to 2, "exposicion" to 2, "sustentacion" to 3,
            "actividad" to 2, "actividades" to 2, "informe" to 2, "ensayo" to 2,
            "trabajo" to 1, "proyecto" to 1,
        ),
        SystemFolder.COURSES to mapOf(
            "horario" to 3, "horarios" to 3, "semestre" to 3,
            "asignatura" to 3, "asignaturas" to 3, "plan de estudios" to 3, "syllabus" to 3,
            "clase" to 2, "clases" to 2, "salon" to 2, "aula" to 2,
            "profesor" to 2, "profesora" to 2, "docente" to 2,
            "materia" to 2, "materias" to 2, "curso" to 2, "cursos" to 2, "creditos" to 2,
            "laboratorio" to 1, "universidad" to 1, "facultad" to 1,
            "lunes" to 1, "martes" to 1, "miercoles" to 1, "jueves" to 1, "viernes" to 1,
            "sabado" to 1,
        ),
        SystemFolder.DOCUMENTS to mapOf(
            "documento" to 3, "documentos" to 3, "certificado" to 3, "constancia" to 3,
            "factura" to 3, "recibo" to 3, "comprobante" to 3, "contrato" to 3,
            "radicado" to 3, "extracto" to 3, "paz y salvo" to 3,
            "pdf" to 2, "formulario" to 2, "cedula" to 2, "nit" to 2, "firma" to 2,
            "pago" to 2, "resolucion" to 2, "acta" to 2, "carta" to 2, "solicitud" to 2,
            "matricula" to 2, "total" to 1, "pagina" to 1,
        ),
    )

    private val academicFolders = setOf(SystemFolder.TASKS, SystemFolder.COURSES)

    /**
     * @param text texto leído de la captura.
     * @param takenAt cuándo se tomó la captura, en la hora local del usuario.
     * @param subjects materias del horario; vacío si el usuario no lo ha registrado.
     */
    fun classify(
        text: String?,
        takenAt: LocalDateTime? = null,
        subjects: List<Subject> = emptyList(),
    ): Classification {
        val words = wordsOf(text.orEmpty())
        val realWords = words.split(' ').count { word -> word.length >= 2 && word.any { it.isLetter() } }
        if (realWords < MIN_WORDS_FOR_TEXT) {
            return Classification(
                ClassificationTarget.System(SystemFolder.IMAGES),
                ClassificationReason.NoText,
            )
        }

        // Espacios a los lados para encontrar solo palabras o frases completas.
        val haystack = " $words "

        // Regla 2: materias mencionadas por su nombre.
        val mentions = subjects
            .map { subject -> subject to occurrences(haystack, " ${wordsOf(subject.name)} ") }
            .filter { (_, count) -> count > 0 }
        if (mentions.isNotEmpty()) {
            val highest = mentions.maxOf { (_, count) -> count }
            val leaders = mentions.filter { (_, count) -> count == highest }
            if (leaders.size == 1) {
                val subject = leaders.single().first
                return Classification(
                    ClassificationTarget.SubjectFolder(subject.folder.id),
                    ClassificationReason.SubjectMentioned(subject.name),
                )
            }
        }

        val scores = keywords.map { (folder, weights) ->
            val matched = weights.filterKeys { haystack.contains(" $it ") }
            Triple(folder, matched.values.sum(), matched)
        }

        // Regla 3: contenido académico durante una clase.
        val isAcademic = scores.any { (folder, score, _) -> folder in academicFolders && score > 0 }
        if (takenAt != null && isAcademic && mentions.isEmpty()) {
            val minuteOfDay = takenAt.hour * 60 + takenAt.minute
            val inClass = subjects.firstOrNull { subject ->
                subject.sessions.any { session ->
                    session.dayOfWeek == takenAt.dayOfWeek &&
                        minuteOfDay in session.startMinutes..session.endMinutes
                }
            }
            if (inClass != null) {
                return Classification(
                    ClassificationTarget.SubjectFolder(inClass.folder.id),
                    ClassificationReason.DuringClass(inClass.name),
                )
            }
        }

        // Reglas 4 y 5: palabras clave. maxByOrNull conserva el primero en caso de empate.
        val best = scores.filter { (_, score, _) -> score > 0 }.maxByOrNull { (_, score, _) -> score }
            ?: return Classification(
                ClassificationTarget.System(SystemFolder.DOCUMENTS),
                ClassificationReason.NoKeywords,
            )
        val (folder, _, matched) = best
        return Classification(
            ClassificationTarget.System(folder),
            ClassificationReason.Keywords(matched.entries.sortedByDescending { it.value }.map { it.key }),
        )
    }

    /** Texto reducido a palabras en minúsculas, sin tildes ni signos, separadas por un espacio. */
    private fun wordsOf(text: String): String =
        spaces.replace(nonWordCharacters.replace(TextNormalizer.normalize(text), " "), " ").trim()

    /** Veces que [phrase] (con espacios a los lados) aparece en [haystack]. */
    private fun occurrences(haystack: String, phrase: String): Int {
        if (phrase.isBlank()) return 0
        var count = 0
        var from = haystack.indexOf(phrase)
        while (from >= 0) {
            count++
            // Se retrocede un carácter: el espacio final de una coincidencia puede ser el inicial de la siguiente.
            from = haystack.indexOf(phrase, from + phrase.length - 1)
        }
        return count
    }
}
