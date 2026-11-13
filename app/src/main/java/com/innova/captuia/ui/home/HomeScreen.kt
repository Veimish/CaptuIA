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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
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
import com.innova.captuia.ui.components.BrandHeader
import com.innova.captuia.ui.components.CaptuCard
import com.innova.captuia.ui.components.Notice
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
import com.innova.captuia.ui.theme.White

/**
 * Inicio: galería de todas las capturas del dispositivo, agrupadas por día.
 * El índice se actualiza cada vez que la app vuelve a primer plano.
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

        LazyVerticalGrid(
            columns = GridCells.Fixed(GRID_COLUMNS),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(
                start = Spacing.x2,
                end = Spacing.x2,
                top = Spacing.x3,
                bottom = Spacing.x3 + navigationBarPadding,
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
                            onClick = { context.openAppSettings() },
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
