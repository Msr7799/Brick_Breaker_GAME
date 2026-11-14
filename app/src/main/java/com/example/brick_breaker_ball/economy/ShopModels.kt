/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/economy/ShopModels.kt
 * الوظيفة: تعريف عروض المتجر وأصنافها.
 * الحسابات: مطابقة المنتج بسعره ونوع مكافأته.
 */

/*
 * Production shop catalog.
 * Positive gameplay Items only; hazards/special drops are never sold as positive Items.
 */
package com.example.brick_breaker_ball.economy

import com.example.brick_breaker_ball.cosmetics.BallCollectionCatalog
import com.example.brick_breaker_ball.cosmetics.NewBallNames
import com.example.brick_breaker_ball.gameplay.PowerUpType
import java.util.Calendar
import java.util.Random

val SHOP_ELIGIBLE_TYPES = listOf(
    PowerUpType.LASER_AUTO_CHARGE, PowerUpType.EXTRA_LIFE, PowerUpType.EXPAND_PADDLE,
    PowerUpType.TIMED_BOMB_BRICKS, PowerUpType.RANDOM_GOOD, PowerUpType.FIRE_BALL,
    PowerUpType.MAGNETIC_PADDLE, PowerUpType.SLOW_BALL, PowerUpType.MULTIBALL_PLUS_4,
    PowerUpType.DUAL_PADDLE, PowerUpType.MEGA_BALL, PowerUpType.STICKY_PADDLE,
    PowerUpType.ONE_HIT_ANY_BRICK, PowerUpType.MULTIBALL_15
)

enum class ShopProductId {
    GEM_80,
    GEM_200,
    GEM_400,
    GEM_850,
    RANDOM_5,
    RANDOM_20,
    RANDOM_50,
    FIVE_OF_EACH,
    TEN_OF_EACH,
    MIXED_150,
    EPIC_BALL_004,
    EPIC_BALL_005,
    EPIC_BALL_006,
    EPIC_BALL_013,
    EPIC_BALL_014,
    EPIC_BALL_057
}

/** Retained only so users who bought an older item bundle receive its original contents. */
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
    val grant: BoosterGrant? = null,
    val targetUsdPrice: String? = null,
    /** A fixed, fully disclosed gem reward for NEW in-app purchases. Never a random drop. */
    val gemAmount: Int = 0,
    /** Non-consumable cosmetic entitlement; ownership survives reinstall through Play restore. */
    val ballSpriteName: String? = null
)

data class ShopProductUiState(val enabled: Boolean, val buttonLabel: String)

fun shopProductUiState(formattedPrice: String?, busy: Boolean): ShopProductUiState = when {
    busy -> ShopProductUiState(false, "PLEASE WAIT…")
    formattedPrice == null -> ShopProductUiState(false, "CURRENTLY UNAVAILABLE")
    else -> ShopProductUiState(true, "BUY  $formattedPrice")
}

object ShopCatalog {
    /** New Play Console one-time consumable INAPP product IDs. Prices come from Google Play. */
    val products = listOf(
        ShopProduct(ShopProductId.GEM_80, "gems_80_099", "GEM POUCH", "80 GEMS", targetUsdPrice = "\$0.99", gemAmount = 80),
        ShopProduct(ShopProductId.GEM_200, "gems_200_199", "GEM CHEST", "200 GEMS", targetUsdPrice = "\$1.99", gemAmount = 200),
        ShopProduct(ShopProductId.GEM_400, "gems_400_299", "GEM TREASURY", "400 GEMS", targetUsdPrice = "\$2.99", gemAmount = 400),
        ShopProduct(ShopProductId.GEM_850, "gems_850_499", "GEM VAULT", "850 GEMS", targetUsdPrice = "\$4.99", gemAmount = 850)
    )

    /** Configure these six product IDs in Play Console at the intended USD 2.99 base price. */
    val epicBallProducts: List<ShopProduct> = BallCollectionCatalog.epicFrames.sorted().map { frame ->
        val sprite = "new_ball_${frame.toString().padStart(3, '0')}"
        ShopProduct(
            id = ShopProductId.valueOf("EPIC_BALL_${frame.toString().padStart(3, '0')}"),
            storeId = BallCollectionCatalog.offer(sprite).playProductId!!,
            title = NewBallNames.name(frame),
            subtitle = "PERMANENT EPIC BALL",
            targetUsdPrice = "\$2.99",
            ballSpriteName = sprite
        )
    }

    /** Previously sold SKUs: read/consume and honor previous grants, never advertise again. */
    val legacyProducts = listOf(
        ShopProduct(ShopProductId.RANDOM_5, "booster_random_5_099", "LEGACY SPARK PACK", "LEGACY RANDOM ITEM PACK", BoosterGrant.RandomTotal(5)),
        ShopProduct(ShopProductId.RANDOM_20, "booster_random_20_099", "LEGACY SPARK PACK", "20 RANDOM POSITIVE ITEMS", BoosterGrant.RandomTotal(20)),
        ShopProduct(ShopProductId.RANDOM_50, "booster_random_50_199", "LEGACY POWER CRATE", "50 RANDOM POSITIVE ITEMS", BoosterGrant.RandomTotal(50)),
        ShopProduct(ShopProductId.FIVE_OF_EACH, "booster_five_each_299", "LEGACY ARSENAL", "5 OF EACH POSITIVE ITEM", BoosterGrant.EachType(5)),
        ShopProduct(ShopProductId.MIXED_150, "booster_mixed_150_499", "LEGACY VAULT", "10 EACH + 10 RANDOM", BoosterGrant.EachTypePlusRandom(10, 10)),
        ShopProduct(ShopProductId.TEN_OF_EACH, "booster_ten_each_599", "LEGACY FORGE VAULT", "10 OF EACH POSITIVE ITEM", BoosterGrant.EachType(10))
    )

    val purchasableProducts: List<ShopProduct> = products + epicBallProducts
    val billableProducts: List<ShopProduct> = purchasableProducts + legacyProducts
    fun epicProductFor(spriteName: String): ShopProduct? = epicBallProducts.firstOrNull { it.ballSpriteName == spriteName }
    fun findByStoreId(storeId: String): ShopProduct? = billableProducts.firstOrNull { it.storeId == storeId }
}

object BoosterGrantFactory {
    fun createGrant(product: ShopProduct, token: String): Map<PowerUpType, Int> = when (val grant = product.grant) {
        null -> emptyMap()

        // New paid SKUs credit Gems instead, not Items.
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
