package com.innova.captuia.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.innova.captuia.CaptuIAApplication
import com.innova.captuia.domain.repository.CaptureRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class HomeUiState(
    /** True solo mientras se hace la primera lectura de la galería. */
    val isLoading: Boolean = true,
    val total: Int = 0,
    val sections: List<DaySection> = emptyList(),
    val hasError: Boolean = false,
)

class HomeViewModel(
    private val repository: CaptureRepository,
) : ViewModel() {

    private data class SyncStatus(val finishedOnce: Boolean = false, val failed: Boolean = false)

    private val syncStatus = MutableStateFlow(SyncStatus())
    private var syncJob: Job? = null

    val state: StateFlow<HomeUiState> =
        combine(repository.observeAll(), syncStatus) { captures, sync ->
            HomeUiState(
                isLoading = !sync.finishedOnce && captures.isEmpty(),
                total = captures.size,
                sections = groupByDay(captures, LocalDate.now(), ZoneId.systemDefault()),
                hasError = sync.failed,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    /** Actualiza el índice con las capturas que hay ahora en el dispositivo. */
    fun syncWithDevice() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            try {
                repository.syncWithDevice()
                syncStatus.value = SyncStatus(finishedOnce = true, failed = false)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                syncStatus.value = SyncStatus(finishedOnce = true, failed = true)
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY],
                ) as CaptuIAApplication
                HomeViewModel(application.container.captureRepository)
            }
        }
    }
}
