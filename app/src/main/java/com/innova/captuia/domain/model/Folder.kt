package com.innova.captuia.domain.model

/** Carpetas que CaptuIA crea y llena por su cuenta (las de la landing). */
enum class SystemFolder { DOCUMENTS, TASKS, COURSES, IMAGES }

/** Cómo llegó una captura a su carpeta. */
enum class FolderSource {
    /** Todavía no tiene carpeta. */
    NONE,

    /** La asignó CaptuIA según el contenido. */
    AUTO,

    /** La eligió el usuario; CaptuIA no la vuelve a mover. */
    MANUAL,
}

data class Folder(
    val id: Long,
    val name: String,
    /** Null en las carpetas creadas por el usuario. */
    val systemKey: SystemFolder?,
) {
    val isCustom: Boolean get() = systemKey == null
}

data class FolderSummary(
    val folder: Folder,
    val captureCount: Int,
)
