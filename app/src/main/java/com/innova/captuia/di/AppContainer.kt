package com.innova.captuia.di

import android.content.Context
import com.innova.captuia.data.media.ScreenshotSource

/**
 * Contenedor de dependencias de la aplicación (inyección manual).
 * Cada etapa agrega aquí sus componentes: base de datos, repositorios, OCR, etc.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    val screenshotSource: ScreenshotSource by lazy {
        ScreenshotSource(appContext.contentResolver)
    }
}
