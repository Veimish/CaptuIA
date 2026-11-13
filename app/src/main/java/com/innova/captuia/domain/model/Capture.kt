package com.innova.captuia.domain.model

/**
 * Una captura registrada en el índice de CaptuIA.
 * El archivo sigue en la galería del dispositivo; [contentUri] apunta a él.
 */
data class Capture(
    val id: Long,
    val contentUri: String,
    val displayName: String,
    val dateAddedMillis: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
)
