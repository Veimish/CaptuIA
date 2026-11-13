package com.innova.captuia.data.capture

import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import com.innova.captuia.data.local.CaptuDatabase
import com.innova.captuia.data.local.CaptureEntity
import com.innova.captuia.data.media.ScreenshotSource
import com.innova.captuia.domain.model.Capture
import com.innova.captuia.domain.model.DeviceScreenshot
import com.innova.captuia.domain.model.TextProgress
import com.innova.captuia.domain.model.TextStatus
import com.innova.captuia.domain.repository.CaptureRepository
import com.innova.captuia.domain.search.TextNormalizer
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
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

    override fun search(query: String): Flow<List<Capture>> {
        val terms = TextNormalizer.terms(query)
        if (terms.isEmpty()) return flowOf(emptyList())

        // Una condición por palabra: la captura debe contenerlas todas.
        val conditions = terms.joinToString(" AND ") { "searchText LIKE ? ESCAPE '\\'" }
        val sql = "SELECT * FROM captures WHERE $conditions " +
            "ORDER BY dateAddedMillis DESC, mediaId DESC"
        val arguments = terms.map { "%${escapeLike(it)}%" }.toTypedArray<Any>()

        return dao.search(SimpleSQLiteQuery(sql, arguments))
            .map { rows -> rows.map { it.toDomain() } }
    }

    override fun observeTextProgress(): Flow<TextProgress> =
        combine(
            dao.observeCountByStatus(TextStatus.PENDING.name),
            dao.observeTotal(),
        ) { pending, total -> TextProgress(pending = pending, total = total) }

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

    override suspend fun nextPendingText(limit: Int): List<Capture> =
        dao.byStatus(TextStatus.PENDING.name, limit).map { it.toDomain() }

    override suspend fun saveText(id: Long, text: String) {
        dao.updateText(
            id = id,
            status = TextStatus.READY.name,
            text = text,
            searchText = TextNormalizer.normalize(text),
        )
    }

    override suspend fun markTextFailed(id: Long) {
        dao.updateText(id = id, status = TextStatus.FAILED.name, text = null, searchText = null)
    }

    /** Evita que %, _ y \ escritos por el usuario actúen como comodines de LIKE. */
    private fun escapeLike(term: String): String =
        term.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_")

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
    textStatus = TextStatus.PENDING.name,
    recognizedText = null,
    searchText = null,
)

private fun CaptureEntity.toDomain() = Capture(
    id = mediaId,
    contentUri = contentUri,
    displayName = displayName,
    dateAddedMillis = dateAddedMillis,
    width = width,
    height = height,
    sizeBytes = sizeBytes,
    textStatus = TextStatus.entries.firstOrNull { it.name == textStatus } ?: TextStatus.PENDING,
    text = recognizedText,
)
