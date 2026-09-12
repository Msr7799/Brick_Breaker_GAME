package com.example.brick_breaker_ball

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShopAvailabilityTest {
    @Test fun missingProductDetailsDoesNotCrash() {
        val state = shopProductUiState(null, false)
        assertFalse(state.enabled)
    }

    @Test fun missingProductDetailsShowsUnavailableState() {
        assertEquals("CURRENTLY UNAVAILABLE", shopProductUiState(null, false).buttonLabel)
    }

    @Test fun noPaidRandomProductsVisible() {
        assertTrue(ShopCatalog.products.all { it.grant is BoosterGrant.EachType })
        assertTrue(ShopCatalog.legacyProducts.all { it !in ShopCatalog.products })
    }
}
