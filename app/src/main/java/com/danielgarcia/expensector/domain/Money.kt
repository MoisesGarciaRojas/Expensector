package com.danielgarcia.expensector.domain

import java.math.BigDecimal
import java.math.RoundingMode

data class MoneyMinor(
    val minorUnits: Long,
    val currencyCode: String,
) {
    init {
        require(isValidCurrencyCode(currencyCode)) { "Currency must be an ISO 4217 code." }
    }

    fun format(): String {
        val amount = BigDecimal(minorUnits).movePointLeft(2).setScale(2, RoundingMode.UNNECESSARY)
        return "$amount $currencyCode"
    }

    companion object {
        fun parse(input: String, currencyCode: String): MoneyMinor {
            require(isValidCurrencyCode(currencyCode)) { "Currency must be an ISO 4217 code." }
            val normalized = input.trim().replace(",", "")
            require(normalized.isNotBlank()) { "Amount is required." }
            val decimal = normalized.toBigDecimalOrNull() ?: throw IllegalArgumentException("Invalid amount.")
            require(decimal >= BigDecimal.ZERO) { "Amount must not be negative." }
            val minor = decimal.movePointRight(2)
            require(minor.scale() <= 0) { "Amount must use no more than two decimal places." }
            return MoneyMinor(minor.longValueExact(), currencyCode)
        }
    }
}

fun isValidCurrencyCode(value: String): Boolean =
    value.length == 3 && value.all { it in 'A'..'Z' }
