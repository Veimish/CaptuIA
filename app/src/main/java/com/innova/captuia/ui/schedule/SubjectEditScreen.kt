package com.innova.captuia.ui.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.innova.captuia.R
import com.innova.captuia.domain.folder.FolderNameValidator
import com.innova.captuia.domain.schedule.SubjectFormResult
import com.innova.captuia.ui.components.CaptuTextField
import com.innova.captuia.ui.components.CompactHeader
import com.innova.captuia.ui.components.ConfirmDialog
import com.innova.captuia.ui.components.FieldError
import com.innova.captuia.ui.components.PrimaryButton
import com.innova.captuia.ui.components.SecondaryButton
import com.innova.captuia.ui.theme.Ink
import com.innova.captuia.ui.theme.InkSoft
import com.innova.captuia.ui.theme.Line
import com.innova.captuia.ui.theme.Paper
import com.innova.captuia.ui.theme.Spacing
import com.innova.captuia.ui.theme.TealDeep
import com.innova.captuia.ui.theme.White
import java.time.DayOfWeek

/** Identificadores de los campos del formulario, para las pruebas de interfaz. */
const val SUBJECT_NAME_TAG = "campo-materia"
fun sessionDayTag(row: Int, day: DayOfWeek) = "fila-$row-dia-${day.value}"
fun sessionStartTag(row: Int) = "fila-$row-inicio"
fun sessionEndTag(row: Int) = "fila-$row-fin"
fun sessionRemoveTag(row: Int) = "fila-$row-quitar"

/** Longitud máxima de una hora escrita: "23:59". */
private const val TIME_MAX_LENGTH = 5

/** Formulario para registrar o editar una materia y sus horas de clase. */
@Composable
fun SubjectEditScreen(
    onClose: () -> Unit,
    viewModel: SubjectEditViewModel = viewModel(factory = SubjectEditViewModel.Factory),
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    val error = viewModel.error

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper)
            .imePadding(),
    ) {
        CompactHeader(
            title = stringResource(
                if (viewModel.isNew) R.string.subject_new_title else R.string.subject_edit_title,
            ),
            onBack = onClose,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(Spacing.x2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            FormCard {
                FieldLabel(stringResource(R.string.subject_name_label))
                CaptuTextField(
                    value = viewModel.name,
                    onValueChange = viewModel::onNameChange,
                    modifier = Modifier.testTag(SUBJECT_NAME_TAG),
                    placeholder = stringResource(R.string.subject_name_placeholder),
                    maxLength = FolderNameValidator.MAX_LENGTH,
                    capitalization = KeyboardCapitalization.Sentences,
                    isError = nameError(error) != null,
                )
                nameError(error)?.let { FieldError(text = it) }
            }

            Text(
                text = stringResource(R.string.subject_sessions_title),
                style = MaterialTheme.typography.labelLarge,
                color = TealDeep,
                modifier = Modifier.padding(start = Spacing.x1, top = Spacing.x1),
            )
            Text(
                text = stringResource(R.string.subject_sessions_hint),
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
                modifier = Modifier.padding(horizontal = Spacing.x1),
            )

            viewModel.rows.forEachIndexed { index, row ->
                // La clave conserva el estado de cada franja cuando se quita otra.
                key(row.key) {
                    SessionCard(
                        index = index,
                        row = row,
                        errorMessage = sessionError(error, index),
                        onDayChange = { day -> viewModel.updateRow(index) { it.copy(day = day) } },
                        onStartChange = { text -> viewModel.updateRow(index) { it.copy(start = text) } },
                        onEndChange = { text -> viewModel.updateRow(index) { it.copy(end = text) } },
                        onRemove = { viewModel.removeRow(index) },
                    )
                }
            }

            SecondaryButton(
                text = stringResource(R.string.action_add_session),
                onClick = viewModel::addRow,
                modifier = Modifier.fillMaxWidth(),
            )

            if (!viewModel.isNew) {
                TextAction(
                    text = stringResource(R.string.action_delete_subject),
                    onClick = { confirmDelete = true },
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )
            }
        }

        Surface(color = White, shadowElevation = 8.dp) {
            PrimaryButton(
                text = stringResource(R.string.action_save_subject),
                onClick = { viewModel.save(onClose) },
                enabled = !viewModel.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = Spacing.x3, vertical = Spacing.x2),
            )
        }
    }

    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.subject_delete_title, viewModel.name),
            message = stringResource(R.string.subject_delete_message),
            confirmText = stringResource(R.string.action_delete_subject),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = {
                confirmDelete = false
                viewModel.delete(onClose)
            },
            onDismiss = { confirmDelete = false },
        )
    }
}

