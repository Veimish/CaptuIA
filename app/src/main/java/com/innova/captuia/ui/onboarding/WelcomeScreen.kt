package com.innova.captuia.ui.onboarding

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.innova.captuia.R
import com.innova.captuia.data.media.MediaAccess
import com.innova.captuia.data.media.MediaPermissions
import com.innova.captuia.ui.components.BrandHeader
import com.innova.captuia.ui.components.Notice
import com.innova.captuia.ui.components.PrimaryButton
import com.innova.captuia.ui.components.SecondaryButton
import com.innova.captuia.ui.components.openAppSettings
import com.innova.captuia.ui.theme.Ink
import com.innova.captuia.ui.theme.InkSoft
import com.innova.captuia.ui.theme.Paper
import com.innova.captuia.ui.theme.Spacing
import com.innova.captuia.ui.theme.TealDeep
import com.innova.captuia.ui.theme.White

/**
 * Pantalla de bienvenida: explica el producto (mismos cuatro pasos de la
 * landing) y solicita el permiso real de lectura de imágenes.
 */
@Composable
fun WelcomeScreen(onAccessReady: () -> Unit) {
    val context = LocalContext.current
    var access by remember { mutableStateOf(MediaPermissions.current(context)) }
    var alreadyRequested by rememberSaveable { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) {
        alreadyRequested = true
        access = MediaPermissions.current(context)
        if (access == MediaAccess.FULL) onAccessReady()
    }

    // Al volver de los ajustes del sistema, el permiso pudo haber cambiado.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        access = MediaPermissions.current(context)
        if (access == MediaAccess.FULL) onAccessReady()
    }

    val wasDenied = alreadyRequested && access == MediaAccess.DENIED
    val isPartial = access == MediaAccess.PARTIAL

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Paper),
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
        ) {
            BrandHeader(title = stringResource(R.string.welcome_headline))

            Column(
                modifier = Modifier.padding(Spacing.x3),
                verticalArrangement = Arrangement.spacedBy(Spacing.x2),
            ) {
                Text(
                    text = stringResource(R.string.welcome_lead),
                    style = MaterialTheme.typography.bodyLarge,
                    color = InkSoft,
                )
                Text(
                    text = stringResource(R.string.welcome_how_kicker),
                    style = MaterialTheme.typography.labelLarge,
                    color = TealDeep,
                )
                StepRow(
                    number = 1,
                    title = stringResource(R.string.welcome_step1_title),
                    text = stringResource(R.string.welcome_step1_text),
                )
                StepRow(
                    number = 2,
                    title = stringResource(R.string.welcome_step2_title),
                    text = stringResource(R.string.welcome_step2_text),
                )
                StepRow(
                    number = 3,
                    title = stringResource(R.string.welcome_step3_title),
                    text = stringResource(R.string.welcome_step3_text),
                )
                StepRow(
                    number = 4,
                    title = stringResource(R.string.welcome_step4_title),
                    text = stringResource(R.string.welcome_step4_text),
                )
                Notice(
                    text = stringResource(R.string.welcome_privacy),
                    iconRes = R.drawable.ic_shield,
                )
            }
        }

        Surface(color = White, shadowElevation = 8.dp) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = Spacing.x3, vertical = Spacing.x2),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                when {
                    isPartial -> {
                        Notice(text = stringResource(R.string.welcome_partial))
                        PrimaryButton(
                            text = stringResource(R.string.action_open_settings),
                            onClick = { context.openAppSettings() },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        SecondaryButton(
                            text = stringResource(R.string.welcome_continue_partial),
                            onClick = onAccessReady,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }

                    else -> {
                        if (wasDenied) {
                            Notice(text = stringResource(R.string.welcome_denied))
                        }
                        PrimaryButton(
                            text = stringResource(R.string.welcome_cta),
                            onClick = { permissionLauncher.launch(MediaPermissions.toRequest()) },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        if (wasDenied) {
                            SecondaryButton(
                                text = stringResource(R.string.action_open_settings),
                                onClick = { context.openAppSettings() },
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepRow(number: Int, title: String, text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(9.dp))
                .background(TealDeep),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = number.toString(),
                style = MaterialTheme.typography.titleSmall,
                color = White,
            )
        }
        Spacer(Modifier.width(Spacing.x2))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Ink,
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = InkSoft,
            )
        }
    }
}
