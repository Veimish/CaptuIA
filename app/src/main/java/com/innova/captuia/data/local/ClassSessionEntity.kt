package com.innova.captuia.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Una franja de clase de una materia. La materia es una carpeta de tipo SUBJECT. */
@Entity(
    tableName = "class_sessions",
    indices = [Index(value = ["folderId"])],
    foreignKeys = [
        ForeignKey(
            entity = FolderEntity::class,
            parentColumns = ["id"],
            childColumns = ["folderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class ClassSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long,
    /** 1 = lunes ... 7 = domingo (ISO-8601). */
    val dayOfWeek: Int,
    val startMinutes: Int,
    val endMinutes: Int,
)
