package com.innova.captuia.di

import android.content.Context
import com.innova.captuia.data.capture.DefaultCaptureRepository
import com.innova.captuia.data.local.CaptuDatabase
import com.innova.captuia.data.media.ScreenshotSource
import com.innova.captuia.domain.repository.CaptureRepository

/**
 * Contenedor de dependencias de la aplicación (inyección manual).
 * Cada etapa agrega aquí sus componentes: OCR, clasificación, horario, etc.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    private val screenshotSource: ScreenshotSource by lazy {
        ScreenshotSource(appContext.contentResolver)
    }

    private val database: CaptuDatabase by lazy {
        CaptuDatabase.create(appContext)
    }

    val captureRepository: CaptureRepository by lazy {
        DefaultCaptureRepository(screenshotSource, database)
    }
}
