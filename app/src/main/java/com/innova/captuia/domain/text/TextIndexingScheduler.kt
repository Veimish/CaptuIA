package com.innova.captuia.domain.text

/** Pone en marcha, en segundo plano, la lectura de texto de las capturas pendientes. */
interface TextIndexingScheduler {
    fun schedule()
}
