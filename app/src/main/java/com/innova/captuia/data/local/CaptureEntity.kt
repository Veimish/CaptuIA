package com.innova.captuia.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** Fila del índice local. La clave es el identificador de la imagen en MediaStore. */
@Entity(
    tableName = "captures",
    indices = [Index(value = ["dateAddedMillis"]), Index(value = ["textStatus"])],
)
data class CaptureEntity(
    @PrimaryKey val mediaId: Long,
    val contentUri: String,
    val displayName: String,
    val dateAddedMillis: Long,
    val width: Int,
    val height: Int,
    val sizeBytes: Long,
    /** Nombre de un valor de TextStatus. */
    val textStatus: String,
    /** Texto reconocido tal como se leyó. */
    val recognizedText: String?,
    /** El mismo texto en minúsculas y sin tildes, para buscar. */
    val searchText: String?,
)
