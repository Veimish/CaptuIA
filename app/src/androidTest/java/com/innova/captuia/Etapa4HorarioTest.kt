package com.innova.captuia

import androidx.compose.ui.test.ComposeTimeoutException
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.innova.captuia.ui.detail.DETAIL_FOLDER_TAG
import com.innova.captuia.ui.home.FOLDER_ALL_TAG
import com.innova.captuia.ui.home.FOLDER_UNCLASSIFIED_TAG
import com.innova.captuia.ui.home.SCHEDULE_ROW_TAG
import com.innova.captuia.ui.home.folderRowTag
import com.innova.captuia.ui.schedule.SUBJECT_NAME_TAG
import com.innova.captuia.ui.schedule.nameRes
import com.innova.captuia.ui.schedule.sessionDayTag
import com.innova.captuia.ui.schedule.sessionEndTag
import com.innova.captuia.ui.schedule.sessionStartTag
import com.innova.captuia.ui.schedule.subjectCardTag
import org.junit.Test
import org.junit.runner.RunWith
import java.time.DayOfWeek
import java.time.LocalDate

/**
 * Etapa 4: horario de clases. Registrar materias con sus días y horas, y
 * clasificar las capturas por materia (por su nombre en el texto o por la
 * hora en que se tomaron).
 *
 * Continúa desde el estado que deja la Etapa 3: cinco capturas. En Tareas
 * están la tarea, el chat y el horario (este último movido a mano); en Cursos,
 * la pantalla de la app; en Imágenes, la foto.
 */
@RunWith(AndroidJUnit4::class)
class Etapa4HorarioTest : PruebaBase() {

    private val esMiniatura = hasContentDescription("Captura del", substring = true)
    private val tareas = folderRowTag("Tareas")
    private val cursos = folderRowTag("Cursos")
    private val imagenes = folderRowTag("Imágenes")

    private val poo = "Programación Orientada a Objetos"
    private val bases = "Bases de Datos"
    private val basesRenombrada = "Bases de Datos II"

    // Las capturas de la prueba se tomaron hoy (o ayer, si el recorrido cruzó la medianoche).
    private val hoy: DayOfWeek = LocalDate.now().dayOfWeek
    private val ayer: DayOfWeek = hoy.minus(1)

    /** Un día en el que no se tomó ninguna captura de la prueba. */
    private val otroDia: DayOfWeek = hoy.plus(3)

