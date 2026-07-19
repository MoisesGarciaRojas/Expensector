package com.danielgarcia.expensector.security

object PinPolicy {
    private val SixDigits = Regex("^\\d{6}$")

    fun isValid(pin: String): Boolean = SixDigits.matches(pin)
}
