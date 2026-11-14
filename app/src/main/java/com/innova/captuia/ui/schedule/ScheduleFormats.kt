package com.innova.captuia.ui.schedule

import androidx.annotation.StringRes
import com.innova.captuia.R
import com.innova.captuia.domain.model.ClassSession
import com.innova.captuia.domain.schedule.TimeOfDay
import java.time.DayOfWeek

/** Nombre completo del día ("Lunes"). */
@StringRes
fun DayOfWeek.nameRes(): Int = when (this) {
    DayOfWeek.MONDAY -> R.string.day_monday
    DayOfWeek.TUESDAY -> R.string.day_tuesday
    DayOfWeek.WEDNESDAY -> R.string.day_wednesday
    DayOfWeek.THURSDAY -> R.string.day_thursday
    DayOfWeek.FRIDAY -> R.string.day_friday
    DayOfWeek.SATURDAY -> R.string.day_saturday
    DayOfWeek.SUNDAY -> R.string.day_sunday
}

/** Inicial del día para los selectores ("L", "M", "X"...). */
@StringRes
fun DayOfWeek.initialRes(): Int = when (this) {
    DayOfWeek.MONDAY -> R.string.day_monday_initial
    DayOfWeek.TUESDAY -> R.string.day_tuesday_initial
    DayOfWeek.WEDNESDAY -> R.string.day_wednesday_initial
    DayOfWeek.THURSDAY -> R.string.day_thursday_initial
    DayOfWeek.FRIDAY -> R.string.day_friday_initial
    DayOfWeek.SATURDAY -> R.string.day_saturday_initial
    DayOfWeek.SUNDAY -> R.string.day_sunday_initial
}

/** Ejemplo: "07:00 – 09:00". */
fun ClassSession.timeRange(): String =
    "${TimeOfDay.format(startMinutes)} – ${TimeOfDay.format(endMinutes)}"
