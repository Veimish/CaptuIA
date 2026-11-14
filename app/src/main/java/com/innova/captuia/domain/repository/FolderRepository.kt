package com.innova.captuia.domain.repository

import com.innova.captuia.domain.folder.FolderNameCheck
import com.innova.captuia.domain.model.Folder
import com.innova.captuia.domain.model.FolderSummary
import com.innova.captuia.domain.model.SystemFolder
import kotlinx.coroutines.flow.Flow

/** Carpetas en las que se organizan las capturas. */
interface FolderRepository {

    /** Todas las carpetas con su número de capturas: primero las del sistema, luego las del usuario. */
    fun observeSummaries(): Flow<List<FolderSummary>>

    /** Una carpeta por su identificador; emite null si deja de existir. */
    fun observeById(id: Long): Flow<Folder?>

    /** Crea las carpetas del sistema si aún no existen. */
    suspend fun ensureSystemFolders()

    suspend fun systemFolderId(key: SystemFolder): Long

    /**
     * Crea una carpeta del usuario. Devuelve [FolderNameCheck.Valid] si se creó,
     * o el motivo por el que el nombre no sirve.
     */
    suspend fun create(name: String): FolderNameCheck

    /** Cambia el nombre de una carpeta del usuario, con las mismas reglas que [create]. */
    suspend fun rename(id: Long, name: String): FolderNameCheck

    /**
     * Elimina una carpeta del usuario. Sus capturas no se borran: quedan sin
     * carpeta para que CaptuIA las vuelva a clasificar.
     */
    suspend fun delete(id: Long)
}
