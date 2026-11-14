package com.innova.captuia.domain.model

import java.time.DayOfWeek

/** Carpetas que CaptuIA crea y llena por su cuenta (las de la landing). */
enum class SystemFolder { DOCUMENTS, TASKS, COURSES, IMAGES }

enum class FolderKind {
    /** Documentos, Tareas, Cursos, Imágenes. */
    SYSTEM,

    /** Una materia del horario de clases. */
    SUBJECT,

    /** Creada por el usuario. */
    CUSTOM,
}

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
    val kind: FolderKind,
    /** Solo en las carpetas del sistema. */
    val systemKey: SystemFolder? = null,
) {
    val isCustom: Boolean get() = kind == FolderKind.CUSTOM
}

data class FolderSummary(
    val folder: Folder,
    val captureCount: Int,
)

/** Una franja de clase: día de la semana y horas en minutos desde la medianoche. */
data class ClassSession(
    val dayOfWeek: DayOfWeek,
    val startMinutes: Int,
    val endMinutes: Int,
)

/** Una materia del horario: su carpeta y cuándo se dicta. */
data class Subject(
    val folder: Folder,
    val sessions: List<ClassSession>,
) {
    val name: String get() = folder.name
}
