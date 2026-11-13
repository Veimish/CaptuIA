package com.innova.captuia.ui.home

import com.innova.captuia.domain.model.Capture
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/** Cómo se nombra un día respecto a hoy. */
enum class RelativeDay { TODAY, YESTERDAY, EARLIER }

/** Capturas de un mismo día, para mostrarlas agrupadas en la galería. */
data class DaySection(
    val date: LocalDate,
    val relativeDay: RelativeDay,
    val captures: List<Capture>,
)

/**
 * Agrupa las capturas por día (del más reciente al más antiguo) y, dentro de
 * cada día, de la más reciente a la más antigua.
 */
fun groupByDay(captures: List<Capture>, today: LocalDate, zone: ZoneId): List<DaySection> =
    captures
        .groupBy { Instant.ofEpochMilli(it.dateAddedMillis).atZone(zone).toLocalDate() }
        .entries
        .sortedByDescending { it.key }
        .map { (date, items) ->
            DaySection(
                date = date,
                relativeDay = when (date) {
                    today -> RelativeDay.TODAY
                    today.minusDays(1) -> RelativeDay.YESTERDAY
                    else -> RelativeDay.EARLIER
                },
                captures = items.sortedByDescending { it.dateAddedMillis },
            )
        }
