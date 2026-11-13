package com.innova.captuia.ui.format

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * Formatos de presentación. La app está en español, así que fechas y números
 * se muestran siempre en español, sin depender del idioma del teléfono.
 */
object Formats {

    val locale: Locale = Locale("es", "CO")

    private val longDay: DateTimeFormatter =
        DateTimeFormatter.ofPattern("d 'de' MMMM 'de' yyyy", locale)

    /** Ejemplo: "13 nov 2026, 5:34 p. m." */
    fun dateTime(millis: Long): String =
        SimpleDateFormat("d MMM yyyy, h:mm a", locale).format(Date(millis))

    /** Ejemplo: "13 de noviembre de 2026" */
    fun day(date: LocalDate): String = date.format(longDay)

    /** Ejemplo: "1.204" */
    fun count(value: Int): String = NumberFormat.getIntegerInstance(locale).format(value)

    /** Ejemplo: "850 KB", "1,2 MB" */
    fun fileSize(bytes: Long): String {
        val kilobytes = bytes / 1024.0
        return if (kilobytes < 1024) {
            String.format(locale, "%.0f KB", kilobytes)
        } else {
            String.format(locale, "%.1f MB", kilobytes / 1024.0)
        }
    }
}
