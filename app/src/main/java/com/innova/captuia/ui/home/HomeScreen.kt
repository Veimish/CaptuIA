package com.innova.captuia.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.innova.captuia.R
import com.innova.captuia.data.media.MediaAccess
import com.innova.captuia.data.media.MediaPermissions
import com.innova.captuia.ui.components.BrandHeader
import com.innova.captuia.ui.components.CaptuCard
import com.innova.captuia.ui.components.Notice
import com.innova.captuia.ui.components.SecondaryButton
import com.innova.captuia.ui.components.openAppSettings
import com.innova.captuia.ui.theme.Ink
import com.innova.captuia.ui.theme.InkSoft
import com.innova.captuia.ui.theme.Line
import com.innova.captuia.ui.theme.Paper
import com.innova.captuia.ui.theme.Spacing
import com.innova.captuia.ui.theme.TealDark
import java.text.DateFormat
import java.text.NumberFormat
import java.util.Date

/**
 * Inicio (Etapa 0): muestra cuántas capturas reales hay en el dispositivo.
 * Se actualiza cada vez que la app vuelve a primer plano.
 */
@Composable
fun HomeScreen(
    onAccessLost: () -> Unit,
    viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory),
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var access by remember { mutableStateOf(MediaPermissions.current(context)) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        access = MediaPermissions.current(context)
        if (access == MediaAccess.DENIED) onAccessLost() else viewModel.refresh()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper),
    ) {
        BrandHeader(title = stringResource(R.string.home_title))

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(Spacing.x3),
            verticalArrangement = Arrangement.spacedBy(Spacing.x2),
        ) {
            if (access == MediaAccess.PARTIAL) {
                Notice(text = stringResource(R.string.home_partial))
                SecondaryButton(
                    text = stringResource(R.string.action_open_settings),
                    onClick = { context.openAppSettings() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.hasError) {
                Notice(text = stringResource(R.string.home_error))
            }

            val total = state.total
            if (total == null) {
                if (state.isLoading) LoadingCard()
            } else if (total == 0) {
                EmptyCard()
            } else {
                SummaryCard(total = total, latestMillis = state.latestMillis)
            }

            SecondaryButton(
                text = stringResource(R.string.home_refresh),
                onClick = { viewModel.refresh() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
            )

            Text(
                text = stringResource(R.string.home_stage_note),
                style = MaterialTheme.typography.bodySmall,
                color = InkSoft,
            )
        }
    }
}

@Composable
private fun LoadingCard() {
    CaptuCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = TealDark,
                strokeWidth = 3.dp,
            )
            Spacer(Modifier.width(Spacing.x2))
            Text(
                text = stringResource(R.string.home_loading),
                style = MaterialTheme.typography.bodyLarge,
                color = Ink,
            )
        }
    }
}

@Composable
private fun EmptyCard() {
    CaptuCard {
        Text(
            text = stringResource(R.string.home_empty_title),
            style = MaterialTheme.typography.titleLarge,
            color = Ink,
        )
        Spacer(Modifier.height(Spacing.x1))
        Text(
            text = stringResource(R.string.home_empty_text),
            style = MaterialTheme.typography.bodyMedium,
            color = InkSoft,
        )
    }
}

@Composable
private fun SummaryCard(total: Int, latestMillis: Long?) {
    val formattedTotal = remember(total) { NumberFormat.getIntegerInstance().format(total) }
    val formattedDate = remember(latestMillis) {
        latestMillis?.let {
            DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(it))
        }
    }

    CaptuCard {
        Text(
            text = formattedTotal,
            style = MaterialTheme.typography.displayLarge,
            color = TealDark,
        )
        Text(
            text = stringResource(
                if (total == 1) R.string.home_found_one else R.string.home_found_many,
            ),
            style = MaterialTheme.typography.bodyLarge,
            color = InkSoft,
        )
        if (formattedDate != null) {
            Spacer(Modifier.height(Spacing.x2))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Line),
            )
            Spacer(Modifier.height(Spacing.x2))
            Text(
                text = stringResource(R.string.home_latest, formattedDate),
                style = MaterialTheme.typography.bodyMedium,
                color = Ink,
            )
        }
    }
}
