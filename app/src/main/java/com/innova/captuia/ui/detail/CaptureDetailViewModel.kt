package com.innova.captuia.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.innova.captuia.CaptuIAApplication
import com.innova.captuia.domain.model.Capture
import com.innova.captuia.domain.repository.CaptureRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

sealed interface CaptureDetailUiState {
    data object Loading : CaptureDetailUiState

    /** La captura ya no existe (el usuario la borró de la galería). */
    data object Missing : CaptureDetailUiState

    data class Ready(val capture: Capture) : CaptureDetailUiState
}

class CaptureDetailViewModel(
    savedStateHandle: SavedStateHandle,
    repository: CaptureRepository,
) : ViewModel() {

    private val captureId: Long = checkNotNull(savedStateHandle.get<Long>(ARG_CAPTURE_ID))

    val state: StateFlow<CaptureDetailUiState> =
        repository.observeById(captureId)
            .map { capture ->
                if (capture == null) {
                    CaptureDetailUiState.Missing
                } else {
                    CaptureDetailUiState.Ready(capture)
                }
            }
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                CaptureDetailUiState.Loading,
            )

    companion object {
        const val ARG_CAPTURE_ID = "captureId"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY],
                ) as CaptuIAApplication
                CaptureDetailViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    repository = application.container.captureRepository,
                )
            }
        }
    }
}
