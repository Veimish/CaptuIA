package com.innova.captuia.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.innova.captuia.R
import com.innova.captuia.data.media.MediaAccess
import com.innova.captuia.data.media.MediaPermissions
import com.innova.captuia.domain.folder.FolderNameCheck
import com.innova.captuia.domain.folder.FolderNameValidator
import com.innova.captuia.domain.model.CaptureFilter
import com.innova.captuia.domain.model.TextProgress
import com.innova.captuia.ui.components.BrandHeader
import com.innova.captuia.ui.components.CaptuCard
import com.innova.captuia.ui.components.FolderIcon
import com.innova.captuia.ui.components.FolderVisual
import com.innova.captuia.ui.components.Notice
import com.innova.captuia.ui.components.SearchField
import com.innova.captuia.ui.components.SecondaryButton
import com.innova.captuia.ui.components.TextInputDialog
import com.innova.captuia.ui.components.openAppSettings
import com.innova.captuia.ui.format.Formats
import com.innova.captuia.ui.gallery.CaptureThumbnail
import com.innova.captuia.ui.gallery.GRID_COLUMNS
import com.innova.captuia.ui.gallery.THUMBNAIL_ASPECT_RATIO
import com.innova.captuia.ui.gallery.captureCountText
import com.innova.captuia.ui.theme.Ink
import com.innova.captuia.ui.theme.InkSoft
import com.innova.captuia.ui.theme.Line
import com.innova.captuia.ui.theme.Paper
import com.innova.captuia.ui.theme.Spacing
import com.innova.captuia.ui.theme.TealDark
import com.innova.captuia.ui.theme.TealDeep
import com.innova.captuia.ui.theme.TealLight
import com.innova.captuia.ui.theme.White

/** Identificadores de las filas de carpeta, para las pruebas de interfaz. */
const val FOLDER_ALL_TAG = "carpeta-todas"
const val FOLDER_UNCLASSIFIED_TAG = "carpeta-sin-clasificar"
fun folderRowTag(name: String) = "carpeta-$name"

/**
 * Inicio: buscador por contenido, carpetas con su número de capturas (como en
 * la maqueta de la landing) y las capturas más recientes.
 */
@Composable
fun HomeScreen(
    onAccessLost: () -> Unit,
    onOpenCapture: (Long) -> Unit,
    onOpenFolder: (CaptureFilter) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var access by remember { mutableStateOf(MediaPermissions.current(context)) }
    var showNewFolder by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        access = MediaPermissions.current(context)
        if (access == MediaAccess.DENIED) onAccessLost() else viewModel.syncWithDevice()
    }

    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper),
    ) {
        BrandHeader(title = stringResource(R.string.home_title))

        val query = viewModel.query
        val isSearching = query.isNotBlank()

        Column(
            modifier = Modifier.padding(
                start = Spacing.x2,
                end = Spacing.x2,
                top = Spacing.x2,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SearchField(
                value = query,
                onValueChange = viewModel::onQueryChange,
                placeholder = stringResource(R.string.search_placeholder),
            )
            if (state.textProgress.isWorking) {
                ReadingProgress(state.textProgress)
            }
        }

        if (isSearching) {
            SearchResults(
                outcome = state.search,
                bottomPadding = navigationBarPadding,
                onOpenCapture = onOpenCapture,
                modifier = Modifier.weight(1f),
            )
        } else {
            Library(
                state = state,
                access = access,
                bottomPadding = navigationBarPadding,
                onOpenSettings = { context.openAppSettings() },
                onOpenCapture = onOpenCapture,
                onOpenFolder = onOpenFolder,
                onNewFolder = {
                    viewModel.clearNewFolderError()
                    showNewFolder = true
                },
                modifier = Modifier.weight(1f),
            )
        }
    }

    if (showNewFolder) {
        TextInputDialog(
            title = stringResource(R.string.folder_new_title),
            label = stringResource(R.string.folder_name_label),
            initialValue = "",
            maxLength = FolderNameValidator.MAX_LENGTH,
            errorMessage = folderNameError(viewModel.newFolderError),
            confirmText = stringResource(R.string.action_create),
            dismissText = stringResource(R.string.action_cancel),
            onValueChange = viewModel::clearNewFolderError,
            onConfirm = { name -> viewModel.createFolder(name) { showNewFolder = false } },
            onDismiss = { showNewFolder = false },
        )
    }
}

