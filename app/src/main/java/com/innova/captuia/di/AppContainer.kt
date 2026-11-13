package com.innova.captuia.di

import android.content.Context
import com.innova.captuia.data.capture.DefaultCaptureRepository
import com.innova.captuia.data.local.CaptuDatabase
import com.innova.captuia.data.media.ScreenshotSource
import com.innova.captuia.data.ocr.MlKitTextReader
import com.innova.captuia.data.processing.TextIndexer
import com.innova.captuia.data.processing.WorkManagerTextIndexingScheduler
import com.innova.captuia.domain.repository.CaptureRepository
import com.innova.captuia.domain.text.TextIndexingScheduler
import com.innova.captuia.domain.text.TextReader
import com.innova.captuia.domain.usecase.SyncCapturesUseCase

/**
 * Contenedor de dependencias de la aplicación (inyección manual).
 * Cada etapa agrega aquí sus componentes: clasificación, horario, etc.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    private val screenshotSource: ScreenshotSource by lazy {
        ScreenshotSource(appContext.contentResolver)
    }

    private val database: CaptuDatabase by lazy {
        CaptuDatabase.create(appContext)
    }

    private val textReader: TextReader by lazy {
        MlKitTextReader(appContext)
    }

    private val textIndexingScheduler: TextIndexingScheduler by lazy {
        WorkManagerTextIndexingScheduler(appContext)
    }

    val captureRepository: CaptureRepository by lazy {
        DefaultCaptureRepository(screenshotSource, database)
    }

    val textIndexer: TextIndexer by lazy {
        TextIndexer(captureRepository, textReader)
    }

    val syncCaptures: SyncCapturesUseCase by lazy {
        SyncCapturesUseCase(captureRepository, textIndexingScheduler)
    }
}
