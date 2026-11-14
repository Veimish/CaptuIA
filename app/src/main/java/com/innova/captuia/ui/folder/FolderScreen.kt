package com.innova.captuia.ui.folder

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.innova.captuia.R
import com.innova.captuia.domain.folder.FolderNameValidator
import com.innova.captuia.domain.model.CaptureFilter
import com.innova.captuia.ui.components.CaptuCard
import com.innova.captuia.ui.components.CaptuDialog
import com.innova.captuia.ui.components.CompactHeader
import com.innova.captuia.ui.components.ConfirmDialog
import com.innova.captuia.ui.components.PrimaryButton
import com.innova.captuia.ui.components.SecondaryButton
import com.innova.captuia.ui.components.TextInputDialog
import com.innova.captuia.ui.gallery.CaptureGrid
import com.innova.captuia.ui.gallery.captureCountText
import com.innova.captuia.ui.home.folderNameError
import com.innova.captuia.ui.theme.Ink
import com.innova.captuia.ui.theme.InkSoft
import com.innova.captuia.ui.theme.Paper
import com.innova.captuia.ui.theme.Spacing

private enum class FolderDialog { NONE, OPTIONS, RENAME, DELETE }

/** Capturas de una carpeta, agrupadas por día. Las carpetas del usuario se pueden renombrar y eliminar. */
@Composable
fun FolderScreen(
    onBack: () -> Unit,
    onOpenCapture: (Long) -> Unit,
    viewModel: FolderViewModel = viewModel(factory = FolderViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var dialog by rememberSaveable { mutableStateOf(FolderDialog.NONE) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.syncWithDevice() }

    // Si la carpeta se eliminó, no queda nada que mostrar aquí.
    if (state.isGone) {
        LaunchedEffect(Unit) { onBack() }
    }

    val folder = state.folder
    val title = when (state.filter) {
        CaptureFilter.All -> stringResource(R.string.folder_all)
        CaptureFilter.Unclassified -> stringResource(R.string.folder_unclassified)
        is CaptureFilter.InFolder -> folder?.name.orEmpty()
    }
    val navigationBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper),
    ) {
        CompactHeader(
            title = title,
            onBack = onBack,
            actionIconRes = if (folder?.isCustom == true) R.drawable.ic_more else null,
            actionDescription = stringResource(R.string.action_folder_options),
            onAction = { dialog = FolderDialog.OPTIONS },
        )

        CaptureGrid(
            sections = state.sections,
            onOpenCapture = onOpenCapture,
            contentPadding = PaddingValues(
                start = Spacing.x2,
                end = Spacing.x2,
                top = Spacing.x3,
                bottom = Spacing.x3 + navigationBarPadding,
            ),
            modifier = Modifier.weight(1f),
            header = {
                item(key = "summary", span = { GridItemSpan(maxLineSpan) }) {
                    if (state.isLoading) {
                        Spacer(Modifier.height(Spacing.x1))
                    } else if (state.total == 0) {
                        CaptuCard {
                            Text(
                                text = stringResource(R.string.folder_empty_title),
                                style = MaterialTheme.typography.titleLarge,
                                color = Ink,
                            )
                            Spacer(Modifier.height(Spacing.x1))
                            Text(
                                text = stringResource(
                                    if (state.filter == CaptureFilter.Unclassified) {
                                        R.string.folder_unclassified_empty_text
                                    } else {
                                        R.string.folder_empty_text
                                    },
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = InkSoft,
                            )
                        }
                    } else {
                        Column {
                            Text(
                                text = captureCountText(state.total),
                                style = MaterialTheme.typography.headlineSmall,
                                color = Ink,
                            )
                            if (state.filter == CaptureFilter.Unclassified) {
                                Text(
                                    text = stringResource(R.string.folder_unclassified_hint),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = InkSoft,
                                )
                            }
                        }
                    }
                }
            },
        )
    }

    when (dialog) {
        FolderDialog.NONE -> Unit

        FolderDialog.OPTIONS -> CaptuDialog(
            title = title,
            onDismiss = { dialog = FolderDialog.NONE },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                PrimaryButton(
                    text = stringResource(R.string.action_rename_folder),
                    onClick = {
                        viewModel.clearRenameError()
                        dialog = FolderDialog.RENAME
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                SecondaryButton(
                    text = stringResource(R.string.action_delete_folder),
                    onClick = { dialog = FolderDialog.DELETE },
                    modifier = Modifier.fillMaxWidth(),
                )
                SecondaryButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = { dialog = FolderDialog.NONE },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        FolderDialog.RENAME -> TextInputDialog(
            title = stringResource(R.string.folder_rename_title),
            label = stringResource(R.string.folder_name_label),
            initialValue = folder?.name.orEmpty(),
            maxLength = FolderNameValidator.MAX_LENGTH,
            errorMessage = folderNameError(viewModel.renameError),
            confirmText = stringResource(R.string.action_save),
            dismissText = stringResource(R.string.action_cancel),
            onValueChange = viewModel::clearRenameError,
            onConfirm = { name -> viewModel.rename(name) { dialog = FolderDialog.NONE } },
            onDismiss = { dialog = FolderDialog.NONE },
        )

        FolderDialog.DELETE -> ConfirmDialog(
            title = stringResource(R.string.folder_delete_title, title),
            message = stringResource(R.string.folder_delete_message),
            confirmText = stringResource(R.string.action_delete_folder),
            dismissText = stringResource(R.string.action_cancel),
            onConfirm = {
                dialog = FolderDialog.NONE
                viewModel.delete()
            },
            onDismiss = { dialog = FolderDialog.NONE },
        )
    }
}
