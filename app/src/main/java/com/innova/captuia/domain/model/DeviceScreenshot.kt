package com.innova.captuia.domain.model

/**
 * Una captura de pantalla tal como existe en la galería del dispositivo.
 * La imagen no se copia: [contentUri] apunta al archivo original.
 */
data class DeviceScreenshot(
    val mediaId: Long,
    val contentUri: String,
    val displayName: String,
    val dateAddedMillis: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
)