/** Mensaje para mostrar bajo el campo cuando el nombre de carpeta no sirve. */
@Composable
fun folderNameError(check: FolderNameCheck?): String? = when (check) {
    FolderNameCheck.Empty -> stringResource(R.string.folder_error_empty)
    FolderNameCheck.TooLong -> stringResource(R.string.folder_error_too_long)
    FolderNameCheck.Duplicate -> stringResource(R.string.folder_error_duplicate)
    is FolderNameCheck.Valid, null -> null
}

@Composable
private fun ReadingProgress(progress: TextProgress) {
    Notice(
        text = stringResource(
            R.string.home_reading_progress,
            Formats.count(progress.done),
            Formats.count(progress.total),
        ),
    )
}

@Composable
private fun Library(
    state: HomeUiState,
    access: MediaAccess,
    bottomPadding: Dp,
    onOpenSettings: () -> Unit,
    onOpenCapture: (Long) -> Unit,
    onOpenFolder: (CaptureFilter) -> Unit,
    onNewFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = Spacing.x2,
            end = Spacing.x2,
            top = Spacing.x2,
            bottom = Spacing.x3 + bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(Spacing.x1),
    ) {
        if (access == MediaAccess.PARTIAL) {
            item(key = "partial") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Notice(text = stringResource(R.string.home_partial))
                    SecondaryButton(
                        text = stringResource(R.string.action_open_settings),
                        onClick = onOpenSettings,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        if (state.hasError) {
            item(key = "error") { Notice(text = stringResource(R.string.home_error)) }
        }

        when {
            state.isLoading -> item(key = "loading") { LoadingCard() }
            state.total == 0 -> item(key = "empty") { EmptyCard() }
            else -> libraryContent(state, onOpenCapture, onOpenFolder, onNewFolder)
        }
    }
}

private fun LazyListScope.libraryContent(
    state: HomeUiState,
    onOpenCapture: (Long) -> Unit,
    onOpenFolder: (CaptureFilter) -> Unit,
    onNewFolder: () -> Unit,
) {
    item(key = "folders-title") { SectionTitle(stringResource(R.string.home_folders_title)) }

    item(key = "folder-all") {
        FolderRow(
            name = stringResource(R.string.folder_all),
            count = state.total,
            visual = FolderVisual.All,
            onClick = { onOpenFolder(CaptureFilter.All) },
            modifier = Modifier.testTag(FOLDER_ALL_TAG),
        )
    }
    listItems(items = state.folders, key = { "folder-${it.folder.id}" }) { summary ->
        FolderRow(
            name = summary.folder.name,
            count = summary.captureCount,
            visual = FolderVisual.of(summary.folder),
            onClick = { onOpenFolder(CaptureFilter.InFolder(summary.folder.id)) },
            modifier = Modifier.testTag(folderRowTag(summary.folder.name)),
        )
    }
    if (state.unclassified > 0) {
        item(key = "folder-unclassified") {
            FolderRow(
                name = stringResource(R.string.folder_unclassified),
                count = state.unclassified,
                visual = FolderVisual.Unclassified,
                onClick = { onOpenFolder(CaptureFilter.Unclassified) },
                modifier = Modifier.testTag(FOLDER_UNCLASSIFIED_TAG),
            )
        }
    }
    item(key = "folder-new") {
        SecondaryButton(
            text = stringResource(R.string.action_new_folder),
            onClick = onNewFolder,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = Spacing.half),
        )
    }

    item(key = "recent-title") {
        SectionTitle(
            text = stringResource(R.string.home_recent_title),
            modifier = Modifier.padding(top = Spacing.x2),
        )
    }
    val rows = state.recent.chunked(GRID_COLUMNS)
    listItems(items = rows, key = { row -> "recent-${row.first().id}" }) { row ->
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.x1)) {
            row.forEach { capture ->
                CaptureThumbnail(
                    capture = capture,
                    onClick = { onOpenCapture(capture.id) },
                    modifier = Modifier.weight(1f),
                )
            }
            // Completa la fila para que las miniaturas conserven su ancho.
            repeat(GRID_COLUMNS - row.size) {
                Box(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = TealDeep,
        modifier = modifier.padding(bottom = Spacing.half),
    )
}

/** Fila de carpeta como en la maqueta de la landing: ícono, nombre y número de capturas. */
@Composable
private fun FolderRow(
    name: String,
    count: Int,
    visual: FolderVisual,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = MaterialTheme.shapes.medium
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClickLabel = stringResource(R.string.action_open_folder), onClick = onClick),
        shape = shape,
        color = White,
        border = BorderStroke(1.dp, Line),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            FolderIcon(visual = visual)
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = captureCountText(count),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
        }
    }
}

