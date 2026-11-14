package com.innova.captuia.domain.usecase

import com.innova.captuia.domain.classify.CaptureClassifier
import com.innova.captuia.domain.classify.ClassificationTarget
import com.innova.captuia.domain.model.Capture
import com.innova.captuia.domain.model.Subject
import com.innova.captuia.domain.repository.CaptureRepository
import com.innova.captuia.domain.repository.FolderRepository
import com.innova.captuia.domain.repository.ScheduleRepository
import java.time.Instant
import java.time.ZoneId

/**
 * Asigna carpeta a las capturas según su contenido y el horario del usuario.
 * Nunca mueve una captura que el usuario ubicó a mano.
 */
class ClassifyCapturesUseCase(
    private val captures: CaptureRepository,
    private val folders: FolderRepository,
    private val schedule: ScheduleRepository,
    private val zone: () -> ZoneId = { ZoneId.systemDefault() },
) {

    /** Clasifica las capturas ya leídas que aún no están en ninguna carpeta. */
    suspend operator fun invoke() {
        folders.ensureSystemFolders()
        val subjects = schedule.subjects()
        while (true) {
            val batch = captures.nextUnclassified(BATCH_SIZE)
            if (batch.isEmpty()) return
            batch.forEach { capture ->
                captures.assignFolderAutomatically(capture.id, folderFor(capture, subjects))
            }
        }
    }

    /**
     * Vuelve a evaluar las capturas que CaptuIA clasificó por su cuenta. Se usa
     * cuando cambia el horario: una materia nueva puede recibir capturas que
     * antes estaban en otra carpeta.
     */
    suspend fun reclassifyAutomatic() {
        folders.ensureSystemFolders()
        val subjects = schedule.subjects()
        captures.automaticallyClassified().forEach { capture ->
            val folderId = folderFor(capture, subjects)
            if (folderId != capture.folderId) {
                captures.assignFolderAutomatically(capture.id, folderId)
            }
        }
    }

    private suspend fun folderFor(capture: Capture, subjects: List<Subject>): Long {
        val takenAt = Instant.ofEpochMilli(capture.dateAddedMillis).atZone(zone()).toLocalDateTime()
        return when (val target = CaptureClassifier.classify(capture.text, takenAt, subjects).target) {
            is ClassificationTarget.System -> folders.systemFolderId(target.folder)
            is ClassificationTarget.SubjectFolder -> target.folderId
        }
    }

    private companion object {
        const val BATCH_SIZE = 50
    }
}
