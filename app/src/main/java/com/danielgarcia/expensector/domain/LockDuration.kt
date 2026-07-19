package com.danielgarcia.expensector.domain

enum class LockDuration(val preferenceValue: String, val timeoutMillis: Long) {
    Immediately("immediately", 0L),
    OneMinute("one_minute", 60_000L),
    FiveMinutes("five_minutes", 5 * 60_000L),
    FifteenMinutes("fifteen_minutes", 15 * 60_000L);

    companion object {
        val Default = OneMinute

        fun fromPreferenceValue(value: String?): LockDuration =
            entries.firstOrNull { it.preferenceValue == value } ?: Default
    }
}
