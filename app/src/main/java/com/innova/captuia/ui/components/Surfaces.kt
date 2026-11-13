package com.innova.captuia.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.innova.captuia.R
import com.innova.captuia.ui.theme.Ink
import com.innova.captuia.ui.theme.Line
import com.innova.captuia.ui.theme.Spacing
import com.innova.captuia.ui.theme.TealLight
import com.innova.captuia.ui.theme.White

/** Tarjeta blanca con borde fino y radio de 18, como las tarjetas de la landing. */
@Composable
fun CaptuCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = White,
        border = BorderStroke(1.dp, Line),
    ) {
        Column(modifier = Modifier.padding(Spacing.x3), content = content)
    }
}

/**
 * Aviso informativo. Lleva siempre un ícono además del color, para no
 * comunicar solo con color (lineamiento de accesibilidad de la infografía).
 */
@Composable
fun Notice(
    text: String,
    modifier: Modifier = Modifier,
    @DrawableRes iconRes: Int = R.drawable.ic_info,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = TealLight.copy(alpha = 0.5f),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = Spacing.x2, vertical = 12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = Ink,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = Ink,
            )
        }
    }
}
