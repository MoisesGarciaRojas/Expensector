package com.danielgarcia.expensector.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.util.UUID

class BudgetPeriodGenerator {
    fun generateSemimonthly(
        configuration: BudgetPeriodConfiguration,
        from: LocalDate,
        through: LocalDate,
        nowEpochMillis: Long,
        today: LocalDate = LocalDate.now(),
    ): List<BudgetPeriod> {
        require(configuration.periodType == PeriodType.SEMIMONTHLY) { "Only semimonthly periods are implemented." }
        require(!through.isBefore(from)) { "End date must not be before start date." }
        val firstMonth = YearMonth.from(from.minusDays(15))
        val lastMonth = YearMonth.from(through)
        val periods = mutableListOf<BudgetPeriod>()
        var cursor = firstMonth
        while (!cursor.isAfter(lastMonth)) {
            periods += buildPeriod(configuration, cursor, firstHalf = true, nowEpochMillis, today)
            periods += buildPeriod(configuration, cursor, firstHalf = false, nowEpochMillis, today)
            cursor = cursor.plusMonths(1)
        }
        return periods
            .filter { !it.endDate.isBefore(from) && !it.startDate.isAfter(through) }
            .sortedBy { it.startDate }
    }

    fun adjustedPaymentDate(
        nominalDate: LocalDate,
        strategy: WeekendAdjustmentStrategy,
    ): LocalDate =
        when (strategy) {
            WeekendAdjustmentStrategy.PREVIOUS_FRIDAY -> when (nominalDate.dayOfWeek) {
                DayOfWeek.SATURDAY -> nominalDate.minusDays(1)
                DayOfWeek.SUNDAY -> nominalDate.minusDays(2)
                else -> nominalDate
            }
        }

    private fun buildPeriod(
        configuration: BudgetPeriodConfiguration,
        month: YearMonth,
        firstHalf: Boolean,
        nowEpochMillis: Long,
        today: LocalDate,
    ): BudgetPeriod {
        val startDay = if (firstHalf) 1 else 16
        val endDay = if (firstHalf) 15 else month.lengthOfMonth()
        val start = month.atDay(startDay)
        val end = month.atDay(endDay)
        val key = "${month.year}-${month.monthValue.toString().padStart(2, '0')}-${if (firstHalf) "A" else "B"}"
        return BudgetPeriod(
            id = UUID.nameUUIDFromBytes("${configuration.id}:$key".toByteArray()).toString(),
            financialSpaceId = configuration.financialSpaceId,
            configurationId = configuration.id,
            periodKey = key,
            periodType = PeriodType.SEMIMONTHLY,
            startDate = start,
            endDate = end,
            nominalExpectedPaymentDate = start,
            adjustedExpectedPaymentDate = adjustedPaymentDate(start, configuration.weekendAdjustmentStrategy),
            actualPaymentDate = null,
            status = when {
                today.isBefore(start) -> BudgetPeriodStatus.UPCOMING
                today.isAfter(end) -> BudgetPeriodStatus.CLOSED
                else -> BudgetPeriodStatus.CURRENT
            },
            createdAtEpochMillis = nowEpochMillis,
            updatedAtEpochMillis = nowEpochMillis,
        )
    }
}

fun openingPeriodWindow(startingDate: LocalDate): Pair<LocalDate, LocalDate> =
    startingDate.minusDays(20) to startingDate.plusMonths(3).plusDays(20)
