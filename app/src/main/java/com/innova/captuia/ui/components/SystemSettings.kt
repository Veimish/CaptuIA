package com.innova.captuia.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings

/** Abre la pantalla de ajustes del sistema para CaptuIA (permisos incluidos). */
fun Context.openAppSettings() {
    val intent = Intent(
        Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
        Uri.fromParts("package", packageName, null),
    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    startActivity(intent)
}
