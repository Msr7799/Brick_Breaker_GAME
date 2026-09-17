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

    @Test fun fourFeaturedOffersHaveTheRequestedRewards() {
        assertEquals(4, ShopCatalog.products.size)
        assertEquals(ShopCatalog.billableProducts.size, ShopCatalog.billableProducts.map { it.storeId }.toSet().size)
        val expected = mapOf(
            ShopProductId.RANDOM_20 to 20,
            ShopProductId.RANDOM_50 to 50,
            ShopProductId.FIVE_OF_EACH to 70,
            ShopProductId.MIXED_150 to 150,
        )
        ShopCatalog.products.forEach { product ->
            val grant = BoosterGrantFactory.createGrant(product, "purchase-token")
            assertEquals(expected.getValue(product.id), grant.values.sum())
            assertTrue(grant.keys.all { it in SHOP_ELIGIBLE_TYPES })
            assertEquals(grant, BoosterGrantFactory.createGrant(product, "purchase-token"))
        }
        val vault = ShopCatalog.products.first { it.id == ShopProductId.MIXED_150 }
        val vaultGrant = BoosterGrantFactory.createGrant(vault, "purchase-token")
        assertTrue(SHOP_ELIGIBLE_TYPES.all { vaultGrant.getValue(it) >= 10 })
    }

    @Test fun legacyPurchaseIdsKeepTheirOriginalRewards() {
        assertTrue(ShopCatalog.legacyProducts.all { it !in ShopCatalog.products })
        assertEquals(5, BoosterGrantFactory.createGrant(ShopCatalog.findByStoreId("booster_random_5_099")!!, "old").values.sum())
        assertEquals(140, BoosterGrantFactory.createGrant(ShopCatalog.findByStoreId("booster_ten_each_599")!!, "old").values.sum())
        assertEquals(50, BoosterGrantFactory.createGrant(ShopCatalog.findByStoreId("booster_random_50_199")!!, "old").values.sum())
    }

    @Test fun targetPricesDoNotEnablePurchasesWithoutPlayDetails() {
        ShopCatalog.products.forEach { product ->
            assertFalse(product.targetUsdPrice.isNullOrBlank())
            assertFalse(shopProductUiState(null, false).enabled)
        }
    }
}
