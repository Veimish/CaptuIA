package com.innova.captuia.domain.usecase

import com.innova.captuia.domain.repository.CaptureRepository
import com.innova.captuia.domain.text.TextIndexingScheduler

/**
 * Iguala el índice con la galería del dispositivo y deja programada la
 * lectura de texto de las capturas nuevas.
 */
class SyncCapturesUseCase(
    private val repository: CaptureRepository,
    private val scheduler: TextIndexingScheduler,
) {
    suspend operator fun invoke() {
        repository.syncWithDevice()
        scheduler.schedule()
    }
}
