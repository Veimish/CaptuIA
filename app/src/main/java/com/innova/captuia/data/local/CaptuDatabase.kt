package com.innova.captuia.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Base de datos local de CaptuIA. Vive solo en el dispositivo.
 *
 * Además del índice de capturas (que se puede reconstruir desde la galería),
 * guarda datos del usuario que no se pueden recuperar: sus carpetas, las
 * capturas que movió a mano y su horario. Por eso cualquier cambio de esquema
 * debe subir la versión y llevar su migración; sin ella la app falla al abrir
 * en lugar de borrar los datos en silencio.
 */
@Database(
    entities = [CaptureEntity::class, FolderEntity::class, ClassSessionEntity::class],
    version = 4,
    exportSchema = false,
)
abstract class CaptuDatabase : RoomDatabase() {

    abstract fun captureDao(): CaptureDao

    abstract fun folderDao(): FolderDao

    abstract fun scheduleDao(): ScheduleDao

    companion object {
        private const val FILE_NAME = "captuia.db"

        fun create(context: Context): CaptuDatabase =
            Room.databaseBuilder(context, CaptuDatabase::class.java, FILE_NAME).build()
    }
}
