package com.danielgarcia.expensector.core.common

private val EmailPattern = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")

fun isValidOptionalEmail(value: String): Boolean =
    value.isBlank() || EmailPattern.matches(value.trim())

fun isValidIsoCurrencyCode(value: String): Boolean =
    value.length == 3 && value.all { it.isUpperCase() }
