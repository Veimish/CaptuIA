package com.innova.captuia.data.processing

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.innova.captuia.CaptuIAApplication
import com.innova.captuia.domain.text.TextIndexingScheduler

/**
 * Trabajo en segundo plano que lee el texto de las capturas pendientes.
 * Sigue funcionando aunque el usuario salga de la app.
 */
class TextIndexingWorker(
    context: Context,
    parameters: WorkerParameters,
) : CoroutineWorker(context, parameters) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as CaptuIAApplication).container
        container.textIndexer.processPending(shouldStop = { isStopped })
        // Si el sistema detuvo el trabajo, lo pendiente se retoma en la próxima sincronización.
        return Result.success()
    }
}

class WorkManagerTextIndexingScheduler(private val context: Context) : TextIndexingScheduler {

    override fun schedule() {
        // APPEND_OR_REPLACE: si ya hay una lectura en curso, la nueva corre al terminar,
        // de modo que las capturas agregadas mientras tanto no se quedan sin leer.
        WorkManager.getInstance(context).enqueueUniqueWork(
            UNIQUE_NAME,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<TextIndexingWorker>().build(),
        )
    }

    private companion object {
        const val UNIQUE_NAME = "lectura-de-texto"
    }
}
