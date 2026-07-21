package com.danielgarcia.expensector.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class CategoryNormalizationTest {
    @Test
    fun normalizesSpacingCaseAndAccents() {
        assertEquals("restaurantes", normalizeCatalogName("  RESTAURÁNTES  "))
        assertEquals("mobile telephone", normalizeCatalogName("Mobile    Telephone"))
    }
}