    @Test
    fun horarioDeClases() = conCapturaDeFallo("99-fallo-etapa4") {
        esperarTexto(texto(R.string.home_title))
        esperar(filaConConteo(tareas, 3), timeoutMillis = 60_000)

        // 1. Inicio ofrece el horario de clases; aún no hay materias.
        irA(hasTestTag(SCHEDULE_ROW_TAG))
        composeRule.onNode(hasTestTag(SCHEDULE_ROW_TAG) and hasText(texto(R.string.schedule_row_empty)))
            .assertIsDisplayed()
        guardarPantalla("27-inicio-con-horario", esperaMillis = 1000)
        composeRule.onNodeWithTag(SCHEDULE_ROW_TAG).performClick()
        esperarTexto(texto(R.string.schedule_empty_title))
        guardarPantalla("28-horario-vacio", esperaMillis = 500)

        // 2. El formulario no deja guardar datos incompletos o incoherentes.
        composeRule.onNodeWithText(texto(R.string.action_add_subject)).performClick()
        esperar(hasTestTag(SUBJECT_NAME_TAG))
        guardarYEsperar(hasText(texto(R.string.subject_error_name_empty)))
        escribir(SUBJECT_NAME_TAG, "tareas")
        guardarYEsperar(hasText(texto(R.string.subject_error_name_duplicate)))
        escribir(SUBJECT_NAME_TAG, poo)
        guardarYEsperar(hasText(texto(R.string.subject_error_day_missing)))
        pulsar(sessionDayTag(0, otroDia))
        guardarYEsperar(hasText(texto(R.string.subject_error_time_invalid)))
        escribir(sessionStartTag(0), "9:00")
        escribir(sessionEndTag(0), "7:00")
        guardarYEsperar(hasText(texto(R.string.subject_error_end_before_start)))
        ocultarTeclado()
        guardarPantalla("29-formulario-con-error", esperaMillis = 1000)

        // 3. Con los datos correctos la materia queda en el horario.
        escribir(sessionStartTag(0), "7:00")
        escribir(sessionEndTag(0), "9:00")
        guardarYEsperar(hasTestTag(subjectCardTag(poo)))
        composeRule.onNode(
            hasTestTag(subjectCardTag(poo)) and hasText("${texto(otroDia.nameRes())} · 07:00 – 09:00"),
        ).assertIsDisplayed()
        guardarPantalla("30-horario-con-materia", esperaMillis = 1000)

        // 4. La materia tiene su carpeta, y la tarea que la menciona pasó a ella sola.
        //    El horario que el usuario movió a mano a Tareas no se toca, aunque también la menciona.
        volver()
        esperarTexto(texto(R.string.home_title))
        irA(hasTestTag(SCHEDULE_ROW_TAG))
        esperar(hasTestTag(SCHEDULE_ROW_TAG) and hasText(texto(R.string.schedule_row_one)))
        esperar(filaConConteo(folderRowTag(poo), 1))
        guardarPantalla("31-inicio-con-materia", esperaMillis = 1000)
        irA(hasTestTag(tareas))
        esperar(filaConConteo(tareas, 2))

        irA(hasTestTag(folderRowTag(poo)))
        composeRule.onNodeWithTag(folderRowTag(poo)).performClick()
        esperarTexto(conteo(1))
        composeRule.onAllNodes(esMiniatura).onFirst().performClick()
        esperarTexto(texto(R.string.detail_title))
        composeRule.onNode(hasTestTag(DETAIL_FOLDER_TAG) and hasText(poo)).assertIsDisplayed()
        composeRule.onNode(
            hasTestTag(DETAIL_FOLDER_TAG) and
                hasText(texto(R.string.detail_folder_auto_subject_mentioned, poo)),
        ).assertIsDisplayed()
        guardarPantalla("32-detalle-por-materia", esperaMillis = 1500)
        volver()
        esperarTexto(conteo(1))
        volver()
        esperarTexto(texto(R.string.home_title))

        // 5. Segunda materia. No se aceptan clases que se crucen entre sí ni con otra materia.
        irA(hasTestTag(SCHEDULE_ROW_TAG))
        composeRule.onNodeWithTag(SCHEDULE_ROW_TAG).performClick()
        esperar(hasTestTag(subjectCardTag(poo)))
        composeRule.onNodeWithText(texto(R.string.action_add_subject)).performClick()
        esperar(hasTestTag(SUBJECT_NAME_TAG))
        escribir(SUBJECT_NAME_TAG, bases)
        pulsar(sessionDayTag(0, hoy))
        escribir(sessionStartTag(0), "0:00")
        escribir(sessionEndTag(0), "23:59")
        composeRule.onNodeWithText(texto(R.string.action_add_session)).performScrollTo().performClick()
        esperar(hasTestTag(sessionStartTag(1)))
        pulsar(sessionDayTag(1, hoy))
        escribir(sessionStartTag(1), "10:00")
        escribir(sessionEndTag(1), "12:00")
        guardarYEsperar(hasText(texto(R.string.subject_error_overlap_same)))
        pulsar(sessionDayTag(1, otroDia))
        escribir(sessionStartTag(1), "8:00")
        guardarYEsperar(hasText(texto(R.string.subject_error_overlap_other, poo)))
        ocultarTeclado()
        composeRule.onNodeWithText(texto(R.string.subject_error_overlap_other, poo)).performScrollTo()
        guardarPantalla("33-cruce-de-horarios", esperaMillis = 1000)

        // Clases hoy y ayer, todo el día: cubren el momento en que se tomaron las capturas.
        pulsar(sessionDayTag(1, ayer))
        escribir(sessionStartTag(1), "0:00")
        escribir(sessionEndTag(1), "23:59")
        guardarYEsperar(hasTestTag(subjectCardTag(bases)))
        guardarPantalla("34-horario-con-dos-materias", esperaMillis = 1000)

        // 6. Las capturas de contenido académico tomadas en clase pasan a la materia:
        //    el chat (estaba en Tareas) y la pantalla de la app (estaba en Cursos).
        //    La tarea sigue en la materia que menciona y la foto en Imágenes.
        volver()
        esperarTexto(texto(R.string.home_title))
        irA(hasTestTag(SCHEDULE_ROW_TAG))
        esperar(filaConConteo(folderRowTag(bases), 2))
        esperar(filaConConteo(folderRowTag(poo), 1))
        guardarPantalla("35-inicio-con-dos-materias", esperaMillis = 1000)
        irA(hasTestTag(tareas))
        esperar(filaConConteo(tareas, 1))
        esperar(filaConConteo(cursos, 0))
        esperar(filaConConteo(imagenes, 1))

        irA(hasTestTag(folderRowTag(bases)))
        composeRule.onNodeWithTag(folderRowTag(bases)).performClick()
        esperarTexto(conteo(2))
        composeRule.onAllNodes(esMiniatura).onFirst().performClick()
        esperarTexto(texto(R.string.detail_title))
        composeRule.onNode(
            hasTestTag(DETAIL_FOLDER_TAG) and
                hasText(texto(R.string.detail_folder_auto_during_class, bases)),
        ).assertIsDisplayed()
        guardarPantalla("36-detalle-por-hora-de-clase", esperaMillis = 1500)
        volver()
        esperarTexto(conteo(2))
        volver()
        esperarTexto(texto(R.string.home_title))

        // 7. Editar una materia: el formulario trae sus datos y guarda el nuevo nombre.
        irA(hasTestTag(SCHEDULE_ROW_TAG))
        composeRule.onNodeWithTag(SCHEDULE_ROW_TAG).performClick()
        esperar(hasTestTag(subjectCardTag(bases)))
        composeRule.onNodeWithTag(subjectCardTag(bases)).performClick()
        esperarTexto(texto(R.string.subject_edit_title))
        esperar(hasTestTag(SUBJECT_NAME_TAG) and hasText(bases))
        check(existe(hasTestTag(sessionEndTag(0)) and hasText("23:59"))) {
            "El formulario no cargó las horas guardadas de la materia."
        }
        escribir(SUBJECT_NAME_TAG, basesRenombrada)
        guardarYEsperar(hasTestTag(subjectCardTag(basesRenombrada)))
        check(!existe(hasTestTag(subjectCardTag(bases)))) { "La materia sigue con el nombre anterior." }

        // 8. Eliminar una materia: sus capturas no se pierden, vuelven a sus carpetas.
        composeRule.onNodeWithTag(subjectCardTag(basesRenombrada)).performClick()
        esperarTexto(texto(R.string.subject_edit_title))
        ocultarTeclado()
        composeRule.onNodeWithText(texto(R.string.action_delete_subject)).performScrollTo().performClick()
        esperarTexto(texto(R.string.subject_delete_title, basesRenombrada))
        guardarPantalla("37-confirmar-eliminar-materia", esperaMillis = 500)
        composeRule.onNode(
            hasText(texto(R.string.action_delete_subject)) and hasAnyAncestor(isDialog()),
        ).performClick()
        esperarTexto(texto(R.string.schedule_title))
        composeRule.waitUntil(timeoutMillis = 15_000) { !existe(hasTestTag(subjectCardTag(basesRenombrada))) }
        esperar(hasTestTag(subjectCardTag(poo)))

        volver()
        esperarTexto(texto(R.string.home_title))
        irA(hasTestTag(FOLDER_ALL_TAG))
        esperar(filaConConteo(FOLDER_ALL_TAG, 5))
        esperar(filaConConteo(tareas, 2))
        esperar(filaConConteo(cursos, 1))
        check(!existe(hasTestTag(FOLDER_UNCLASSIFIED_TAG))) { "Quedó una captura sin clasificar tras eliminar la materia." }
        irA(hasTestTag(SCHEDULE_ROW_TAG))
        esperar(hasTestTag(SCHEDULE_ROW_TAG) and hasText(texto(R.string.schedule_row_one)))
        esperar(filaConConteo(folderRowTag(poo), 1))
        check(!existe(hasTestTag(folderRowTag(basesRenombrada)))) { "La carpeta de la materia eliminada sigue en Inicio." }
        guardarPantalla("38-inicio-final", esperaMillis = 1000)
    }

