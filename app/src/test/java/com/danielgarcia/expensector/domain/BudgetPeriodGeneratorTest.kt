package com.danielgarcia.expensector.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BudgetPeriodGeneratorTest {
    private val configuration = BudgetPeriodConfiguration(
        id = "config",
        financialSpaceId = "space",
        periodType = PeriodType.SEMIMONTHLY,
        effectiveFrom = LocalDate.parse("2026-07-21"),
        effectiveTo = null,
        weekendAdjustmentStrategy = WeekendAdjustmentStrategy.PREVIOUS_FRIDAY,
        active = true,
        createdAtEpochMillis = 1L,
        updatedAtEpochMillis = 1L,
    )

    @Test
    fun generatesMonthEndsAndLeapYear() {
        val periods = BudgetPeriodGenerator().generateSemimonthly(
            configuration,
            LocalDate.parse("2028-02-01"),
            LocalDate.parse("2028-03-31"),
            nowEpochMillis = 1L,
            today = LocalDate.parse("2028-02-02"),
        )

        assertEquals(LocalDate.parse("2028-02-29"), periods.first { it.periodKey == "2028-02-B" }.endDate)
        assertEquals(LocalDate.parse("2028-03-31"), periods.first { it.periodKey == "2028-03-B" }.endDate)
    }

    @Test
    fun adjustsWeekendPaymentDateWithoutMovingBoundaries() {
        val period = BudgetPeriodGenerator().generateSemimonthly(
            configuration,
            LocalDate.parse("2026-08-01"),
            LocalDate.parse("2026-08-16"),
            nowEpochMillis = 1L,
            today = LocalDate.parse("2026-07-21"),
        ).first { it.periodKey == "2026-08-A" }

        assertEquals(LocalDate.parse("2026-08-01"), period.startDate)
        assertEquals(LocalDate.parse("2026-08-15"), period.endDate)
        assertEquals(LocalDate.parse("2026-08-01"), period.nominalExpectedPaymentDate)
        assertEquals(LocalDate.parse("2026-07-31"), period.adjustedExpectedPaymentDate)
    }

    @Test
    fun deterministicKeysPreventDuplicatePeriods() {
        val first = BudgetPeriodGenerator().generateSemimonthly(configuration, LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-31"), 1L)
        val second = BudgetPeriodGenerator().generateSemimonthly(configuration, LocalDate.parse("2026-07-01"), LocalDate.parse("2026-07-31"), 2L)

        assertEquals(first.map { it.id }, second.map { it.id })
        assertTrue(first.zipWithNext().all { (a, b) -> a.endDate.isBefore(b.startDate) })
    }
}
