package com.innova.captuia.data.processing

import com.innova.captuia.domain.repository.CaptureRepository
import com.innova.captuia.domain.text.TextReader
import com.innova.captuia.domain.usecase.ClassifyCapturesUseCase
import kotlinx.coroutines.CancellationException

/**
 * Procesa las capturas pendientes, una por una: lee su texto, lo guarda en el
 * índice y las clasifica en su carpeta.
 */
class TextIndexer(
    private val repository: CaptureRepository,
    private val textReader: TextReader,
    private val classifyCaptures: ClassifyCapturesUseCase,
) {

    /**
     * Procesa hasta que no queden pendientes o [shouldStop] devuelva true.
     * Devuelve cuántas capturas se procesaron.
     */
    suspend fun processPending(shouldStop: () -> Boolean): Int {
        var processed = 0
        // Capturas leídas antes que quedaron sin carpeta (por ejemplo, al borrar una carpeta).
        classifyCaptures()

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
                // Se clasifica enseguida para que la captura aparezca en su carpeta sin esperar al resto.
                classifyCaptures()
                processed++
            }
        }
    }

    private companion object {
        const val BATCH_SIZE = 20
    }
}
