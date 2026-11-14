package com.innova.captuia.domain.classify

import com.innova.captuia.domain.model.SystemFolder
import com.innova.captuia.domain.search.TextNormalizer

/**
 * Resultado de clasificar una captura: la carpeta y las palabras del texto que
 * llevaron a esa decisión (vacío si se decidió por descarte).
 */
data class Classification(
    val folder: SystemFolder,
    val matchedKeywords: List<String>,
)

/**
 * Clasificación por reglas sobre el texto leído de la captura.
 *
 * Cada carpeta tiene palabras clave con un peso. Gana la carpeta con mayor
 * puntaje; en empate se prefiere Tareas, luego Cursos, luego Documentos.
 * Una captura casi sin texto va a Imágenes y una con texto pero sin palabras
 * clave va a Documentos.
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

    fun classify(text: String?): Classification {
        val words = spaces.replace(
            nonWordCharacters.replace(TextNormalizer.normalize(text.orEmpty()), " "),
            " ",
        ).trim()

        val realWords = words.split(' ').count { word -> word.length >= 2 && word.any { it.isLetter() } }
        if (realWords < MIN_WORDS_FOR_TEXT) {
            return Classification(SystemFolder.IMAGES, emptyList())
        }

        // Espacios a los lados para encontrar solo palabras o frases completas.
        val haystack = " $words "
        var best: Classification? = null
        var bestScore = 0
        keywords.forEach { (folder, weights) ->
            val matched = weights.filterKeys { haystack.contains(" $it ") }
            val score = matched.values.sum()
            // ">" y no ">=": en empate se queda la carpeta que aparece antes en la lista.
            if (score > bestScore) {
                bestScore = score
                best = Classification(
                    folder = folder,
                    matchedKeywords = matched.entries
                        .sortedByDescending { it.value }
                        .map { it.key },
                )
            }
        }
        return best ?: Classification(SystemFolder.DOCUMENTS, emptyList())
    }
}
