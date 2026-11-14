package com.innova.captuia

import android.content.ClipboardManager
import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.innova.captuia.ui.components.SEARCH_FIELD_TAG
import com.innova.captuia.ui.home.FOLDER_ALL_TAG
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Etapa 2: lectura de texto (OCR) y búsqueda por contenido.
 * Continúa desde el estado que deja la Etapa 1: permiso concedido y una captura.
 */
@RunWith(AndroidJUnit4::class)
class Etapa2TextoYBusquedaTest : PruebaBase() {

    private val esMiniatura = hasContentDescription("Captura del", substring = true)

    @Test
    fun lecturaDeTextoYBusqueda() = conCapturaDeFallo("99-fallo-etapa2") {
        esperarTexto(texto(R.string.home_title))

        // 1. Llegan cuatro capturas nuevas: un horario, una tarea, un chat y una foto sin texto.
        copiarMuestraComoCaptura("horario.png", "Screenshot_20261113-110000.png")
        copiarMuestraComoCaptura("tarea.png", "Screenshot_20261113-110100.png")
        copiarMuestraComoCaptura("chat.png", "Screenshot_20261113-110200.png")
        copiarMuestraComoCaptura("foto.png", "Screenshot_20261113-110300.png")
        volverALaAppHastaVerTotal(5)

        // 2. La app lee el texto de todas en segundo plano, sin intervención.
        Thread.sleep(1500)
        composeRule.waitUntil(timeoutMillis = 120_000) {
            composeRule.onAllNodes(hasText("Leyendo el texto", substring = true))
                .fetchSemanticsNodes().isEmpty()
        }
        guardarPantalla("10-galeria-con-buscador", esperaMillis = 1500)

        // 3. Buscar sin tilde encuentra la palabra con tilde.
        buscar("calculo")
        esperarTexto(texto(R.string.search_count_one))
        composeRule.onNode(hasText("Cálculo Diferencial", substring = true)).assertIsDisplayed()
        guardarPantalla("11-busqueda-calculo", esperaMillis = 1500)

        // 4. Una palabra que está en tres capturas distintas.
        buscar("viernes")
        esperarTexto(texto(R.string.search_count_many, "3"))
        guardarPantalla("12-busqueda-viernes", esperaMillis = 1500)

        // 5. Dos palabras: solo la captura que contiene ambas.
        buscar("taller viernes")
        esperarTexto(texto(R.string.search_count_one))
        composeRule.onNode(hasText("taller resuelto", substring = true)).assertIsDisplayed()

        // 6. Una búsqueda sin coincidencias lo dice con claridad.
        buscar("zzzqqq")
        esperarTexto(texto(R.string.search_empty_title, "zzzqqq"))
        guardarPantalla("13-sin-resultados")

        // 7. El botón de borrar devuelve la galería completa.
        composeRule.onNodeWithContentDescription(texto(R.string.action_clear_search)).performClick()
        esperar(filaConConteo(FOLDER_ALL_TAG, 5))

        // 8. Desde un resultado se abre el detalle con el texto leído, y se puede copiar.
        buscar("semestre")
        esperarTexto(texto(R.string.search_count_one))
        composeRule.onNode(hasText("Semestre", substring = true)).performClick()
        esperarTexto(texto(R.string.detail_text_label))
        composeRule.onNode(hasText("Cálculo Diferencial", substring = true)).assertExists()
        composeRule.onNode(hasText("Bases de Datos", substring = true)).assertExists()
        composeRule.onNodeWithText(texto(R.string.action_copy_text)).performClick()
        esperarTexto(texto(R.string.action_text_copied))
        val copiado = textoDelPortapapeles()
        check(copiado.contains("Horario de clases")) {
            "El portapapeles no tiene el texto de la captura: \"$copiado\""
        }
        guardarPantalla("14-detalle-con-texto", esperaMillis = 1500)

        composeRule.onNodeWithText(texto(R.string.action_show_all_text)).performClick()
        esperarTexto(texto(R.string.action_show_less_text))
        guardarPantalla("15-detalle-texto-completo", esperaMillis = 500)
        composeRule.onNodeWithContentDescription(texto(R.string.action_back)).performClick()

        // 9. Una captura sin texto (la foto, que es la más reciente) lo indica.
        esperarTexto(texto(R.string.search_count_one))
        composeRule.onNodeWithContentDescription(texto(R.string.action_clear_search)).performClick()
        esperar(filaConConteo(FOLDER_ALL_TAG, 5))
        composeRule.onNodeWithTag(FOLDER_ALL_TAG).performClick()
        esperarTexto(texto(R.string.day_today))
        composeRule.onAllNodes(esMiniatura).onFirst().performClick()
        esperarTexto(texto(R.string.detail_text_none))
        composeRule.onNodeWithText("Screenshot_20261113-110300.png").assertIsDisplayed()
        guardarPantalla("16-detalle-sin-texto", esperaMillis = 1500)
        composeRule.onNodeWithContentDescription(texto(R.string.action_back)).performClick()
        esperarTexto(texto(R.string.day_today))
        composeRule.onNodeWithContentDescription(texto(R.string.action_back)).performClick()
        esperarTexto(texto(R.string.home_title))
    }

    private fun buscar(consulta: String) {
        composeRule.onNodeWithTag(SEARCH_FIELD_TAG).performTextReplacement(consulta)
    }

    private fun textoDelPortapapeles(): String {
        var contenido = ""
        instrumentation.runOnMainSync {
            val portapapeles = composeRule.activity
                .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            contenido = portapapeles.primaryClip?.getItemAt(0)?.text?.toString().orEmpty()
        }
        return contenido
    }
}
