package com.innova.captuia.data.processing

import com.innova.captuia.domain.repository.CaptureRepository
import com.innova.captuia.domain.text.TextReader
import kotlinx.coroutines.CancellationException

/** Lee el texto de las capturas pendientes, una por una, y lo guarda en el índice. */
class TextIndexer(
    private val repository: CaptureRepository,
    private val textReader: TextReader,
) {

    /**
     * Procesa hasta que no queden pendientes o [shouldStop] devuelva true.
     * Devuelve cuántas capturas se procesaron.
     */
    suspend fun processPending(shouldStop: () -> Boolean): Int {
        var processed = 0
        while (true) {
            val batch = repository.nextPendingText(BATCH_SIZE)
            if (batch.isEmpty()) return processed

            for (capture in batch) {
                if (shouldStop()) return processed
                try {
                    repository.saveText(capture.id, textReader.read(capture.contentUri))
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Exception) {
                    // Archivo ilegible o borrado: se marca para no reintentarlo sin fin.
                    repository.markTextFailed(capture.id)
                }
                processed++
            }
        }
    }

    private companion object {
        const val BATCH_SIZE = 20
    }
}