@Composable
private fun nameError(error: SubjectFormResult?): String? = when (error) {
    SubjectFormResult.NameEmpty -> stringResource(R.string.subject_error_name_empty)
    SubjectFormResult.NameTooLong -> stringResource(R.string.folder_error_too_long)
    SubjectFormResult.NameDuplicate -> stringResource(R.string.subject_error_name_duplicate)
    else -> null
}

/** Mensaje del problema de la franja [row], si el error del formulario es de esa franja. */
@Composable
private fun sessionError(error: SubjectFormResult?, row: Int): String? = when {
    error is SubjectFormResult.DayMissing && error.row == row ->
        stringResource(R.string.subject_error_day_missing)
    error is SubjectFormResult.TimeInvalid && error.row == row ->
        stringResource(R.string.subject_error_time_invalid)
    error is SubjectFormResult.EndNotAfterStart && error.row == row ->
        stringResource(R.string.subject_error_end_before_start)
    error is SubjectFormResult.Overlap && error.row == row ->
        if (error.otherSubject == null) {
            stringResource(R.string.subject_error_overlap_same)
        } else {
            stringResource(R.string.subject_error_overlap_other, error.otherSubject)
        }
    else -> null
}

@Composable
private fun SessionCard(
    index: Int,
    row: SessionRow,
    errorMessage: String?,
    onDayChange: (DayOfWeek) -> Unit,
    onStartChange: (String) -> Unit,
    onEndChange: (String) -> Unit,
    onRemove: () -> Unit,
) {
    FormCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.subject_session_number, index + 1),
                style = MaterialTheme.typography.titleSmall,
                color = Ink,
                modifier = Modifier.weight(1f),
            )
            TextAction(
                text = stringResource(R.string.action_remove_session),
                onClick = onRemove,
                modifier = Modifier.testTag(sessionRemoveTag(index)),
            )
        }

        FieldLabel(stringResource(R.string.subject_day_label))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            DayOfWeek.values().forEach { day ->
                DayChip(
                    day = day,
                    selected = row.day == day,
                    onClick = { onDayChange(day) },
                    modifier = Modifier.testTag(sessionDayTag(index, day)),
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.x1),
            ) {
                FieldLabel(stringResource(R.string.subject_start_label))
                CaptuTextField(
                    value = row.start,
                    onValueChange = onStartChange,
                    modifier = Modifier.testTag(sessionStartTag(index)),
                    placeholder = stringResource(R.string.subject_start_placeholder),
                    maxLength = TIME_MAX_LENGTH,
                    keyboardType = KeyboardType.Number,
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(Spacing.x1),
            ) {
                FieldLabel(stringResource(R.string.subject_end_label))
                CaptuTextField(
                    value = row.end,
                    onValueChange = onEndChange,
                    modifier = Modifier.testTag(sessionEndTag(index)),
                    placeholder = stringResource(R.string.subject_end_placeholder),
                    maxLength = TIME_MAX_LENGTH,
                    keyboardType = KeyboardType.Number,
                )
            }
        }

        if (errorMessage != null) FieldError(text = errorMessage)
    }
}

/** Selector de día: círculo con la inicial. El día elegido se rellena y además se anuncia como seleccionado. */
@Composable
private fun DayChip(
    day: DayOfWeek,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val dayName = stringResource(day.nameRes())
    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(if (selected) TealDeep else White)
            .then(if (selected) Modifier else Modifier.border(BorderStroke(1.5.dp, Line), CircleShape))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = dayName },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(day.initialRes()),
            style = MaterialTheme.typography.labelLarge,
            color = if (selected) White else Ink,
        )
    }
}

@Composable
private fun FormCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = White,
        border = BorderStroke(1.dp, Line),
    ) {
        Column(
            modifier = Modifier.padding(Spacing.x2),
            verticalArrangement = Arrangement.spacedBy(Spacing.x1),
        ) {
            content()
        }
    }
}

@Composable
private fun FieldLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = Ink,
    )
}

/** Acción secundaria en texto, para opciones de menor peso que un botón. */
@Composable
private fun TextAction(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = TealDeep,
        modifier = modifier
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = Spacing.x1),
    )
}
