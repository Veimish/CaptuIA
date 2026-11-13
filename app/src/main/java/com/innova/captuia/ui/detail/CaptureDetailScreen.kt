package com.innova.captuia.ui.detail

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.innova.captuia.R
import com.innova.captuia.domain.model.Capture
import com.innova.captuia.ui.components.CompactHeader
import com.innova.captuia.ui.components.PrimaryButton
import com.innova.captuia.ui.format.Formats
import com.innova.captuia.ui.theme.Ink
import com.innova.captuia.ui.theme.InkSoft
import com.innova.captuia.ui.theme.Line
import com.innova.captuia.ui.theme.Paper
import com.innova.captuia.ui.theme.Spacing
import com.innova.captuia.ui.theme.White

/** Detalle de una captura: imagen completa con zoom, datos del archivo y compartir. */
@Composable
fun CaptureDetailScreen(
    onBack: () -> Unit,
    viewModel: CaptureDetailViewModel = viewModel(factory = CaptureDetailViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Si la captura se borró de la galería mientras estaba abierta, se vuelve atrás.
    if (state is CaptureDetailUiState.Missing) {
        LaunchedEffect(Unit) { onBack() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper),
    ) {
        CompactHeader(
            title = stringResource(R.string.detail_title),
            onBack = onBack,
        )

        val capture = (state as? CaptureDetailUiState.Ready)?.capture
        if (capture != null) {
            ZoomableImage(
                capture = capture,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            )
            InfoPanel(capture = capture)
        }
    }
}

private const val MIN_SCALE = 1f
private const val MAX_SCALE = 5f
private const val DOUBLE_TAP_SCALE = 2.5f

/** Imagen ajustada a la pantalla. Se amplía con dos dedos o con doble toque. */
@Composable
private fun ZoomableImage(capture: Capture, modifier: Modifier = Modifier) {
    var scale by remember { mutableFloatStateOf(MIN_SCALE) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var size by remember { mutableStateOf(IntSize.Zero) }

    fun clamp(candidate: Offset, forScale: Float): Offset {
        val maxX = size.width * (forScale - 1f) / 2f
        val maxY = size.height * (forScale - 1f) / 2f
        return Offset(
            x = candidate.x.coerceIn(-maxX, maxX),
            y = candidate.y.coerceIn(-maxY, maxY),
        )
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(MIN_SCALE, MAX_SCALE)
        scale = newScale
        offset = clamp(offset + panChange, newScale)
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { size = it }
            .pointerInput(Unit) {
                detectTapGestures(
                    onDoubleTap = {
                        if (scale > MIN_SCALE) {
                            scale = MIN_SCALE
                            offset = Offset.Zero
                        } else {
                            scale = DOUBLE_TAP_SCALE
                        }
                    },
                )
            }
            .transformable(state = transformState),
        contentAlignment = Alignment.Center,
    ) {
        // El marco toma la proporción real de la captura para dibujarle borde y sombra:
        // así se distingue del fondo aunque la captura sea de una pantalla clara.
        val imageRatio = if (capture.width > 0 && capture.height > 0) {
            capture.width.toFloat() / capture.height.toFloat()
        } else {
            FALLBACK_RATIO
        }
        val areaRatio = if (size.height > 0) size.width.toFloat() / size.height.toFloat() else 1f
        val frameShape = MaterialTheme.shapes.small

        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(capture.contentUri)
                .crossfade(true)
                .build(),
            contentDescription = stringResource(
                R.string.capture_thumbnail_description,
                Formats.dateTime(capture.dateAddedMillis),
            ),
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .padding(Spacing.x2)
                .aspectRatio(imageRatio, matchHeightConstraintsFirst = imageRatio < areaRatio)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
                .shadow(elevation = 6.dp, shape = frameShape)
                .clip(frameShape)
                .background(White)
                .border(BorderStroke(1.dp, Line), frameShape),
        )
    }
}

/** Proporción típica de una captura vertical, por si el sistema no informa las dimensiones. */
private const val FALLBACK_RATIO = 9f / 19.5f

@Composable
private fun InfoPanel(capture: Capture) {
    val context = LocalContext.current
    val chooserTitle = stringResource(R.string.share_chooser_title)

    Surface(color = White, shadowElevation = 8.dp) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = Spacing.x3, vertical = Spacing.x2),
            verticalArrangement = Arrangement.spacedBy(Spacing.x1),
        ) {
            Text(
                text = capture.displayName,
                style = MaterialTheme.typography.titleMedium,
                color = Ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            InfoRow(
                label = stringResource(R.string.detail_date),
                value = Formats.dateTime(capture.dateAddedMillis),
            )
            InfoRow(
                label = stringResource(R.string.detail_dimensions),
                value = stringResource(
                    R.string.detail_dimensions_value,
                    capture.width,
                    capture.height,
                ),
            )
            InfoRow(
                label = stringResource(R.string.detail_size),
                value = Formats.fileSize(capture.sizeBytes),
            )
            PrimaryButton(
                text = stringResource(R.string.action_share),
                onClick = { context.shareCapture(capture, chooserTitle) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Spacing.x1),
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = Ink,
        )
    }
}

/** Abre el selector del sistema para enviar la imagen a otra app. */
private fun Context.shareCapture(capture: Capture, chooserTitle: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "image/*"
        putExtra(Intent.EXTRA_STREAM, Uri.parse(capture.contentUri))
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    startActivity(Intent.createChooser(send, chooserTitle))
}
