package com.innova.captuia.domain.model

/** Estado de la lectura de texto de una captura. */
enum class TextStatus {
    /** Aún no se ha leído. */
    PENDING,

    /** Ya se leyó; [Capture.text] puede estar vacío si la imagen no tiene texto. */
    READY,

    /** No se pudo leer el archivo. */
    FAILED,
}

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
    val textStatus: TextStatus = TextStatus.PENDING,
    /** Texto reconocido en la imagen; null mientras no se haya leído. */
    val text: String? = null,
    /** Carpeta a la que pertenece; null si aún no se ha clasificado. */
    val folderId: Long? = null,
    val folderSource: FolderSource = FolderSource.NONE,
)

/** Avance de la lectura de texto sobre todas las capturas. */
data class TextProgress(
    val pending: Int = 0,
    val total: Int = 0,
) {
    val done: Int get() = total - pending
    val isWorking: Boolean get() = pending > 0
}

/** Qué capturas mostrar en una galería. */
sealed interface CaptureFilter {
    data object All : CaptureFilter
    data object Unclassified : CaptureFilter
    data class InFolder(val folderId: Long) : CaptureFilter
}
