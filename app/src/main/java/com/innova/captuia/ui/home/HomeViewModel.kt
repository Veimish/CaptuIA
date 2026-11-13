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
import com.innova.captuia.domain.model.Capture
import com.innova.captuia.domain.model.TextProgress
import com.innova.captuia.domain.repository.CaptureRepository
import com.innova.captuia.domain.search.Snippet
import com.innova.captuia.domain.search.SnippetBuilder
import com.innova.captuia.domain.search.TextNormalizer
import com.innova.captuia.domain.usecase.SyncCapturesUseCase
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
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
import java.time.LocalDate
import java.time.ZoneId

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
    val sections: List<DaySection> = emptyList(),
    val hasError: Boolean = false,
    val textProgress: TextProgress = TextProgress(),
    /** Null cuando no hay búsqueda activa o aún no termina. */
    val search: SearchOutcome? = null,
)

class HomeViewModel(
    private val repository: CaptureRepository,
    private val syncCaptures: SyncCapturesUseCase,
) : ViewModel() {

    private data class SyncStatus(val finishedOnce: Boolean = false, val failed: Boolean = false)

    private val syncStatus = MutableStateFlow(SyncStatus())
    private var syncJob: Job? = null

    /** Texto del buscador. Es estado de Compose para que escribir sea inmediato. */
    var query by mutableStateOf("")
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
                    repository.search(text).map { captures ->
                        SearchOutcome(
                            query = text,
                            results = captures.map { capture ->
                                SearchResult(
                                    capture = capture,
                                    snippet = SnippetBuilder.build(capture.text.orEmpty(), terms),
                                )
                            },
                        )
                    }
                }
            }

    val state: StateFlow<HomeUiState> =
        combine(
            repository.observeAll(),
            syncStatus,
            repository.observeTextProgress(),
            searchOutcome,
        ) { captures, sync, progress, search ->
            HomeUiState(
                isLoading = !sync.finishedOnce && captures.isEmpty(),
                total = captures.size,
                sections = groupByDay(captures, LocalDate.now(), ZoneId.systemDefault()),
                hasError = sync.failed,
                textProgress = progress,
                search = search,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun onQueryChange(text: String) {
        query = text
        queryFlow.value = text
    }

    /** Actualiza el índice con las capturas del dispositivo y programa la lectura de texto. */
    fun syncWithDevice() {
        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            try {
                syncCaptures()
                syncStatus.value = SyncStatus(finishedOnce = true, failed = false)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                syncStatus.value = SyncStatus(finishedOnce = true, failed = true)
            }
        }
    }

    companion object {
        private const val SEARCH_DEBOUNCE_MILLIS = 250L

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val application = checkNotNull(
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY],
                ) as CaptuIAApplication
                HomeViewModel(
                    repository = application.container.captureRepository,
                    syncCaptures = application.container.syncCaptures,
                )
            }
        }
    }
}
