package com.innova.captuia.domain.usecase

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class SyncStatus(
    /** True cuando ya terminó al menos una sincronización (con o sin error). */
    val finishedOnce: Boolean = false,
    val failed: Boolean = false,
)

/**
 * Punto único para pedir una sincronización con la galería. Varias pantallas
 * pueden pedirla a la vez: nunca corren dos en paralelo, y una petición que
 * llega durante una sincronización hace que se repita al terminar.
 *
 * [scope] debe usar un único hilo (el principal).
 */
class SyncCoordinator(
    private val scope: CoroutineScope,
    private val syncCaptures: SyncCapturesUseCase,
) {
    private val _status = MutableStateFlow(SyncStatus())
    val status: StateFlow<SyncStatus> = _status.asStateFlow()

    private var job: Job? = null
    private var requestedAgain = false

    fun requestSync() {
        if (job?.isActive == true) {
            requestedAgain = true
            return
        }
        job = scope.launch {
            do {
                requestedAgain = false
                try {
                    syncCaptures()
                    _status.value = SyncStatus(finishedOnce = true, failed = false)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (error: Exception) {
                    _status.value = SyncStatus(finishedOnce = true, failed = true)
                }
            } while (requestedAgain)
        }
    }
}
