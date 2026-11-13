package com.innova.captuia.data.capture

import androidx.room.withTransaction
import com.innova.captuia.data.local.CaptuDatabase
import com.innova.captuia.data.local.CaptureEntity
import com.innova.captuia.data.media.ScreenshotSource
import com.innova.captuia.domain.model.Capture
import com.innova.captuia.domain.model.DeviceScreenshot
import com.innova.captuia.domain.repository.CaptureRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/** Índice de capturas respaldado por Room y sincronizado con MediaStore. */
class DefaultCaptureRepository(
    private val screenshotSource: ScreenshotSource,
    private val database: CaptuDatabase,
) : CaptureRepository {

    private val dao = database.captureDao()

    override fun observeAll(): Flow<List<Capture>> =
        dao.observeAll().map { rows -> rows.map { it.toDomain() } }

    override fun observeById(id: Long): Flow<Capture?> =
        dao.observeById(id).map { it?.toDomain() }

    override suspend fun syncWithDevice() {
        // Si la lectura de la galería falla, se lanza la excepción antes de tocar el índice.
        val onDevice = screenshotSource.loadAll()
        val deviceIds = onDevice.mapTo(HashSet()) { it.mediaId }

        database.withTransaction {
            val indexedIds = dao.allIds().toHashSet()

            val added = onDevice.filter { it.mediaId !in indexedIds }
            added.chunked(BATCH_SIZE).forEach { batch ->
                dao.insertNew(batch.map { it.toEntity() })
            }

            val removed = indexedIds.filter { it !in deviceIds }
            removed.chunked(BATCH_SIZE).forEach { batch ->
                dao.deleteByIds(batch)
            }
        }
    }

    private companion object {
        /** SQLite limita el número de parámetros por consulta. */
        const val BATCH_SIZE = 500
    }
}

private fun DeviceScreenshot.toEntity() = CaptureEntity(
    mediaId = mediaId,
    contentUri = contentUri,
    displayName = displayName,
    dateAddedMillis = dateAddedMillis,
    width = width,
    height = height,
    sizeBytes = sizeBytes,
)

private fun CaptureEntity.toDomain() = Capture(
    id = mediaId,
    contentUri = contentUri,
    displayName = displayName,
    dateAddedMillis = dateAddedMillis,
    width = width,
    height = height,
    sizeBytes = sizeBytes,
)
