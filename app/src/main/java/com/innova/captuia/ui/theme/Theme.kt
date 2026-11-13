package com.innova.captuia.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

/*
 * Tema único y claro. La documentación del proyecto solo define una paleta
 * clara, así que no se usa tema oscuro ni colores dinámicos del sistema.
 */
private val CaptuColorScheme = lightColorScheme(
    primary = TealDark,
    onPrimary = White,
    primaryContainer = TealLight,
    onPrimaryContainer = Ink,
    secondary = Teal,
    onSecondary = Ink,
    secondaryContainer = TealLight,
    onSecondaryContainer = Ink,
    tertiary = Amber,
    onTertiary = Ink,
    tertiaryContainer = Amber,
    onTertiaryContainer = Ink,
    background = Paper,
    onBackground = Ink,
    surface = White,
    onSurface = Ink,
    surfaceVariant = Paper,
    onSurfaceVariant = InkSoft,
    outline = InkSoft,
    outlineVariant = Line,
)

/* Radios de la landing: 10 px en campos, 18 px en tarjetas. */
private val CaptuShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(18.dp),
    extraLarge = RoundedCornerShape(24.dp),
)

/* Sistema de espaciado de 8 px de la infografía (4 como medio paso). */
object Spacing {
    val half = 4.dp
    val x1 = 8.dp
    val x2 = 16.dp
    val x3 = 24.dp
    val x4 = 32.dp
    val x5 = 40.dp
    val x6 = 48.dp
}

@Composable
fun CaptuIATheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = CaptuColorScheme,
        typography = CaptuTypography,
        shapes = CaptuShapes,
        content = content,
    )
}
