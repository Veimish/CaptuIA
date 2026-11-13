package com.innova.captuia.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as listItems
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
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
import com.innova.captuia.domain.model.Capture
import com.innova.captuia.domain.model.TextProgress
import com.innova.captuia.ui.components.BrandHeader
import com.innova.captuia.ui.components.CaptuCard
import com.innova.captuia.ui.components.Notice
import com.innova.captuia.ui.components.SearchField
import com.innova.captuia.ui.components.SecondaryButton
import com.innova.captuia.ui.components.openAppSettings
import com.innova.captuia.ui.format.Formats
import com.innova.captuia.ui.theme.Ink
import com.innova.captuia.ui.theme.InkSoft
import com.innova.captuia.ui.theme.Line
import com.innova.captuia.ui.theme.Paper
import com.innova.captuia.ui.theme.Spacing
import com.innova.captuia.ui.theme.TealDark
import com.innova.captuia.ui.theme.TealDeep
import com.innova.captuia.ui.theme.TealLight
import com.innova.captuia.ui.theme.White

/**
 * Inicio: buscador por contenido y galería de todas las capturas del
 * dispositivo, agrupadas por día. El índice se actualiza cada vez que la app
 * vuelve a primer plano.
 */
@Composable
fun HomeScreen(
    onAccessLost: () -> Unit,
    onOpenCapture: (Long) -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var access by remember { mutableStateOf(MediaPermissions.current(context)) }

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
            Gallery(
                state = state,
                access = access,
                bottomPadding = navigationBarPadding,
                onOpenSettings = { context.openAppSettings() },
                onOpenCapture = onOpenCapture,
                modifier = Modifier.weight(1f),
            )
        }
    }
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
private fun Gallery(
    state: HomeUiState,
    access: MediaAccess,
    bottomPadding: Dp,
    onOpenSettings: () -> Unit,
    onOpenCapture: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        modifier = modifier,
        contentPadding = PaddingValues(
            start = Spacing.x2,
            end = Spacing.x2,
            top = Spacing.x2,
            bottom = Spacing.x3 + bottomPadding,
        ),
        horizontalArrangement = Arrangement.spacedBy(Spacing.x1),
        verticalArrangement = Arrangement.spacedBy(Spacing.x1),
    ) {
        if (access == MediaAccess.PARTIAL) {
            item(key = "partial", span = { GridItemSpan(maxLineSpan) }) {
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
            item(key = "error", span = { GridItemSpan(maxLineSpan) }) {
                Notice(text = stringResource(R.string.home_error))
            }
        }

        when {
            state.isLoading -> item(key = "loading", span = { GridItemSpan(maxLineSpan) }) {
                LoadingCard()
            }

            state.total == 0 -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
                EmptyCard()
            }

            else -> {
                item(key = "summary", span = { GridItemSpan(maxLineSpan) }) {
                    Summary(total = state.total)
                }
                state.sections.forEach { section ->
                    item(
                        key = "day-${section.date}",
                        span = { GridItemSpan(maxLineSpan) },
                    ) {
                        DayHeader(section)
                    }
                    items(items = section.captures, key = { it.id }) { capture ->
                        Thumbnail(capture = capture, onClick = { onOpenCapture(capture.id) })
                    }
                }
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

private const val GRID_COLUMNS = 3

@Composable
private fun Summary(total: Int) {
    Column {
        Text(
            text = if (total == 1) {
                stringResource(R.string.home_count_one)
            } else {
                stringResource(R.string.home_count_many, Formats.count(total))
            },
            style = MaterialTheme.typography.headlineSmall,
            color = Ink,
        )
        Text(
            text = stringResource(R.string.home_count_caption),
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft,
        )
    }
}

@Composable
private fun DayHeader(section: DaySection) {
    Text(
        text = when (section.relativeDay) {
            RelativeDay.TODAY -> stringResource(R.string.day_today)
            RelativeDay.YESTERDAY -> stringResource(R.string.day_yesterday)
            RelativeDay.EARLIER -> Formats.day(section.date)
        },
        style = MaterialTheme.typography.labelLarge,
        color = TealDeep,
        modifier = Modifier.padding(top = Spacing.x2, bottom = Spacing.half),
    )
}

@Composable
private fun Thumbnail(capture: Capture, onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.medium
    val description = stringResource(
        R.string.capture_thumbnail_description,
        Formats.dateTime(capture.dateAddedMillis),
    )
    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(capture.contentUri)
            .crossfade(true)
            .build(),
        contentDescription = description,
        contentScale = ContentScale.Crop,
        alignment = Alignment.TopCenter,
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(THUMBNAIL_ASPECT_RATIO)
            .clip(shape)
            .background(White)
            .border(BorderStroke(1.dp, Line), shape)
            .clickable(onClickLabel = stringResource(R.string.action_open_capture), onClick = onClick),
    )
}

/** Miniaturas verticales: proporción 3:4, recortadas desde la parte superior de la captura. */
private const val THUMBNAIL_ASPECT_RATIO = 0.75f

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
