package com.innova.captuia

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollToNodeAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.innova.captuia.ui.components.DIALOG_FIELD_TAG
import com.innova.captuia.ui.detail.DETAIL_FOLDER_TAG
import com.innova.captuia.ui.home.FOLDER_ALL_TAG
import com.innova.captuia.ui.home.FOLDER_UNCLASSIFIED_TAG
import com.innova.captuia.ui.home.folderRowTag
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Etapa 3: clasificación automática en carpetas, mover capturas a mano y
 * carpetas propias (crear, renombrar, eliminar).
 * Continúa desde el estado que deja la Etapa 2: cinco capturas ya leídas
 * (una pantalla de la app, un horario, una tarea, un chat y una foto).
 * La pantalla de la app es el Inicio vacío, que muestra "Horario de clases":
 * por esas palabras va a Cursos, igual que el horario.
 */
@RunWith(AndroidJUnit4::class)
class Etapa3CarpetasTest : PruebaBase() {

    private val esMiniatura = hasContentDescription("Captura del", substring = true)
    private val documentos = folderRowTag("Documentos")
    private val tareas = folderRowTag("Tareas")
    private val cursos = folderRowTag("Cursos")
    private val imagenes = folderRowTag("Imágenes")

    @Test
    fun clasificacionYCarpetas() = conCapturaDeFallo("99-fallo-etapa3") {
        esperarTexto(texto(R.string.home_title))

        // 1. CaptuIA ya repartió las cinco capturas en sus carpetas, sin intervención.
        esperar(filaConConteo(cursos, 2), timeoutMillis = 60_000)
        esperar(filaConConteo(tareas, 2))
        esperar(filaConConteo(documentos, 0))
        esperar(filaConConteo(imagenes, 1))
        check(!existe(hasTestTag(FOLDER_UNCLASSIFIED_TAG))) { "No debería quedar ninguna captura sin clasificar." }
        guardarPantalla("17-inicio-con-carpetas", esperaMillis = 1500)

        // 2. Cursos contiene el horario (la más reciente), y el detalle explica por qué está ahí.
        composeRule.onNodeWithTag(cursos).performClick()
        esperarTexto(conteo(2))
        guardarPantalla("18-carpeta-cursos", esperaMillis = 1500)
        composeRule.onAllNodes(esMiniatura).onFirst().performClick()
        esperarTexto(texto(R.string.detail_title))
        composeRule.onNode(hasTestTag(DETAIL_FOLDER_TAG) and hasText("Cursos")).assertIsDisplayed()
        composeRule.onNode(hasTestTag(DETAIL_FOLDER_TAG) and hasText("horario", substring = true))
            .assertIsDisplayed()
        guardarPantalla("19-detalle-con-carpeta", esperaMillis = 1500)

        // 3. El usuario la mueve a Tareas: queda marcada como elegida por él.
        composeRule.onNodeWithTag(DETAIL_FOLDER_TAG).performClick()
        esperarTexto(texto(R.string.detail_move_title))
        guardarPantalla("20-mover-a-carpeta", esperaMillis = 500)
        composeRule.onNodeWithText("Tareas").performClick()
        esperar(hasTestTag(DETAIL_FOLDER_TAG) and hasText("Tareas"))
        composeRule.onNode(
            hasTestTag(DETAIL_FOLDER_TAG) and hasText(texto(R.string.detail_folder_manual)),
        ).assertIsDisplayed()
        guardarPantalla("21-detalle-movida", esperaMillis = 500)

        composeRule.onNodeWithContentDescription(texto(R.string.action_back)).performClick()
        esperarTexto(conteo(1))
        composeRule.onNodeWithContentDescription(texto(R.string.action_back)).performClick()
        esperar(filaConConteo(cursos, 1))
        esperar(filaConConteo(tareas, 3))

        // 4. Al sincronizar de nuevo, CaptuIA respeta la carpeta que eligió el usuario.
        volverALaAppHastaVerTotal(5)
        Thread.sleep(2000)
        check(existe(filaConConteo(tareas, 3))) { "La captura movida a mano cambió de carpeta." }

        // 5. Crear una carpeta propia: se rechazan el nombre vacío y el repetido.
        irA(hasText(texto(R.string.action_new_folder)))
        composeRule.onNodeWithText(texto(R.string.action_new_folder)).performClick()
        esperar(hasTestTag(DIALOG_FIELD_TAG))
        composeRule.onNodeWithText(texto(R.string.action_create)).performClick()
        esperarTexto(texto(R.string.folder_error_empty))
        composeRule.onNodeWithTag(DIALOG_FIELD_TAG).performTextReplacement("tareas")
        composeRule.onNodeWithText(texto(R.string.action_create)).performClick()
        esperarTexto(texto(R.string.folder_error_duplicate))
        guardarPantalla("22-nombre-repetido", esperaMillis = 500)
        composeRule.onNodeWithTag(DIALOG_FIELD_TAG).performTextReplacement("Proyecto final")
        composeRule.onNodeWithText(texto(R.string.action_create)).performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) { !existe(hasTestTag(DIALOG_FIELD_TAG)) }
        val propia = folderRowTag("Proyecto final")
        irA(hasTestTag(propia))
        esperar(filaConConteo(propia, 0))

