package com.innova.captuia.ui.home

import com.innova.captuia.domain.model.Capture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class GroupByDayTest {

    private val zone: ZoneId = ZoneId.of("America/Bogota")
    private val today: LocalDate = LocalDate.of(2026, 11, 13)

    private fun capture(id: Long, dateTime: LocalDateTime) = Capture(
        id = id,
        contentUri = "content://media/external/images/media/$id",
        displayName = "Screenshot_$id.png",
        dateAddedMillis = dateTime.atZone(zone).toInstant().toEpochMilli(),
        width = 1080,
        height = 2400,
        sizeBytes = 100_000,
    )

    @Test
    fun `sin capturas no hay secciones`() {
        assertTrue(groupByDay(emptyList(), today, zone).isEmpty())
    }

    @Test
    fun `agrupa por dia del mas reciente al mas antiguo`() {
        val sections = groupByDay(
            listOf(
                capture(1, LocalDateTime.of(2026, 11, 10, 9, 0)),
                capture(2, LocalDateTime.of(2026, 11, 13, 8, 0)),
                capture(3, LocalDateTime.of(2026, 11, 12, 22, 30)),
                capture(4, LocalDateTime.of(2026, 11, 13, 17, 45)),
            ),
            today,
            zone,
        )

        assertEquals(
            listOf(LocalDate.of(2026, 11, 13), LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 10)),
            sections.map { it.date },
        )
        assertEquals(
            listOf(RelativeDay.TODAY, RelativeDay.YESTERDAY, RelativeDay.EARLIER),
            sections.map { it.relativeDay },
        )
    }

    @Test
    fun `dentro del dia ordena de la mas reciente a la mas antigua`() {
        val sections = groupByDay(
            listOf(
                capture(2, LocalDateTime.of(2026, 11, 13, 8, 0)),
                capture(4, LocalDateTime.of(2026, 11, 13, 17, 45)),
                capture(5, LocalDateTime.of(2026, 11, 13, 12, 0)),
            ),
            today,
            zone,
        )

        assertEquals(1, sections.size)
        assertEquals(listOf(4L, 5L, 2L), sections.single().captures.map { it.id })
    }

    @Test
    fun `una captura de las 11 59 pm pertenece a ese dia y no al siguiente`() {
        val sections = groupByDay(
            listOf(
                capture(1, LocalDateTime.of(2026, 11, 12, 23, 59)),
                capture(2, LocalDateTime.of(2026, 11, 13, 0, 1)),
            ),
            today,
            zone,
        )

        assertEquals(2, sections.size)
        assertEquals(listOf(2L), sections[0].captures.map { it.id })
        assertEquals(listOf(1L), sections[1].captures.map { it.id })
    }
}
