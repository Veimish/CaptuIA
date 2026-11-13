package com.innova.captuia

import android.media.MediaScannerConnection
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import org.junit.Before
import org.junit.Rule
import java.util.regex.Pattern

/**
 * Utilidades comunes de las pruebas de interfaz. Cada prueba usa la app real
 * en el emulador y guarda una imagen de cada paso para su revisión.
 */
abstract class PruebaBase {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    protected val instrumentation get() = InstrumentationRegistry.getInstrumentation()
    protected val device: UiDevice get() = UiDevice.getInstance(instrumentation)

    /**
     * Un emulador lento puede mostrar "La app X no responde" sobre la pantalla.
     * No es parte de CaptuIA: se pulsa "Esperar" para que no tape lo que se prueba.
     */
    @Before
    fun cerrarDialogosDelSistema() {
        repeat(3) {
            val esperar = device.findObject(By.res("android", "aerr_wait"))
                ?: device.findObject(By.text(Pattern.compile("(?i)^(wait|esperar)$")))
                ?: return
            esperar.click()
            device.waitForIdle()
        }
    }

    protected fun texto(id: Int, vararg args: Any): String =
        instrumentation.targetContext.getString(id, *args)

    /** Ejecuta el recorrido y, si falla, guarda la pantalla del momento del fallo. */
    protected fun conCapturaDeFallo(nombre: String, recorrido: () -> Unit) {
        try {
            recorrido()
        } catch (error: Throwable) {
            guardarPantalla(nombre)
            throw error
        }
    }

    protected fun esperarTexto(texto: String, timeoutMillis: Long = 15_000) {
        composeRule.waitUntil(timeoutMillis = timeoutMillis) {
            composeRule.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /**
     * Simula salir de la app y volver (lo que dispara la sincronización con la
     * galería) hasta que aparezca [texto].
     */
    protected fun volverALaAppHastaVer(texto: String, intentos: Int = 15) {
        repeat(intentos) {
            composeRule.activityRule.scenario.moveToState(Lifecycle.State.STARTED)
            composeRule.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
            Thread.sleep(1000)
            composeRule.waitForIdle()
            if (composeRule.onAllNodesWithText(texto).fetchSemanticsNodes().isNotEmpty()) return
        }
        error("La app no mostró \"$texto\" después de volver a primer plano.")
    }

    /** Toma una captura de pantalla real del dispositivo, como lo haría el usuario. */
    protected fun tomarCapturaEnDispositivo(nombreArchivo: String) {
        device.executeShellCommand("mkdir -p $CARPETA_CAPTURAS")
        device.executeShellCommand("screencap -p $CARPETA_CAPTURAS/$nombreArchivo")
        indexarEnGaleria(nombreArchivo)
    }

    /**
     * Pone en la carpeta de capturas del dispositivo una de las imágenes de
     * ejemplo (ci/muestras), como si el usuario acabara de tomarla.
     */
    protected fun copiarMuestraComoCaptura(muestra: String, nombreArchivo: String) {
        device.executeShellCommand("mkdir -p $CARPETA_CAPTURAS")
        device.executeShellCommand("cp $CARPETA_MUESTRAS/$muestra $CARPETA_CAPTURAS/$nombreArchivo")
        indexarEnGaleria(nombreArchivo)
        // MediaStore registra la fecha en segundos: se separan para conservar el orden.
        Thread.sleep(1100)
    }

    /** Borra una captura del dispositivo, como lo haría el usuario desde su galería. */
    protected fun borrarCapturaEnDispositivo(nombreArchivo: String) {
        device.executeShellCommand("rm $CARPETA_CAPTURAS/$nombreArchivo")
        indexarEnGaleria(nombreArchivo)
    }

    private fun indexarEnGaleria(nombreArchivo: String) {
        MediaScannerConnection.scanFile(
            instrumentation.targetContext,
            arrayOf("$CARPETA_CAPTURAS/$nombreArchivo"),
            null,
            null,
        )
    }

    /** Guarda la pantalla actual en la carpeta de evidencias de la prueba. */
    protected fun guardarPantalla(nombre: String, esperaMillis: Long = 0) {
        try {
            composeRule.waitForIdle()
        } catch (ignorado: Throwable) {
            // Si la interfaz no queda en reposo, se guarda la pantalla igualmente.
        }
        if (esperaMillis > 0) Thread.sleep(esperaMillis)
        device.executeShellCommand("mkdir -p $CARPETA_QA")
        device.executeShellCommand("screencap -p $CARPETA_QA/$nombre.png")
    }

    protected companion object {
        const val CARPETA_QA = "/sdcard/Download/captuia-qa"
        const val CARPETA_CAPTURAS = "/sdcard/Pictures/Screenshots"
        const val CARPETA_MUESTRAS = "/data/local/tmp/captuia-muestras"
        const val CAPTURA_1 = "Screenshot_20261113-101500.png"
        const val CAPTURA_2 = "Screenshot_20261113-101530.png"
    }
}