@Composable
private fun SearchResults(
    outcome: SearchOutcome?,
    bottomPadding: Dp,
    onOpenCapture: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = Spacing.x2,
            end = Spacing.x2,
            top = Spacing.x2,
            bottom = Spacing.x3 + bottomPadding,
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when {
            // La búsqueda aún no termina: no se muestra nada para evitar parpadeos.
            outcome == null -> Unit

            outcome.results.isEmpty() -> item(key = "no-results") {
                CaptuCard {
                    Text(
                        text = stringResource(R.string.search_empty_title, outcome.query),
                        style = MaterialTheme.typography.titleLarge,
                        color = Ink,
                    )
                    Spacer(Modifier.height(Spacing.x1))
                    Text(
                        text = stringResource(R.string.search_empty_text),
                        style = MaterialTheme.typography.bodyMedium,
                        color = InkSoft,
                    )
                }
            }

            else -> {
                item(key = "results-count") {
                    Text(
                        text = if (outcome.results.size == 1) {
                            stringResource(R.string.search_count_one)
                        } else {
                            stringResource(
                                R.string.search_count_many,
                                Formats.count(outcome.results.size),
                            )
                        },
                        style = MaterialTheme.typography.labelLarge,
                        color = TealDeep,
                    )
                }
                listItems(items = outcome.results, key = { it.capture.id }) { result ->
                    ResultRow(result = result, onClick = { onOpenCapture(result.capture.id) })
                }
            }
        }
    }
}

@Composable
private fun ResultRow(result: SearchResult, onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.medium
    val snippet = result.snippet
    val highlighted = buildAnnotatedString {
        append(snippet.text)
        snippet.highlights.forEach { range ->
            addStyle(
                SpanStyle(background = TealLight, fontWeight = FontWeight.SemiBold),
                start = range.first,
                end = range.last + 1,
            )
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClickLabel = stringResource(R.string.action_open_capture), onClick = onClick),
        shape = shape,
        color = White,
        border = BorderStroke(1.dp, Line),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(result.capture.contentUri)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = Alignment.TopCenter,
                modifier = Modifier
                    .width(64.dp)
                    .aspectRatio(THUMBNAIL_ASPECT_RATIO)
                    .clip(MaterialTheme.shapes.small)
                    .background(Paper)
                    .border(BorderStroke(1.dp, Line), MaterialTheme.shapes.small),
            )
            Spacer(Modifier.width(12.dp))
            Column(verticalArrangement = Arrangement.spacedBy(Spacing.half)) {
                Text(
                    text = highlighted,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Ink,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = Formats.dateTime(result.capture.dateAddedMillis),
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSoft,
                )
            }
        }
    }
}

@Composable
private fun LoadingCard() {
    CaptuCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = TealDark,
                strokeWidth = 3.dp,
            )
            Spacer(Modifier.width(Spacing.x2))
            Text(
                text = stringResource(R.string.home_loading),
                style = MaterialTheme.typography.bodyLarge,
                color = Ink,
            )
        }
    }
}

@Composable
private fun EmptyCard() {
    CaptuCard {
        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = Ink,
        )
        Spacer(Modifier.height(Spacing.x1))
        Text(
            text = stringResource(R.string.home_empty_text),
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft,
        )
    }
}
