/*
 * Production shop catalog.
 * Positive gameplay Items only; hazards/special drops are never sold as positive Items.
 */
package com.example.brick_breaker_ball

import java.util.Calendar
import java.util.Random

val SHOP_ELIGIBLE_TYPES = listOf(
    PowerUpType.LASER_AUTO_CHARGE, PowerUpType.EXTRA_LIFE, PowerUpType.EXPAND_PADDLE,
    PowerUpType.TIMED_BOMB_BRICKS, PowerUpType.RANDOM_GOOD, PowerUpType.FIRE_BALL,
    PowerUpType.MAGNETIC_PADDLE, PowerUpType.SLOW_BALL, PowerUpType.MULTIBALL_PLUS_4,
    PowerUpType.DUAL_PADDLE, PowerUpType.MEGA_BALL, PowerUpType.STICKY_PADDLE,
    PowerUpType.ONE_HIT_ANY_BRICK, PowerUpType.MULTIBALL_15
)

enum class ShopProductId { RANDOM_5, RANDOM_20, RANDOM_50, FIVE_OF_EACH, TEN_OF_EACH, MIXED_150 }
sealed interface BoosterGrant {
    data class RandomTotal(val amount: Int) : BoosterGrant
    data class EachType(val amountPerType: Int) : BoosterGrant
    data class EachTypePlusRandom(val amountPerType: Int, val randomAmount: Int) : BoosterGrant
}

data class ShopProduct(
    val id: ShopProductId,
    val storeId: String,
    val title: String,
    val subtitle: String,
    val grant: BoosterGrant,
    val targetUsdPrice: String? = null,
)

data class ShopProductUiState(val enabled: Boolean, val buttonLabel: String)

fun shopProductUiState(formattedPrice: String?, busy: Boolean): ShopProductUiState = when {
    busy -> ShopProductUiState(false, "PLEASE WAIT…")
    formattedPrice == null -> ShopProductUiState(false, "CURRENTLY UNAVAILABLE")
    else -> ShopProductUiState(true, "BUY  $formattedPrice")
}

object ShopCatalog {
    /** Target USD prices are display guidance until Play returns the actual localized price. */
    val products = listOf(
        ShopProduct(
            ShopProductId.RANDOM_20,
            "booster_random_20_099",
            "SPARK PACK",
            "20 RANDOM POSITIVE ITEMS",
            BoosterGrant.RandomTotal(20),
            "\$0.99",
        ),
        ShopProduct(
            ShopProductId.RANDOM_50,
            "booster_random_50_199",
            "POWER CRATE",
            "50 RANDOM POSITIVE ITEMS",
            BoosterGrant.RandomTotal(50),
            "\$1.99",
        ),
        ShopProduct(
            ShopProductId.FIVE_OF_EACH,
            "booster_five_each_299",
            "FULL ARSENAL",
            "5 OF EACH POSITIVE ITEM • 70 TOTAL",
            BoosterGrant.EachType(5),
            "\$2.99",
        ),
        ShopProduct(
            ShopProductId.MIXED_150,
            "booster_mixed_150_499",
            "FORGE VAULT",
            "10 OF EACH + 10 RANDOM • 150 TOTAL",
            BoosterGrant.EachTypePlusRandom(10, 10),
            "\$4.99",
        )
    )

    /**
     * Old SKUs remain available for reconciliation and consumption at their original reward.
     */
    val legacyProducts = listOf(
        ShopProduct(
            ShopProductId.RANDOM_5,
            "booster_random_5_099",
            "LEGACY SPARK PACK",
            "LEGACY RANDOM ITEM PACK",
            BoosterGrant.RandomTotal(5)
        ),
        ShopProduct(
            ShopProductId.TEN_OF_EACH,
            "booster_ten_each_599",
            "LEGACY FORGE VAULT",
            "10 OF EACH POSITIVE ITEM • 140 TOTAL",
            BoosterGrant.EachType(10)
        )
    )

    /** All IDs Play Billing should know about, including hidden legacy consumables. */
    val billableProducts: List<ShopProduct> = products + legacyProducts

    fun findByStoreId(storeId: String): ShopProduct? = billableProducts.firstOrNull { it.storeId == storeId }
}

object BoosterGrantFactory {
    fun createGrant(product: ShopProduct, token: String): Map<PowerUpType, Int> = when (val grant = product.grant) {
        is BoosterGrant.EachType -> SHOP_ELIGIBLE_TYPES.associateWith { grant.amountPerType }
        is BoosterGrant.RandomTotal -> randomGrant(grant.amount, token)
        is BoosterGrant.EachTypePlusRandom -> buildMap {
            SHOP_ELIGIBLE_TYPES.forEach { put(it, grant.amountPerType) }
            randomGrant(grant.randomAmount, token).forEach { (type, count) ->
                put(type, getValue(type) + count)
            }
        }
    }

    private fun randomGrant(amount: Int, token: String): Map<PowerUpType, Int> = buildMap {
        val random = Random(token.hashCode().toLong())
        repeat(amount) {
            val type = SHOP_ELIGIBLE_TYPES[random.nextInt(SHOP_ELIGIBLE_TYPES.size)]
            put(type, (get(type) ?: 0) + 1)
        }
    }

    fun reward(token: String): PowerUpType = SHOP_ELIGIBLE_TYPES[Math.floorMod(token.hashCode(), SHOP_ELIGIBLE_TYPES.size)]
}

data class DailyRewardState(val claimedToday: Int, val dailyLimit: Int = 5, val nextEligibleAtMillis: Long)
internal fun dayKey(now: Long): String = Calendar.getInstance().run {
    timeInMillis = now
    "${get(Calendar.YEAR)}-${get(Calendar.DAY_OF_YEAR)}"
}
