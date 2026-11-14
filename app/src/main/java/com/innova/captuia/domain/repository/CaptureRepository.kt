package com.innova.captuia.domain.repository

import com.innova.captuia.domain.model.Capture
import com.innova.captuia.domain.model.CaptureFilter
import com.innova.captuia.domain.model.TextProgress
import kotlinx.coroutines.flow.Flow

/** Acceso al índice de capturas de CaptuIA. */
interface CaptureRepository {

    /** Capturas de [filter], de la más reciente a la más antigua. */
    fun observe(filter: CaptureFilter): Flow<List<Capture>>

    /** Una captura por su identificador; emite null si deja de existir. */
    fun observeById(id: Long): Flow<Capture?>

    /**
     * Capturas cuyo texto contiene todas las palabras de [query], sin
     * distinguir mayúsculas ni tildes. Una consulta vacía no devuelve nada.
     */
    fun search(query: String): Flow<List<Capture>>

    /** Cuántas capturas faltan por leer. */
    fun observeTextProgress(): Flow<TextProgress>

    /**
     * Iguala el índice con la galería del dispositivo: agrega las capturas
     * nuevas y retira las que el usuario borró.
     */
    suspend fun syncWithDevice()

    /** Siguientes capturas sin leer, de la más reciente a la más antigua. */
    suspend fun nextPendingText(limit: Int): List<Capture>

    /** Guarda el texto leído de una captura. */
    suspend fun saveText(id: Long, text: String)

    /** Marca que la captura no se pudo leer. */
    suspend fun markTextFailed(id: Long)

    /** Capturas ya leídas que todavía no tienen carpeta. */
    suspend fun nextUnclassified(limit: Int): List<Capture>

    /** Capturas que CaptuIA clasificó por su cuenta. */
    suspend fun automaticallyClassified(): List<Capture>

    /**
     * Asigna la carpeta decidida por CaptuIA. No tiene efecto si el usuario ya
     * eligió una carpeta para esa captura.
     */
    suspend fun assignFolderAutomatically(captureId: Long, folderId: Long)

    /** Mueve la captura a la carpeta que eligió el usuario. */
    suspend fun moveToFolder(captureId: Long, folderId: Long)
}
