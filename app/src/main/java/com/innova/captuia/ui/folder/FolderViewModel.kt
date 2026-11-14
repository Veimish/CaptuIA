package com.innova.captuia.ui.folder

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.innova.captuia.CaptuIAApplication
import com.innova.captuia.domain.folder.FolderNameCheck
import com.innova.captuia.domain.model.CaptureFilter
import com.innova.captuia.domain.model.Folder
import com.innova.captuia.domain.repository.CaptureRepository
import com.innova.captuia.domain.repository.FolderRepository
import com.innova.captuia.domain.usecase.ClassifyCapturesUseCase
import com.innova.captuia.domain.usecase.SyncCoordinator
import com.innova.captuia.ui.gallery.DaySection
import com.innova.captuia.ui.gallery.groupByDay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class FolderUiState(
    val isLoading: Boolean = true,
    val filter: CaptureFilter,
    /** La carpeta mostrada; null para "Todas" y "Sin clasificar". */
    val folder: Folder? = null,
    /** True si la carpeta dejó de existir (se eliminó). */
    val isGone: Boolean = false,
    val total: Int = 0,
    val sections: List<DaySection> = emptyList(),
)

class FolderViewModel(
    savedStateHandle: SavedStateHandle,
    captures: CaptureRepository,
    private val folders: FolderRepository,
    private val classifyCaptures: ClassifyCapturesUseCase,
    private val syncCoordinator: SyncCoordinator,
) : ViewModel() {

    val filter: CaptureFilter = FolderRoute.parse(checkNotNull(savedStateHandle.get<String>(ARG_FILTER)))

    /** Motivo por el que no se pudo renombrar; null si no hay error que mostrar. */
    var renameError by mutableStateOf<FolderNameCheck?>(null)
        private set

    private val folderFlow: Flow<FolderLookup> = when (filter) {
        is CaptureFilter.InFolder -> folders.observeById(filter.folderId).map { FolderLookup(it, it == null) }
        else -> flowOf(FolderLookup(null, false))
    }

    val state: StateFlow<FolderUiState> =
        combine(captures.observe(filter), folderFlow) { list, lookup ->
            FolderUiState(
                isLoading = false,
                filter = filter,
                folder = lookup.folder,
                isGone = lookup.gone,
                total = list.size,
                sections = groupByDay(list, LocalDate.now(), ZoneId.systemDefault()),
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), FolderUiState(filter = filter))

    fun syncWithDevice() = syncCoordinator.requestSync()

    /** Intenta renombrar la carpeta; [onRenamed] se llama solo si se pudo. */
    fun rename(name: String, onRenamed: () -> Unit) {
        val folderId = (filter as? CaptureFilter.InFolder)?.folderId ?: return
        viewModelScope.launch {
            val check = folders.rename(folderId, name)
            if (check is FolderNameCheck.Valid) {
                renameError = null
                onRenamed()
            } else {
                renameError = check
            }
        }
    }

    fun clearRenameError() {
        renameError = null
    }

    /** Elimina la carpeta. Sus capturas vuelven a clasificarse automáticamente. */
    fun delete() {
        val folderId = (filter as? CaptureFilter.InFolder)?.folderId ?: return
        viewModelScope.launch {
            folders.delete(folderId)
            classifyCaptures()
        }
    }

    private data class FolderLookup(val folder: Folder?, val gone: Boolean)

    companion object {
        const val ARG_FILTER = "filter"

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (
                    checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                        as CaptuIAApplication
                    ).container
                FolderViewModel(
                    savedStateHandle = createSavedStateHandle(),
                    captures = container.captureRepository,
                    folders = container.folderRepository,
                    classifyCaptures = container.classifyCaptures,
                    syncCoordinator = container.syncCoordinator,
                )
            }
        }
    }
}

/** Traduce un [CaptureFilter] al texto que viaja en la ruta de navegación y viceversa. */
object FolderRoute {
    private const val ALL = "todas"
    private const val UNCLASSIFIED = "sin-clasificar"

    fun encode(filter: CaptureFilter): String = when (filter) {
        CaptureFilter.All -> ALL
        CaptureFilter.Unclassified -> UNCLASSIFIED
        is CaptureFilter.InFolder -> filter.folderId.toString()
    }

    fun parse(value: String): CaptureFilter = when (value) {
        ALL -> CaptureFilter.All
        UNCLASSIFIED -> CaptureFilter.Unclassified
        else -> CaptureFilter.InFolder(value.toLong())
    }
}
