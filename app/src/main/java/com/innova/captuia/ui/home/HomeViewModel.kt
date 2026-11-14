package com.innova.captuia.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.innova.captuia.CaptuIAApplication
import com.innova.captuia.domain.folder.FolderNameCheck
import com.innova.captuia.domain.model.Capture
import com.innova.captuia.domain.model.CaptureFilter
import com.innova.captuia.domain.model.FolderKind
import com.innova.captuia.domain.model.FolderSummary
import com.innova.captuia.domain.model.TextProgress
import com.innova.captuia.domain.repository.CaptureRepository
import com.innova.captuia.domain.repository.FolderRepository
import com.innova.captuia.domain.search.Snippet
import com.innova.captuia.domain.search.SnippetBuilder
import com.innova.captuia.domain.search.TextNormalizer
import com.innova.captuia.domain.usecase.SyncCoordinator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Una captura encontrada por el buscador, con el fragmento de texto que coincide. */
data class SearchResult(
    val capture: Capture,
    val snippet: Snippet,
)

/** Resultados de una búsqueda ya resuelta para [query]. */
data class SearchOutcome(
    val query: String,
    val results: List<SearchResult>,
)

data class HomeUiState(
    /** True solo mientras se hace la primera lectura de la galería. */
    val isLoading: Boolean = true,
    val total: Int = 0,
    /** Carpetas del sistema y del usuario. */
    val folders: List<FolderSummary> = emptyList(),
    /** Carpetas de las materias del horario de clases. */
    val subjects: List<FolderSummary> = emptyList(),
    /** Capturas que aún no tienen carpeta (sin leer o que no se pudieron leer). */
    val unclassified: Int = 0,
    val recent: List<Capture> = emptyList(),
    val hasError: Boolean = false,
    val textProgress: TextProgress = TextProgress(),
    /** Null cuando no hay búsqueda activa o aún no termina. */
    val search: SearchOutcome? = null,
)

class HomeViewModel(
    private val captures: CaptureRepository,
    private val folders: FolderRepository,
    private val syncCoordinator: SyncCoordinator,
) : ViewModel() {

    /** Texto del buscador. Es estado de Compose para que escribir sea inmediato. */
    var query by mutableStateOf("")
        private set

    /** Motivo por el que no se pudo crear la carpeta; null si no hay error que mostrar. */
    var newFolderError by mutableStateOf<FolderNameCheck?>(null)
        private set

    private val queryFlow = MutableStateFlow("")

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private val searchOutcome: Flow<SearchOutcome?> =
        queryFlow
            .debounce(SEARCH_DEBOUNCE_MILLIS)
            .map { it.trim() }
            .distinctUntilChanged()
            .flatMapLatest { text ->
                val terms = TextNormalizer.terms(text)
                if (terms.isEmpty()) {
                    flowOf(null)
                } else {
                    captures.search(text).map { found ->
                        SearchOutcome(
                            query = text,
                            results = found.map { capture ->
                                SearchResult(
                                    capture = capture,
                                    snippet = SnippetBuilder.build(capture.text.orEmpty(), terms),
                                )
                            },
                        )
                    }
                }
            }

    private val library: Flow<Pair<List<Capture>, List<FolderSummary>>> =
        combine(captures.observe(CaptureFilter.All), folders.observeSummaries()) { all, summaries ->
            all to summaries
        }

    val state: StateFlow<HomeUiState> =
        combine(
            library,
            syncCoordinator.status,
            captures.observeTextProgress(),
            searchOutcome,
        ) { (all, summaries), sync, progress, search ->
            HomeUiState(
                isLoading = !sync.finishedOnce && all.isEmpty(),
                total = all.size,
                folders = summaries.filter { it.folder.kind != FolderKind.SUBJECT },
                subjects = summaries.filter { it.folder.kind == FolderKind.SUBJECT },
                unclassified = all.count { it.folderId == null },
                recent = all.take(RECENT_COUNT),
                hasError = sync.failed,
                textProgress = progress,
                search = search,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onQueryChange(text: String) {
        query = text
        queryFlow.value = text
    }

    /** Actualiza el índice con las capturas del dispositivo. */
    fun syncWithDevice() = syncCoordinator.requestSync()

    /** Intenta crear una carpeta; [onCreated] se llama solo si se creó. */
    fun createFolder(name: String, onCreated: () -> Unit) {
        viewModelScope.launch {
            val check = folders.create(name)
            if (check is FolderNameCheck.Valid) {
                newFolderError = null
                onCreated()
            } else {
                newFolderError = check
            }
        }
    }

    fun clearNewFolderError() {
        newFolderError = null
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MILLIS = 250L
        private const val RECENT_COUNT = 6

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val container = (
                    checkNotNull(this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                        as CaptuIAApplication
                    ).container
                HomeViewModel(
                    captures = container.captureRepository,
                    folders = container.folderRepository,
                    syncCoordinator = container.syncCoordinator,
                )
            }
        }
    }
}
