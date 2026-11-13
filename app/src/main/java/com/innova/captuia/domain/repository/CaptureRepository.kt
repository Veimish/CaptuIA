package com.innova.captuia.domain.repository

import com.innova.captuia.domain.model.Capture
import kotlinx.coroutines.flow.Flow

/** Acceso al índice de capturas de CaptuIA. */
interface CaptureRepository {

    /** Todas las capturas indexadas, de la más reciente a la más antigua. */
    fun observeAll(): Flow<List<Capture>>

    /** Una captura por su identificador; emite null si deja de existir. */
    fun observeById(id: Long): Flow<Capture?>

    /**
     * Iguala el índice con la galería del dispositivo: agrega las capturas
     * nuevas y retira las que el usuario borró.
     */
    suspend fun syncWithDevice()
}
