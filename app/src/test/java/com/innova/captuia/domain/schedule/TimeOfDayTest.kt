package com.innova.captuia.domain.schedule

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TimeOfDayTest {

    @Test
    fun `acepta horas de 24 horas con uno o dos digitos`() {
        assertEquals(7 * 60, TimeOfDay.parse("7:00"))
        assertEquals(7 * 60, TimeOfDay.parse("07:00"))
        assertEquals(14 * 60 + 30, TimeOfDay.parse(" 14:30 "))
        assertEquals(0, TimeOfDay.parse("0:00"))
        assertEquals(23 * 60 + 59, TimeOfDay.parse("23:59"))
    }

    @Test
    fun `acepta punto como separador`() {
        assertEquals(9 * 60 + 15, TimeOfDay.parse("9.15"))
    }

    @Test
    fun `acepta solo digitos para el teclado numerico`() {
        assertEquals(7 * 60, TimeOfDay.parse("700"))
        assertEquals(14 * 60 + 30, TimeOfDay.parse("1430"))
        assertEquals(5, TimeOfDay.parse("005"))
    }

    @Test
    fun `rechaza lo que no es una hora`() {
        listOf("", "7", "70", "7:0", "24:00", "2400", "12:60", "7:00 am", "siete", "7:000", "70000", "-1:00").forEach {
            assertNull("\"$it\" no debería ser una hora válida", TimeOfDay.parse(it))
        }
    }

    @Test
    fun `da formato de dos digitos`() {
        assertEquals("07:00", TimeOfDay.format(420))
        assertEquals("14:05", TimeOfDay.format(14 * 60 + 5))
        assertEquals("00:00", TimeOfDay.format(0))
    }
}
