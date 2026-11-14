package com.innova.captuia.ui.schedule

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.innova.captuia.R
import com.innova.captuia.domain.model.Subject
import com.innova.captuia.ui.components.CaptuCard
import com.innova.captuia.ui.components.CompactHeader
import com.innova.captuia.ui.components.FolderIcon
import com.innova.captuia.ui.components.FolderVisual
import com.innova.captuia.ui.components.PrimaryButton
import com.innova.captuia.ui.theme.Ink
import com.innova.captuia.ui.theme.InkSoft
import com.innova.captuia.ui.theme.Line
import com.innova.captuia.ui.theme.Paper
import com.innova.captuia.ui.theme.Spacing
import com.innova.captuia.ui.theme.TealDeep
import com.innova.captuia.ui.theme.White

/** Identificador de la tarjeta de una materia, para las pruebas de interfaz. */
fun subjectCardTag(name: String) = "materia-$name"

/** Horario de clases: las materias registradas y sus franjas. */
@Composable
fun ScheduleScreen(
    onBack: () -> Unit,
    onAddSubject: () -> Unit,
    onEditSubject: (Long) -> Unit,
    viewModel: ScheduleViewModel = viewModel(factory = ScheduleViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper),
    ) {
        CompactHeader(title = stringResource(R.string.schedule_title), onBack = onBack)

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(Spacing.x2),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "intro") {
                Text(
                    text = stringResource(R.string.schedule_intro),
                    style = MaterialTheme.typography.bodyMedium,
                    color = InkSoft,
                    modifier = Modifier.padding(horizontal = Spacing.x1, vertical = Spacing.half),
                )
            }
            if (!state.isLoading && state.subjects.isEmpty()) {
                item(key = "empty") {
                    CaptuCard {
                        Text(
                            text = stringResource(R.string.schedule_empty_title),
                            style = MaterialTheme.typography.titleLarge,
                            color = Ink,
                        )
                        Spacer(Modifier.height(Spacing.x1))
                        Text(
                            text = stringResource(R.string.schedule_empty_text),
                            style = MaterialTheme.typography.bodyMedium,
                            color = InkSoft,
                        )
                    }
                }
            }
            items(items = state.subjects, key = { it.folder.id }) { subject ->
                SubjectCard(subject = subject, onClick = { onEditSubject(subject.folder.id) })
            }
        }

        Surface(color = White, shadowElevation = 8.dp) {
            PrimaryButton(
                text = stringResource(R.string.action_add_subject),
                onClick = onAddSubject,
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = Spacing.x3, vertical = Spacing.x2),
            )
        }
    }
}

@Composable
private fun SubjectCard(subject: Subject, onClick: () -> Unit) {
    val shape = MaterialTheme.shapes.medium
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClickLabel = stringResource(R.string.action_edit_subject), onClick = onClick)
            .testTag(subjectCardTag(subject.name)),
        shape = shape,
        color = White,
        border = BorderStroke(1.dp, Line),
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top,
        ) {
            FolderIcon(visual = FolderVisual.of(subject.folder))
            Spacer(Modifier.width(12.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = Ink,
                )
                if (subject.sessions.isEmpty()) {
                    Text(
                        text = stringResource(R.string.schedule_no_sessions),
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                }
                subject.sessions.forEach { session ->
                    Text(
                        text = stringResource(
                            R.string.schedule_session_line,
                            stringResource(session.dayOfWeek.nameRes()),
                            session.timeRange(),
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = InkSoft,
                    )
                }
            }
            Text(
                text = stringResource(R.string.action_edit),
                style = MaterialTheme.typography.labelMedium,
                color = TealDeep,
            )
        }
    }
}
