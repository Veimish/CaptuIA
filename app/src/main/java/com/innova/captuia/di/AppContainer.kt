package com.innova.captuia.di

import android.content.Context
import com.innova.captuia.R
import com.innova.captuia.data.capture.DefaultCaptureRepository
import com.innova.captuia.data.folder.DefaultFolderRepository
import com.innova.captuia.data.local.CaptuDatabase
import com.innova.captuia.data.media.ScreenshotSource
import com.innova.captuia.data.ocr.MlKitTextReader
import com.innova.captuia.data.processing.TextIndexer
import com.innova.captuia.data.schedule.DefaultScheduleRepository
import com.innova.captuia.data.processing.WorkManagerTextIndexingScheduler
import com.innova.captuia.domain.model.SystemFolder
import com.innova.captuia.domain.repository.CaptureRepository
import com.innova.captuia.domain.repository.FolderRepository
import com.innova.captuia.domain.repository.ScheduleRepository
import com.innova.captuia.domain.text.TextIndexingScheduler
import com.innova.captuia.domain.text.TextReader
import com.innova.captuia.domain.usecase.ClassifyCapturesUseCase
import com.innova.captuia.domain.usecase.SyncCapturesUseCase
import com.innova.captuia.domain.usecase.SyncCoordinator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Contenedor de dependencias de la aplicación (inyección manual).
 * Cada etapa agrega aquí sus componentes.
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    /** Vive mientras viva el proceso de la app; usa el hilo principal. */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

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

    val folderRepository: FolderRepository by lazy {
        DefaultFolderRepository(
            database = database,
            systemFolderNames = mapOf(
                SystemFolder.DOCUMENTS to appContext.getString(R.string.folder_documents),
                SystemFolder.TASKS to appContext.getString(R.string.folder_tasks),
                SystemFolder.COURSES to appContext.getString(R.string.folder_courses),
                SystemFolder.IMAGES to appContext.getString(R.string.folder_images),
            ),
        )
    }

    val scheduleRepository: ScheduleRepository by lazy {
        DefaultScheduleRepository(database)
    }

    val classifyCaptures: ClassifyCapturesUseCase by lazy {
        ClassifyCapturesUseCase(captureRepository, folderRepository, scheduleRepository)
    }

    val textIndexer: TextIndexer by lazy {
        TextIndexer(captureRepository, textReader, classifyCaptures)
    }

    val syncCoordinator: SyncCoordinator by lazy {
        SyncCoordinator(
            scope = applicationScope,
            syncCaptures = SyncCapturesUseCase(captureRepository, folderRepository, textIndexingScheduler),
        )
    }
}