    /**
     * Pulsa "Guardar materia" y espera el resultado (un mensaje de error o la pantalla siguiente).
     * Si el toque coincide con el movimiento del teclado al abrirse puede no llegar al botón,
     * así que se repite mientras el formulario siga igual.
     */
    private fun guardarYEsperar(resultado: SemanticsMatcher) {
        val botonGuardar = hasText(texto(R.string.action_save_subject))
        repeat(4) {
            if (existe(botonGuardar)) composeRule.onNode(botonGuardar).performClick()
            try {
                composeRule.waitUntil(timeoutMillis = 6_000) { existe(resultado) }
                return
            } catch (sinResultado: ComposeTimeoutException) {
                // Se intenta de nuevo.
            }
        }
        error("El formulario no respondió al guardar la materia.")
    }

    private fun volver() {
        composeRule.onNodeWithContentDescription(texto(R.string.action_back)).performClick()
    }

    /** Escribe en un campo del formulario, desplazándolo a la vista si hace falta. */
    private fun escribir(tag: String, valor: String) {
        composeRule.onNodeWithTag(tag).performScrollTo().performTextReplacement(valor)
    }

    private fun pulsar(tag: String) {
        composeRule.onNodeWithTag(tag).performScrollTo().performClick()
    }

    /** Desplaza la lista de la pantalla hasta que el elemento sea visible. */
    private fun irA(elemento: SemanticsMatcher) {
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(elemento)
        composeRule.waitForIdle()
    }
}
