package com.innova.captuia.ui.gallery

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.innova.captuia.R
import com.innova.captuia.domain.model.Capture
import com.innova.captuia.ui.format.Formats
import com.innova.captuia.ui.theme.Line
import com.innova.captuia.ui.theme.Spacing
import com.innova.captuia.ui.theme.TealDeep
import com.innova.captuia.ui.theme.White

const val GRID_COLUMNS = 3

/** Miniaturas verticales: proporción 3:4, recortadas desde la parte superior de la captura. */
const val THUMBNAIL_ASPECT_RATIO = 0.75f

/**
 * Cuadrícula de capturas agrupadas por día. [header] permite poner contenido
 * a todo el ancho antes de las capturas (avisos, resumen, estados vacíos).
 */
@Composable
fun CaptureGrid(
    sections: List<DaySection>,
    onOpenCapture: (Long) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    header: LazyGridScope.() -> Unit = {},
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(GRID_COLUMNS),
        modifier = modifier,
        contentPadding = contentPadding,
        horizontalArrangement = Arrangement.spacedBy(Spacing.x1),
        verticalArrangement = Arrangement.spacedBy(Spacing.x1),
    ) {
        header()
        sections.forEach { section ->
            item(
                key = "day-${section.date}",
                span = { GridItemSpan(maxLineSpan) },
            ) {
                DayHeader(section)
            }
            items(items = section.captures, key = { it.id }) { capture ->
                CaptureThumbnail(capture = capture, onClick = { onOpenCapture(capture.id) })
            }
        }
    }
}

/** Texto "N capturas" con el número en formato local. */
@Composable
fun captureCountText(total: Int): String =
    if (total == 1) {
        stringResource(R.string.count_one)
    } else {
        stringResource(R.string.count_many, Formats.count(total))
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
fun CaptureThumbnail(
    capture: Capture,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(THUMBNAIL_ASPECT_RATIO)
            .clip(shape)
            .background(White)
            .border(BorderStroke(1.dp, Line), shape)
            .clickable(onClickLabel = stringResource(R.string.action_open_capture), onClick = onClick),
    )
}
