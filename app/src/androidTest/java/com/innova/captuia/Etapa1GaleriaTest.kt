package com.innova.captuia

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.doubleClick
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import com.innova.captuia.ui.home.FOLDER_ALL_TAG
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Etapa 1: galería, detalle con zoom, compartir y sincronización al borrar.
 * Continúa desde el estado que deja la Etapa 0: permiso concedido y dos capturas.
 */
@RunWith(AndroidJUnit4::class)
class Etapa1GaleriaTest : PruebaBase() {

    private val esMiniatura = hasContentDescription("Captura del", substring = true)

    @Test
    fun galeriaDetalleCompartirYBorrado() = conCapturaDeFallo("99-fallo-etapa1") {
        // 1. "Todas las capturas" muestra las dos capturas del dispositivo, agrupadas en "Hoy".
        esperarTexto(texto(R.string.home_title))
        esperar(filaConConteo(FOLDER_ALL_TAG, 2))
        composeRule.onNodeWithTag(FOLDER_ALL_TAG).performClick()
        esperarTexto(texto(R.string.day_today))
        composeRule.onNodeWithText(conteo(2)).assertIsDisplayed()
        val miniaturas = composeRule.onAllNodes(esMiniatura).fetchSemanticsNodes().size
        check(miniaturas == 2) { "Se esperaban 2 miniaturas y hay $miniaturas." }
        guardarPantalla("05-galeria", esperaMillis = 2000)

        // 2. Al tocar la primera miniatura se abre el detalle de la captura más reciente.
        composeRule.onAllNodes(esMiniatura).onFirst().performClick()
        esperarTexto(texto(R.string.detail_title))
        composeRule.onNodeWithText(CAPTURA_2).assertIsDisplayed()
        composeRule.onNode(hasText("1080 × 2400 px", substring = true)).assertIsDisplayed()
        composeRule.onNodeWithText(texto(R.string.action_share)).assertIsDisplayed()
        guardarPantalla("06-detalle", esperaMillis = 2000)

        // 3. Doble toque sobre la imagen: se amplía.
        composeRule.onNode(esMiniatura).performTouchInput { doubleClick() }
        guardarPantalla("07-detalle-ampliado", esperaMillis = 800)
        composeRule.onNode(esMiniatura).performTouchInput { doubleClick() }

        // 4. "Compartir" abre el selector de apps del sistema.
        composeRule.onNodeWithText(texto(R.string.action_share)).performClick()
        val selector = By.pkg("com.android.intentresolver")
        check(device.wait(Until.hasObject(selector), 10_000) == true) {
            "No se abrió el selector para compartir."
        }
        guardarPantalla("08-compartir", esperaMillis = 1000)
        device.pressBack()
        check(device.wait(Until.gone(selector), 10_000) == true) {
            "El selector para compartir no se cerró."
        }

        // 5. El botón de volver regresa a la galería de la carpeta.
        composeRule.onNodeWithContentDescription(texto(R.string.action_back)).performClick()
        esperarTexto(texto(R.string.day_today))

        // 6. Si el usuario borra una captura del teléfono, desaparece de CaptuIA.
        borrarCapturaEnDispositivo(CAPTURA_1)
        volverALaAppHastaVer(conteo(1))
        val restantes = composeRule.onAllNodes(esMiniatura).fetchSemanticsNodes().size
        check(restantes == 1) { "Tras borrar una captura deberían quedar 1 miniatura y hay $restantes." }
        guardarPantalla("09-galeria-tras-borrar", esperaMillis = 1500)

        // 7. Volver lleva a Inicio, que refleja el mismo total.
        composeRule.onNodeWithContentDescription(texto(R.string.action_back)).performClick()
        esperar(filaConConteo(FOLDER_ALL_TAG, 1))
    }
}
