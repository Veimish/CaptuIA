package com.innova.captuia.domain.text

/** Lee el texto que aparece en una imagen (OCR). */
interface TextReader {

    /**
     * Devuelve el texto de la imagen en [contentUri]; cadena vacía si no tiene.
     * Lanza una excepción si la imagen no se puede abrir.
     */
    suspend fun read(contentUri: String): String
}
