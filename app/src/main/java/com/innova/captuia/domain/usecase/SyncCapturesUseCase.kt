package com.innova.captuia.domain.usecase

import com.innova.captuia.domain.repository.CaptureRepository
import com.innova.captuia.domain.repository.FolderRepository
import com.innova.captuia.domain.text.TextIndexingScheduler

/**
 * Iguala el índice con la galería del dispositivo y deja programada la
 * lectura de texto y la clasificación de las capturas nuevas.
 */
class SyncCapturesUseCase(
    private val captures: CaptureRepository,
    private val folders: FolderRepository,
    private val scheduler: TextIndexingScheduler,
) {
    suspend operator fun invoke() {
        folders.ensureSystemFolders()
        captures.syncWithDevice()
        scheduler.schedule()
    }
}
