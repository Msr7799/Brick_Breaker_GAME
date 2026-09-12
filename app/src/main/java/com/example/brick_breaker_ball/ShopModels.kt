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

enum class ShopProductId { RANDOM_5, RANDOM_50, FIVE_OF_EACH, TEN_OF_EACH }
sealed interface BoosterGrant {
    data class RandomTotal(val amount: Int) : BoosterGrant
    data class EachType(val amountPerType: Int) : BoosterGrant
}

data class ShopProduct(val id: ShopProductId, val storeId: String, val title: String, val subtitle: String, val grant: BoosterGrant)

data class ShopProductUiState(val enabled: Boolean, val buttonLabel: String)

fun shopProductUiState(formattedPrice: String?, busy: Boolean): ShopProductUiState = when {
    busy -> ShopProductUiState(false, "PLEASE WAIT…")
    formattedPrice == null -> ShopProductUiState(false, "CURRENTLY UNAVAILABLE")
    else -> ShopProductUiState(true, "BUY  $formattedPrice")
}

object ShopCatalog {
    /**
     * Production-facing catalog. New real-money purchases are deterministic so the player
     * always knows exactly what will be granted before Google Play opens.
     */
    val products = listOf(
        ShopProduct(
            ShopProductId.FIVE_OF_EACH,
            "booster_five_each_299",
            "FULL ARSENAL",
            "5 OF EACH POSITIVE ITEM • 70 TOTAL",
            BoosterGrant.EachType(5)
        ),
        ShopProduct(
            ShopProductId.TEN_OF_EACH,
            "booster_ten_each_599",
            "FORGE VAULT",
            "10 OF EACH POSITIVE ITEM • 140 TOTAL",
            BoosterGrant.EachType(10)
        )
    )

    /**
     * Old randomized SKUs are retained only for reconciliation/consumption of a purchase
     * created by an earlier build. They are deliberately hidden from the production Shop UI.
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
            ShopProductId.RANDOM_50,
            "booster_random_50_199",
            "LEGACY OVERDRIVE CRATE",
            "LEGACY RANDOM ITEM PACK",
            BoosterGrant.RandomTotal(50)
        )
    )

    /** All IDs Play Billing should know about, including hidden legacy consumables. */
    val billableProducts: List<ShopProduct> = products + legacyProducts

    fun findByStoreId(storeId: String): ShopProduct? = billableProducts.firstOrNull { it.storeId == storeId }
}

object BoosterGrantFactory {
    fun createGrant(product: ShopProduct, token: String): Map<PowerUpType, Int> = when (val grant = product.grant) {
        is BoosterGrant.EachType -> SHOP_ELIGIBLE_TYPES.associateWith { grant.amountPerType }

        is BoosterGrant.RandomTotal -> buildMap {
            // Kept only for reconciliation of legacy randomized SKUs.
            val random = Random(token.hashCode().toLong())
            repeat(grant.amount) {
                val type = SHOP_ELIGIBLE_TYPES[random.nextInt(SHOP_ELIGIBLE_TYPES.size)]
                put(type, (get(type) ?: 0) + 1)
            }
        }
    }

    fun reward(token: String): PowerUpType = SHOP_ELIGIBLE_TYPES[Math.floorMod(token.hashCode(), SHOP_ELIGIBLE_TYPES.size)]
}

data class DailyRewardState(val claimedToday: Int, val dailyLimit: Int = 5, val nextEligibleAtMillis: Long)
internal fun dayKey(now: Long): String = Calendar.getInstance().run {
    timeInMillis = now
    "${get(Calendar.YEAR)}-${get(Calendar.DAY_OF_YEAR)}"
}