        // 6. Mover una captura de Tareas a la carpeta propia.
        irA(hasTestTag(tareas))
        composeRule.onNodeWithTag(tareas).performClick()
        esperarTexto(conteo(3))
        composeRule.onAllNodes(esMiniatura).onFirst().performClick()
        esperarTexto(texto(R.string.detail_title))
        composeRule.onNodeWithTag(DETAIL_FOLDER_TAG).performClick()
        esperarTexto(texto(R.string.detail_move_title))
        composeRule.onNodeWithText("Proyecto final").performScrollTo().performClick()
        esperar(hasTestTag(DETAIL_FOLDER_TAG) and hasText("Proyecto final"))
        composeRule.onNodeWithContentDescription(texto(R.string.action_back)).performClick()
        esperarTexto(conteo(2))
        composeRule.onNodeWithContentDescription(texto(R.string.action_back)).performClick()
        esperar(filaConConteo(tareas, 2))
        irA(hasTestTag(propia))
        esperar(filaConConteo(propia, 1))
        guardarPantalla("23-carpeta-propia", esperaMillis = 1000)

        // 7. Renombrar la carpeta propia.
        composeRule.onNodeWithTag(propia).performClick()
        esperarTexto(conteo(1))
        composeRule.onNodeWithContentDescription(texto(R.string.action_folder_options)).performClick()
        composeRule.onNodeWithText(texto(R.string.action_rename_folder)).performClick()
        esperar(hasTestTag(DIALOG_FIELD_TAG))
        composeRule.onNodeWithTag(DIALOG_FIELD_TAG).performTextReplacement("Sustentación")
        composeRule.onNodeWithText(texto(R.string.action_save)).performClick()
        composeRule.waitUntil(timeoutMillis = 15_000) { !existe(hasTestTag(DIALOG_FIELD_TAG)) }
        esperarTexto("Sustentación")
        guardarPantalla("24-carpeta-renombrada", esperaMillis = 1500)

        // 8. Eliminar la carpeta: la captura no se pierde, vuelve a clasificarse sola.
        composeRule.onNodeWithContentDescription(texto(R.string.action_folder_options)).performClick()
        composeRule.onNodeWithText(texto(R.string.action_delete_folder)).performClick()
        esperarTexto(texto(R.string.folder_delete_title, "Sustentación"))
        guardarPantalla("25-confirmar-eliminar", esperaMillis = 500)
        composeRule.onNodeWithText(texto(R.string.action_delete_folder)).performClick()
        esperarTexto(texto(R.string.home_title))
        esperar(filaConConteo(tareas, 3))
        esperar(filaConConteo(FOLDER_ALL_TAG, 5))
        check(!existe(hasTestTag(folderRowTag("Sustentación")))) { "La carpeta eliminada sigue en Inicio." }
        check(!existe(hasTestTag(FOLDER_UNCLASSIFIED_TAG))) { "Quedó una captura sin clasificar tras eliminar la carpeta." }
        guardarPantalla("26-inicio-final", esperaMillis = 1000)
    }

    /** Desplaza la lista de Inicio hasta que el elemento sea visible. */
    private fun irA(elemento: androidx.compose.ui.test.SemanticsMatcher) {
        composeRule.onNode(hasScrollToNodeAction()).performScrollToNode(elemento)
        composeRule.waitForIdle()
    }
}
