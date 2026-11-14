package com.innova.captuia.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "folders",
    indices = [
        Index(value = ["nameKey"], unique = true),
        Index(value = ["systemKey"], unique = true),
    ],
)
data class FolderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    /** Nombre normalizado: impide dos carpetas que solo difieran en mayúsculas o tildes. */
    val nameKey: String,
    /** Nombre de un valor de FolderKind. */
    val kind: String,
    /** Nombre de un valor de SystemFolder; null si no es una carpeta del sistema. */
    val systemKey: String?,
    /** Las del sistema van primero (en el orden de la landing), luego las materias y luego las del usuario. */
    val sortOrder: Int,
)

/** Una carpeta con el número de capturas que contiene. */
data class FolderWithCount(
    @Embedded val folder: FolderEntity,
    val captureCount: Int,
)
