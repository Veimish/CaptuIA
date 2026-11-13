package com.innova.captuia.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.innova.captuia.R
import com.innova.captuia.ui.theme.Spacing
import com.innova.captuia.ui.theme.TealDark
import com.innova.captuia.ui.theme.TealDeep
import com.innova.captuia.ui.theme.TealLight
import com.innova.captuia.ui.theme.White

/** Marca gráfica de la landing: marco de captura con lente. */
@Composable
fun BrandMark(
    modifier: Modifier = Modifier,
    onDark: Boolean = true,
    size: Dp = 36.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(10.dp))
            .background(if (onDark) White.copy(alpha = 0.16f) else TealDark),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_brand_mark),
            contentDescription = stringResource(R.string.brand_mark_description),
            tint = White,
            modifier = Modifier.size(size * 0.56f),
        )
    }
}

/** Nombre de la marca como en la infografía: "Captu" + "IA" resaltado. */
@Composable
fun BrandWordmark(modifier: Modifier = Modifier) {
    val prefix = stringResource(R.string.brand_prefix)
    val suffix = stringResource(R.string.brand_suffix)
    Text(
        text = buildAnnotatedString {
            withStyle(SpanStyle(color = White)) { append(prefix) }
            withStyle(SpanStyle(color = TealLight)) { append(suffix) }
        },
        style = MaterialTheme.typography.titleLarge,
        modifier = modifier,
    )
}

/**
 * Cabecera de marca común a todas las pantallas: degradado del hero de la
 * landing, logo y un título grande en blanco.
 */
@Composable
fun BrandHeader(
    title: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
            .background(Brush.verticalGradient(listOf(TealDark, TealDeep)))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = Spacing.x3, vertical = Spacing.x3),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BrandMark(onDark = true)
            Spacer(Modifier.width(10.dp))
            BrandWordmark()
        }
        Spacer(Modifier.height(Spacing.x3))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = White,
        )
    }
}

/**
 * Cabecera compacta para pantallas secundarias: botón de volver y título.
 */
@Composable
fun CompactHeader(
    title: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Brush.verticalGradient(listOf(TealDark, TealDeep)))
            .windowInsetsPadding(WindowInsets.statusBars)
            .padding(horizontal = Spacing.x1, vertical = Spacing.x1),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(R.drawable.ic_arrow_back),
                contentDescription = stringResource(R.string.action_back),
                tint = White,
            )
        }
        Spacer(Modifier.width(Spacing.half))
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = White,
        )
    }
}
