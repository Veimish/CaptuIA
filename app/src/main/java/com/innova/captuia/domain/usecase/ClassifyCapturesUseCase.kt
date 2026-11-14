package com.innova.captuia.domain.usecase

import com.innova.captuia.domain.classify.CaptureClassifier
import com.innova.captuia.domain.repository.CaptureRepository
import com.innova.captuia.domain.repository.FolderRepository

/**
 * Asigna carpeta a las capturas que ya tienen su texto leído y aún no están
 * en ninguna. Nunca mueve una captura que el usuario ubicó a mano.
 */
class ClassifyCapturesUseCase(
    private val captures: CaptureRepository,
    private val folders: FolderRepository,
) {
    suspend operator fun invoke() {
        folders.ensureSystemFolders()
        while (true) {
            val batch = captures.nextUnclassified(BATCH_SIZE)
            if (batch.isEmpty()) return
            batch.forEach { capture ->
                val folder = CaptureClassifier.classify(capture.text).folder
                captures.assignFolderAutomatically(capture.id, folders.systemFolderId(folder))
            }
        }
    }

    private companion object {
        const val BATCH_SIZE = 50
    }
}
