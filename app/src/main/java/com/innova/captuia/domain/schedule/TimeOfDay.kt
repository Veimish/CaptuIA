package com.innova.captuia.domain.schedule

import java.util.Locale

/**
 * Horas del día escritas por el usuario en formato de 24 horas. Acepta "7:00",
 * "14:30", y también solo dígitos ("700", "1430") para escribir con el teclado numérico.
 */
object TimeOfDay {

    private val pattern = Regex("^(\\d{1,2})[:.]?(\\d{2})$")

    /** Minutos desde la medianoche, o null si [text] no es una hora válida. */
    fun parse(text: String): Int? {
        val match = pattern.matchEntire(text.trim()) ?: return null
        val hours = match.groupValues[1].toInt()
        val minutes = match.groupValues[2].toInt()
        if (hours > 23 || minutes > 59) return null
        return hours * 60 + minutes
    }

    /** Ejemplo: 420 -> "07:00". */
    fun format(minutesOfDay: Int): String =
        String.format(Locale.ROOT, "%02d:%02d", minutesOfDay / 60, minutesOfDay % 60)
}
