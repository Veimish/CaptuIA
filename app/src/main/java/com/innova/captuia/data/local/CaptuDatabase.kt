package com.innova.captuia.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos local de CaptuIA. Vive solo en el dispositivo.
 *
 * Mientras la app está en desarrollo (antes de la versión 1.0) un cambio de
 * esquema reconstruye el índice desde la galería en lugar de migrarlo: hasta
 * ahora todo su contenido se obtiene del dispositivo. A partir de la primera
 * versión publicada, cada cambio de esquema debe llevar su migración.
 */
@Database(
    entities = [CaptureEntity::class, FolderEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class CaptuDatabase : RoomDatabase() {

    abstract fun captureDao(): CaptureDao

    abstract fun folderDao(): FolderDao

    companion object {
        private const val FILE_NAME = "captuia.db"

        fun create(context: Context): CaptuDatabase =
            Room.databaseBuilder(context, CaptuDatabase::class.java, FILE_NAME)
                .fallbackToDestructiveMigration()
                .build()
    }
}
