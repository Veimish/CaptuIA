package com.innova.captuia.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.innova.captuia.R
import com.innova.captuia.ui.theme.Ink
import com.innova.captuia.ui.theme.InkSoft
import com.innova.captuia.ui.theme.Line
import com.innova.captuia.ui.theme.Paper
import com.innova.captuia.ui.theme.Spacing
import com.innova.captuia.ui.theme.TealDark
import com.innova.captuia.ui.theme.TealDeep
import com.innova.captuia.ui.theme.White

/** Identificador del campo de texto de los diálogos, para las pruebas de interfaz. */
const val DIALOG_FIELD_TAG = "campo-dialogo"

/** Diálogo con el estilo de las tarjetas de CaptuIA. */
@Composable
fun CaptuDialog(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = White,
            shadowElevation = 12.dp,
        ) {
            Column(
                modifier = Modifier.padding(Spacing.x3),
                verticalArrangement = Arrangement.spacedBy(Spacing.x2),
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = Ink,
                )
                content()
            }
        }
    }
}

/** Botones de un diálogo: la acción principal (amarilla) y cancelar. */
@Composable
fun DialogActions(
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    confirmEnabled: Boolean = true,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        PrimaryButton(
            text = confirmText,
            onClick = onConfirm,
            modifier = Modifier.fillMaxWidth(),
            enabled = confirmEnabled,
        )
        SecondaryButton(
            text = dismissText,
            onClick = onDismiss,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

/** Diálogo de confirmación para acciones que no se pueden deshacer. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmText: String,
    dismissText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    CaptuDialog(title = title, onDismiss = onDismiss) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft,
        )
        DialogActions(
            confirmText = confirmText,
            dismissText = dismissText,
            onConfirm = onConfirm,
            onDismiss = onDismiss,
        )
    }
}

/**
 * Diálogo para escribir un nombre. [errorMessage] se muestra bajo el campo con
 * un ícono (no solo con color) cuando el nombre no sirve.
 */
@Composable
fun TextInputDialog(
    title: String,
    label: String,
    initialValue: String,
    maxLength: Int,
    errorMessage: String?,
    confirmText: String,
    dismissText: String,
    onValueChange: () -> Unit,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by rememberSaveable(stateSaver = TextFieldValue.Saver) {
        mutableStateOf(TextFieldValue(initialValue, selection = TextRange(initialValue.length)))
    }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    CaptuDialog(title = title, onDismiss = onDismiss) {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.x1)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = Ink,
            )
            val fieldShape = MaterialTheme.shapes.small
            BasicTextField(
                value = value,
                onValueChange = { changed ->
                    if (changed.text.length <= maxLength) {
                        if (changed.text != value.text) onValueChange()
                        value = changed
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .testTag(DIALOG_FIELD_TAG),
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = Ink),
                cursorBrush = SolidColor(TealDark),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Sentences,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(onDone = { onConfirm(value.text) }),
                decorationBox = { innerTextField ->
                    Row(
                        modifier = Modifier
                            .clip(fieldShape)
                            .background(Paper)
                            .border(
                                BorderStroke(1.5.dp, if (errorMessage != null) TealDeep else Line),
                                fieldShape,
                            )
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                    ) {
                        innerTextField()
                    }
                },
            )
            if (errorMessage != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.ic_info),
                        contentDescription = null,
                        tint = Ink,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(Spacing.x1))
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = Ink,
                    )
                }
            }
        }
        DialogActions(
            confirmText = confirmText,
            dismissText = dismissText,
            onConfirm = { onConfirm(value.text) },
            onDismiss = onDismiss,
        )
    }
}
