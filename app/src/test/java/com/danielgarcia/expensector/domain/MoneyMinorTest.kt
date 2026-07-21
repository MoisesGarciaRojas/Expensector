package com.danielgarcia.expensector.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class MoneyMinorTest {
    @Test
    fun parsesMxnMinorUnitsExactly() {
        assertEquals(100L, MoneyMinor.parse("1.00", "MXN").minorUnits)
        assertEquals(0L, MoneyMinor.parse("0", "MXN").minorUnits)
        assertEquals(12345L, MoneyMinor.parse("123.45", "MXN").minorUnits)
    }

    @Test
    fun rejectsInvalidPrecisionAndCurrency() {
        assertThrows(IllegalArgumentException::class.java) { MoneyMinor.parse("1.001", "MXN") }
        assertThrows(IllegalArgumentException::class.java) { MoneyMinor.parse("-1.00", "MXN") }
        assertThrows(IllegalArgumentException::class.java) { MoneyMinor.parse("1.00", "mxn") }
    }

    @Test
    fun rejectsOverflow() {
        assertThrows(ArithmeticException::class.java) { MoneyMinor.parse("999999999999999999.99", "MXN") }
    }
}
