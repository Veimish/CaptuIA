package com.innova.captuia.domain.repository

import com.innova.captuia.domain.model.ClassSession
import com.innova.captuia.domain.model.Subject
import kotlinx.coroutines.flow.Flow

/** Horario de clases: las materias del usuario y sus franjas. */
interface ScheduleRepository {

    /** Materias en orden alfabético, cada una con sus franjas ordenadas por día y hora. */
    fun observeSubjects(): Flow<List<Subject>>

    suspend fun subjects(): List<Subject>

    /** Nombres de todas las carpetas, para no repetir uno al nombrar una materia. */
    suspend fun allFolderNames(): List<String>

    /**
     * Guarda una materia ya validada. Con [folderId] null crea una nueva; si no,
     * actualiza la existente. Devuelve el identificador de su carpeta.
     */
    suspend fun saveSubject(folderId: Long?, name: String, sessions: List<ClassSession>): Long

    /**
     * Elimina la materia y su carpeta. Sus capturas no se borran: quedan sin
     * carpeta para que CaptuIA las vuelva a clasificar.
     */
    suspend fun deleteSubject(folderId: Long)
}
