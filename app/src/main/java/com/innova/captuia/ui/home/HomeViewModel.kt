package com.innova.captuia.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.innova.captuia.CaptuIAApplication
import com.innova.captuia.data.media.ScreenshotSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Estado de la pantalla de inicio.
 * [total] es null hasta que termina la primera lectura de la galería.
 */
data class HomeUiState(
    val isLoading: Boolean = true,
    val total: Int? = null,
    val latestMillis: Long? = null,
    val hasError: Boolean = false,
)

class HomeViewModel(
    private val screenshotSource: ScreenshotSource,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private var loadJob: Job? = null

    /** Vuelve a leer las capturas del dispositivo. */
    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(isLoading = true, hasError = false) }
            try {
                val screenshots = screenshotSource.loadAll()
                _state.value = HomeUiState(
                    isLoading = false,
                    total = screenshots.size,
                    latestMillis = screenshots.firstOrNull()?.dateAddedMillis,
                    hasError = false,
                )
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                _state.update { it.copy(isLoading = false, hasError = true) }
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY],
                ) as CaptuIAApplication
                HomeViewModel(application.container.screenshotSource)
            }
        }
    }
}
