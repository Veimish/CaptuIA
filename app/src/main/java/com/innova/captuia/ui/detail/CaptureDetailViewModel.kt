package com.innova.captuia.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.innova.captuia.CaptuIAApplication
import com.innova.captuia.domain.classify.CaptureClassifier
import com.innova.captuia.domain.model.Capture
import com.innova.captuia.domain.model.Folder
import com.innova.captuia.domain.model.FolderSource
import com.innova.captuia.domain.model.TextStatus
import com.innova.captuia.domain.repository.CaptureRepository
import com.innova.captuia.domain.repository.FolderRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CaptureDetailUiState {
    data object Loading : CaptureDetailUiState

    /** La captura ya no existe (el usuario la borró de la galería). */
    data object Missing : CaptureDetailUiState

    data class Ready(
        val capture: Capture,
        /** Carpeta actual de la captura; null si aún no tiene. */
        val folder: Folder?,
        /** Todas las carpetas, para poder moverla. */
        val folders: List<Folder>,
        /**
         * Palabras del texto por las que CaptuIA eligió la carpeta. Null si la
         * carpeta la eligió el usuario o la captura aún no está clasificada.
         */
        val autoKeywords: List<String>?,
    ) : CaptureDetailUiState
}

class CaptureDetailViewModel(
    savedStateHandle: SavedStateHandle,
    private val captures: CaptureRepository,
    folders: FolderRepository,
) : ViewModel() {

    private val captureId: Long = checkNotNull(savedStateHandle.get<Long>(ARG_CAPTURE_ID))

    val state: StateFlow<CaptureDetailUiState> =
        combine(captures.observeById(captureId), folders.observeSummaries()) { capture, summaries ->
            if (capture == null) {
                CaptureDetailUiState.Missing
            } else {
                val allFolders = summaries.map { it.folder }
                CaptureDetailUiState.Ready(
                    capture = capture,
                    folder = allFolders.firstOrNull { it.id == capture.folderId },
                    folders = allFolders,
                    autoKeywords = if (
                        capture.folderSource == FolderSource.AUTO && capture.textStatus == TextStatus.READY
                    ) {
                        CaptureClassifier.classify(capture.text).matchedKeywords
                    } else {
                        null
                    },
                )
            }
        }.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            CaptureDetailUiState.Loading,
        )

    /** Mueve la captura a la carpeta elegida por el usuario. */
    fun moveToFolder(folderId: Long) {
        viewModelScope.launch { captures.moveToFolder(captureId, folderId) }
    }

    companion object {
        const val ARG_CAPTURE_ID = "captureId"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (
                    checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                        as CaptuIAApplication
                    ).container
                CaptureDetailViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    captures = container.captureRepository,
                    folders = container.folderRepository,
                )
            }
        }
    }
}
