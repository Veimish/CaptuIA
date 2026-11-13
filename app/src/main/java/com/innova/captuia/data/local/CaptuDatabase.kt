package com.innova.captuia.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos local de CaptuIA. Vive solo en el dispositivo.
 * Cada cambio de esquema sube [version] y agrega su migración.
 */
@Database(
    entities = [CaptureEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class CaptuDatabase : RoomDatabase() {

    abstract fun captureDao(): CaptureDao

    companion object {
        private const val FILE_NAME = "captuia.db"

        fun create(context: Context): CaptuDatabase =
            Room.databaseBuilder(context, CaptuDatabase::class.java, FILE_NAME).build()
    }
}
