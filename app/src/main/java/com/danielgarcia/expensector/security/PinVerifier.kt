package com.danielgarcia.expensector.security

data class PinVerifier(
    val algorithm: String,
    val iterations: Int,
    val saltBase64: String,
    val verifierBase64: String,
)
