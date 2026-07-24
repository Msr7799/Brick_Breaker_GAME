package com.example.brick_breaker_ball

import java.util.Calendar
import java.util.Random

val SHOP_ELIGIBLE_TYPES = listOf(
    PowerUpType.LASER_AUTO_CHARGE, PowerUpType.EXTRA_LIFE, PowerUpType.EXPAND_PADDLE,
    PowerUpType.TIMED_BOMB_BRICKS, PowerUpType.RANDOM_GOOD, PowerUpType.FIRE_BALL,
    PowerUpType.MAGNETIC_PADDLE, PowerUpType.SLOW_BALL, PowerUpType.MULTIBALL_PLUS_4,
    PowerUpType.DUAL_PADDLE, PowerUpType.MEGA_BALL, PowerUpType.STICKY_PADDLE,
    PowerUpType.ONE_HIT_ANY_BRICK, PowerUpType.MULTIBALL_15,
)

enum class ShopProductId { RANDOM_5, RANDOM_50, FIVE_OF_EACH, TEN_OF_EACH }
sealed interface BoosterGrant { data class RandomTotal(val amount:Int):BoosterGrant; data class EachType(val amountPerType:Int):BoosterGrant }
data class ShopProduct(val id:ShopProductId,val storeId:String,val title:String,val subtitle:String,val displayPrice:String,val grant:BoosterGrant)
object ShopCatalog { val products=listOf(
    ShopProduct(ShopProductId.RANDOM_5,"booster_random_5_099","5 RANDOM BOOSTERS","5 BOOSTERS TOTAL","$0.99",BoosterGrant.RandomTotal(5)),
    ShopProduct(ShopProductId.RANDOM_50,"booster_random_50_199","50 RANDOM BOOSTERS","50 BOOSTERS TOTAL","$1.99",BoosterGrant.RandomTotal(50)),
    ShopProduct(ShopProductId.FIVE_OF_EACH,"booster_five_each_299","5 OF EVERY BOOSTER","70 TOTAL - 5 OF EACH","$2.99",BoosterGrant.EachType(5)),
    ShopProduct(ShopProductId.TEN_OF_EACH,"booster_ten_each_599","10 OF EVERY BOOSTER","140 TOTAL - 10 OF EACH","$5.99",BoosterGrant.EachType(10)),
) }
object BoosterGrantFactory {
    fun createGrant(product:ShopProduct,token:String):Map<PowerUpType,Int> = when(val grant=product.grant){
        is BoosterGrant.EachType -> SHOP_ELIGIBLE_TYPES.associateWith{grant.amountPerType}
        is BoosterGrant.RandomTotal -> buildMap { val random=Random(token.hashCode().toLong()); repeat(grant.amount){val type=SHOP_ELIGIBLE_TYPES[random.nextInt(SHOP_ELIGIBLE_TYPES.size)];put(type,(get(type)?:0)+1)} }
    }
    fun reward(token:String)=SHOP_ELIGIBLE_TYPES[Math.floorMod(token.hashCode(),SHOP_ELIGIBLE_TYPES.size)]
}

data class DailyRewardState(val claimedToday:Int,val dailyLimit:Int=5,val nextEligibleAtMillis:Long)
internal fun dayKey(now:Long):String=Calendar.getInstance().run{timeInMillis=now;"${get(Calendar.YEAR)}-${get(Calendar.DAY_OF_YEAR)}"}
