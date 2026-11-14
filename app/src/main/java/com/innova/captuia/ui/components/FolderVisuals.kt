package com.innova.captuia.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.innova.captuia.R
import com.innova.captuia.domain.model.Folder
import com.innova.captuia.domain.model.FolderKind
import com.innova.captuia.domain.model.SystemFolder
import com.innova.captuia.ui.theme.Amber
import com.innova.captuia.ui.theme.Ink
import com.innova.captuia.ui.theme.Line
import com.innova.captuia.ui.theme.Paper
import com.innova.captuia.ui.theme.Teal
import com.innova.captuia.ui.theme.TealDark
import com.innova.captuia.ui.theme.TealDeep
import com.innova.captuia.ui.theme.TealLight
import com.innova.captuia.ui.theme.White

/** Ícono y colores de una carpeta. Las del sistema usan los de la maqueta de la landing. */
data class FolderVisual(
    @DrawableRes val iconRes: Int,
    val background: Color,
    val iconTint: Color,
    val outlined: Boolean = false,
) {
    companion object {
        val All = FolderVisual(R.drawable.ic_folder_all, TealDeep, White)
        val Unclassified = FolderVisual(R.drawable.ic_info, Paper, Ink, outlined = true)
        val New = FolderVisual(R.drawable.ic_plus, Paper, TealDeep, outlined = true)
        private val Custom = FolderVisual(R.drawable.ic_folder, Paper, TealDeep, outlined = true)

        val Schedule = FolderVisual(R.drawable.ic_calendar, TealDeep, White)
        private val Subject = FolderVisual(R.drawable.ic_subject, Paper, TealDeep, outlined = true)

        fun of(folder: Folder): FolderVisual = when (folder.kind) {
            FolderKind.SUBJECT -> Subject
            FolderKind.CUSTOM -> Custom
            FolderKind.SYSTEM -> when (folder.systemKey) {
                SystemFolder.DOCUMENTS -> FolderVisual(R.drawable.ic_folder, TealDark, White)
                SystemFolder.TASKS -> FolderVisual(R.drawable.ic_folder_tasks, Teal, White)
                SystemFolder.COURSES -> FolderVisual(R.drawable.ic_folder_courses, TealLight, Ink)
                SystemFolder.IMAGES -> FolderVisual(R.drawable.ic_folder_images, Amber, Ink)
                null -> Custom
            }
        }
    }
}

/** Cuadro de color con el ícono de la carpeta (36 dp, radio 9, como en la landing). */
@Composable
fun FolderIcon(
    visual: FolderVisual,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp,
) {
    val shape = RoundedCornerShape(10.dp)
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(visual.background)
            .then(if (visual.outlined) Modifier.border(BorderStroke(1.dp, Line), shape) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(visual.iconRes),
            contentDescription = null,
            tint = visual.iconTint,
            modifier = Modifier.size(size * 0.5f),
        )
    }
}
