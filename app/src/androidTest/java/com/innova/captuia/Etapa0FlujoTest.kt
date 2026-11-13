package com.innova.captuia

import android.media.MediaScannerConnection
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.Lifecycle
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Pattern

/**
 * Prueba de extremo a extremo de la Etapa 0, sobre la app real en un emulador:
 * bienvenida -> permiso denegado -> permiso concedido -> detección de capturas.
 *
 * Guarda una imagen de cada paso en /sdcard/Download/captuia-qa para revisión.
 */
@RunWith(AndroidJUnit4::class)
class Etapa0FlujoTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    private val device: UiDevice get() = UiDevice.getInstance(instrumentation)

    private fun texto(id: Int): String = instrumentation.targetContext.getString(id)

    @Test
    fun permisoYDeteccionDeCapturas() {
        try {
            flujoCompleto()
        } catch (error: Throwable) {
            guardarPantalla("99-fallo")
            throw error
        }
    }

    private fun flujoCompleto() {
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
        device.executeShellCommand("mkdir -p /sdcard/Pictures/Screenshots")
        device.executeShellCommand("screencap -p /sdcard/Pictures/Screenshots/Screenshot_20261113-101500.png")
        device.executeShellCommand("screencap -p /sdcard/Pictures/Screenshots/Screenshot_20261113-101530.png")
        // Pide a la galería del sistema que indexe los archivos nuevos.
        MediaScannerConnection.scanFile(
            instrumentation.targetContext,
            arrayOf(
                "/sdcard/Pictures/Screenshots/Screenshot_20261113-101500.png",
                "/sdcard/Pictures/Screenshots/Screenshot_20261113-101530.png",
            ),
            null,
            null,
        )

        var detectadas = false
        for (intento in 1..15) {
            // Simula salir y volver a la app (dispara la actualización automática).
            composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            Thread.sleep(1000)
            composeRule.waitForIdle()
            if (composeRule.onAllNodesWithText("2").fetchSemanticsNodes().isNotEmpty()) {
                detectadas = true
                break
            }
        }
        check(detectadas) { "La app no mostró las 2 capturas creadas en el dispositivo." }
        composeRule.onNodeWithText(texto(R.string.home_found_many)).assertIsDisplayed()
        guardarPantalla("05-inicio-con-capturas")

        // 5. El botón "Volver a buscar" funciona y mantiene el resultado.
        composeRule.onNodeWithText(texto(R.string.home_refresh)).performClick()
        composeRule.waitForIdle()
        esperarTexto("2")
    }

    private fun esperarTexto(texto: String) {
        composeRule.waitUntil(timeoutMillis = 15_000) {
            composeRule.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun esperarDialogoDePermisos() {
        val dialogo = device.wait(
            Until.hasObject(By.res(Pattern.compile(".*:id/permission_.*button"))),
            15_000,
        )
        check(dialogo == true) { "No apareció el diálogo de permisos del sistema." }
    }

    private fun pulsarEnDialogo(idBoton: String, textoAlternativo: String) {
        val boton = device.findObject(By.res(Pattern.compile(".*:id/$idBoton")))
            ?: device.findObject(By.text(Pattern.compile(textoAlternativo)))
        checkNotNull(boton) { "No se encontró el botón del diálogo: $idBoton" }.click()
        device.waitForIdle()
    }

    private fun guardarPantalla(nombre: String) {
        try {
            composeRule.waitForIdle()
        } catch (ignorado: Throwable) {
            // Si la interfaz no queda en reposo, se guarda la pantalla igualmente.
        }
        device.executeShellCommand("mkdir -p $CARPETA_QA")
        device.executeShellCommand("screencap -p $CARPETA_QA/$nombre.png")
    }

    private companion object {
        const val CARPETA_QA = "/sdcard/Download/captuia-qa"
        const val BOTON_DENEGAR = "permission_deny.*button"
        const val BOTON_PERMITIR_TODO = "permission_allow_all_button"
    }
}
