package com.innova.captuia

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

/**
 * Etapa 0: bienvenida -> permiso denegado -> permiso concedido -> detección
 * de capturas. Parte de una instalación limpia, sin permisos.
 */
@RunWith(AndroidJUnit4::class)
class Etapa0PermisosTest : PruebaBase() {

    @Test
    fun permisoYDeteccionDeCapturas() = conCapturaDeFallo("99-fallo-etapa0") {
        // 1. Primera ejecución: sin permiso, se muestra la bienvenida.
        composeRule.onNodeWithText(texto(R.string.welcome_headline)).assertIsDisplayed()
        composeRule.onNodeWithText(texto(R.string.welcome_step1_title)).assertExists()
        composeRule.onNodeWithText(texto(R.string.welcome_step4_title)).assertExists()
        composeRule.onNodeWithText(texto(R.string.welcome_privacy)).assertExists()
        composeRule.onNodeWithText(texto(R.string.welcome_cta)).assertIsDisplayed()
        guardarPantalla("01-bienvenida")

        // 2. El botón amarillo abre el diálogo de permisos del sistema. Se deniega.
        composeRule.onNodeWithText(texto(R.string.welcome_cta)).performClick()
        esperarDialogoDePermisos()
        guardarPantalla("02-dialogo-del-sistema")
        pulsarEnDialogo(BOTON_DENEGAR, "(?i)don.t allow|no permitir")

        esperarTexto(texto(R.string.welcome_denied))
        composeRule.onNodeWithText(texto(R.string.action_open_settings)).assertIsDisplayed()
        guardarPantalla("03-permiso-denegado")

        // 3. Segundo intento: se concede acceso a todas las fotos y se llega a Inicio.
        composeRule.onNodeWithText(texto(R.string.welcome_cta)).performClick()
        esperarDialogoDePermisos()
        pulsarEnDialogo(BOTON_PERMITIR_TODO, "(?i)allow all|permitir todo|^allow$|^permitir$")

        esperarTexto(texto(R.string.home_title))
        esperarTexto(texto(R.string.home_empty_title))
        guardarPantalla("04-inicio-sin-capturas")

        // 4. Se toman dos capturas reales en el dispositivo; al volver a la app las detecta.
        tomarCapturaEnDispositivo(CAPTURA_1)
        tomarCapturaEnDispositivo(CAPTURA_2)
        volverALaAppHastaVerTotal(2)
    }

    private fun esperarDialogoDePermisos() {
        cerrarDialogosDelSistema()
        val aparecio = device.wait(
            Until.hasObject(By.res(Pattern.compile(".*:id/permission_.*button"))),
            15_000,
        )
        check(aparecio == true) { "No apareció el diálogo de permisos del sistema." }
    }

    private fun pulsarEnDialogo(idBoton: String, textoAlternativo: String) {
        val boton = device.findObject(By.res(Pattern.compile(".*:id/$idBoton")))
            ?: device.findObject(By.text(Pattern.compile(textoAlternativo)))
        checkNotNull(boton) { "No se encontró el botón del diálogo: $idBoton" }.click()
        device.waitForIdle()
    }

    private companion object {
        const val BOTON_DENEGAR = "permission_deny.*button"
        const val BOTON_PERMITIR_TODO = "permission_allow_all_button"
    }
}
