package com.innova.captuia.data.ocr

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.innova.captuia.domain.text.TextReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Lectura de texto con ML Kit de Google. El modelo va incluido en la app y se
 * ejecuta en el dispositivo: la imagen no se envía a ningún servidor.
 */
class MlKitTextReader(private val context: Context) : TextReader {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    override suspend fun read(contentUri: String): String {
        // Abrir y decodificar la imagen es trabajo de disco: fuera del hilo principal.
        val image = withContext(Dispatchers.IO) {
            InputImage.fromFilePath(context, Uri.parse(contentUri))
        }
        return suspendCancellableCoroutine { continuation ->
            recognizer.process(image)
                .addOnSuccessListener { result -> continuation.resume(result.text) }
                .addOnFailureListener { error -> continuation.resumeWithException(error) }
        }
    }
}
