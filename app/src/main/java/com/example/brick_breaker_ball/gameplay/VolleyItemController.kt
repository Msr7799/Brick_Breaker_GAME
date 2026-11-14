/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/gameplay/VolleyItemController.kt
 * الوظيفة: تطبيق عناصر Volley ووصفها للاعب.
 * الحسابات: ربط العنصر بتأثيره وحدود تفعيله.
 */

package com.example.brick_breaker_ball.gameplay

import com.example.brick_breaker_ball.economy.BoosterInventoryStore
import com.example.brick_breaker_ball.economy.SHOP_ELIGIBLE_TYPES

data class VolleyItemCheck(val usable: Boolean, val reason: String = "")

/** Visible inventory icon for a volley modifier; challenge-only modifiers have no item icon. */
fun VolleyModifier.itemType(): PowerUpType? = when (this) {
    VolleyModifier.FIRE -> PowerUpType.FIRE_BALL
    VolleyModifier.MEGA -> PowerUpType.MEGA_BALL
    VolleyModifier.PIERCING -> PowerUpType.ONE_HIT_ANY_BRICK
    VolleyModifier.SLOW -> PowerUpType.SLOW_BALL
    VolleyModifier.DUAL_LAUNCH -> PowerUpType.DUAL_PADDLE
    VolleyModifier.WIDE_LAUNCH -> PowerUpType.EXPAND_PADDLE
    VolleyModifier.MAGNETIC_RETURN -> PowerUpType.MAGNETIC_PADDLE
    VolleyModifier.STICKY_RETURN -> PowerUpType.STICKY_PADDLE
    VolleyModifier.LASER -> PowerUpType.LASER_AUTO_CHARGE
    VolleyModifier.TIMED_BOMBS -> PowerUpType.TIMED_BOMB_BRICKS
    else -> null
}

sealed interface VolleyItemUseResult {
    data class Applied(val requested: PowerUpType, val applied: PowerUpType) : VolleyItemUseResult
    data class Rejected(val reason: String) : VolleyItemUseResult
}

/** Atomic bridge between the canonical Items store and VolleySession. */
class VolleyItemController(
    private val session: VolleySession,
    private val inventory: BoosterInventoryStore
) {
    fun canUseVolleyItem(type: PowerUpType): VolleyItemCheck {
        if (type !in SHOP_ELIGIBLE_TYPES) return VolleyItemCheck(false, "NOT A POSITIVE ITEM")
        if (inventory.count(type) <= 0) return VolleyItemCheck(false, "NO ITEMS OWNED")
        if (type == PowerUpType.RANDOM_GOOD && applicableRandomPool().isEmpty()) {
            return VolleyItemCheck(false, "NO USEFUL POSITIVE EFFECT")
        }
        val reason = session.canArmCharm(type)
        return if (reason == null) VolleyItemCheck(true) else VolleyItemCheck(false, reason)
    }

    /** Consume first, then roll back the inventory unit if the effect cannot be armed. */
    fun tryUseVolleyItem(type: PowerUpType): VolleyItemUseResult {
        val check = canUseVolleyItem(type)
        if (!check.usable) return VolleyItemUseResult.Rejected(check.reason)

        val appliedType = if (type == PowerUpType.RANDOM_GOOD) chooseDeterministicRandom() else type
        if (!inventory.consume(type)) return VolleyItemUseResult.Rejected("ITEM COUNT CHANGED")
        if (!session.armCharm(appliedType, inventorySource = type)) {
            inventory.refund(type)
            return VolleyItemUseResult.Rejected("EFFECT COULD NOT START")
        }
        return VolleyItemUseResult.Applied(type, appliedType)
    }

    fun disarmQueuedItem(modifier: VolleyModifier): Boolean {
        val originalItem = session.disarmQueuedCharm(modifier) ?: return false
        inventory.refund(originalItem)
        return true
    }

    private fun applicableRandomPool(): List<PowerUpType> = SHOP_ELIGIBLE_TYPES
        .asSequence()
        .filter { it != PowerUpType.RANDOM_GOOD }
        .filter { session.canArmCharm(it) == null }
        .sortedBy { it.name }
        .toList()

    private fun chooseDeterministicRandom(): PowerUpType {
        val pool = applicableRandomPool()
        val salt = session.stage.levelId * 131 + session.turn * 31 + session.score
        return pool[Math.floorMod(salt, pool.size)]
    }
}

// أوصاف Volley مستقلة عن قاموس Classic الذي بقي لاختبارات المحاكاة فقط.
data class VolleyItemInfo(val fullName: String, val shortName: String, val description: String)

object VolleyItemDescriptions {
    fun info(type: PowerUpType): VolleyItemInfo {
        val names = when (type) {
            PowerUpType.LASER_AUTO_CHARGE -> "LASER AUTO CHARGE" to "AUTO LASER"
            PowerUpType.TIMED_BOMB_BRICKS -> "TIMED BOMB BRICKS" to "TIMED BOMBS"
            PowerUpType.MULTIBALL_PLUS_4 -> "+4 BALLS" to "+4 BALLS"
            PowerUpType.ONE_HIT_ANY_BRICK -> "ONE-HIT BALL" to "ONE HIT"
            PowerUpType.INSTANT_KILL_BALL -> "BALL-KILLING SPIKES" to "BALL KILL"
            PowerUpType.EXPAND_PADDLE -> "TRIPLE-LANE VOLLEY" to "TRIPLE LANE"
            PowerUpType.MAGNETIC_PADDLE -> "MAGNETIC RETURN" to "MAGNETIC"
            PowerUpType.DUAL_PADDLE -> "DUAL LAUNCH" to "DUAL LAUNCH"
            PowerUpType.STICKY_PADDLE -> "ANCHORED LAUNCH" to "ANCHOR"
            PowerUpType.KILL_PADDLE -> "BALL CURSE" to "BALL CURSE"
            PowerUpType.SHRINK_PADDLE -> "NARROW AIM" to "NARROW AIM"
            PowerUpType.MULTIBALL_15 -> "BALL STORM" to "BALL STORM"
            else -> null
        }
        val defaultName = type.name.replace('_', ' ')
        return VolleyItemInfo(
            fullName = names?.first ?: defaultName,
            shortName = names?.second ?: defaultName,
            description = description(type)
        )
    }

    fun description(type: PowerUpType): String = when (type) {
        PowerUpType.LASER_AUTO_CHARGE -> "Charges every 8 hits, then blasts the full target column."
        PowerUpType.EXTRA_LIFE -> "Adds one Rescue Ward, up to three."
        PowerUpType.EXPAND_PADDLE -> "Launches the next volley in a wide three-lane spread."
        PowerUpType.TIMED_BOMB_BRICKS -> "The first eight targets hit next volley trigger a 3x3 blast."
        PowerUpType.RANDOM_GOOD -> "Chooses one useful positive Volley Item."
        PowerUpType.FIRE_BALL -> "Adds 1 damage splash around direct hits next volley."
        PowerUpType.MAGNETIC_PADDLE -> "Strongly pulls the next launch position toward the center."
        PowerUpType.SLOW_BALL -> "Slows ball speed by 30% for a more controlled next volley."
        PowerUpType.MULTIBALL_PLUS_4 -> "Adds four permanent balls to this attempt."
        PowerUpType.DUAL_PADDLE -> "Alternates the next volley between two launch points."
        PowerUpType.MEGA_BALL -> "Increases radius and direct-hit damage for the next volley."
        PowerUpType.STICKY_PADDLE -> "Keeps the current launch position after the next return."
        PowerUpType.ONE_HIT_ANY_BRICK -> "Breaks eligible bricks in one hit; excludes steel, locks and bosses."
        PowerUpType.MULTIBALL_15 -> "Unleashes thirty permanent balls for a massive Volley barrage."
        PowerUpType.KILL_PADDLE -> "Removes one pending or active ball; does not reduce permanent ball count."
        PowerUpType.SHRINK_BALL -> "Shrinks the volley balls without changing their damage."
        PowerUpType.SHRINK_PADDLE -> "Reduces aim-guide forgiveness or launch spread for the next volley."
        PowerUpType.FAST_BALL -> "Speeds up the next volley without changing damage."
        PowerUpType.INSTANT_KILL_BALL -> "Removes one ball from the current volley only."
        PowerUpType.GHOST_BALL -> "Passes through bricks without damage or rewards; arena boundaries still apply."
        else -> "Activates ${type.name.replace('_', ' ').lowercase()} according to its gameplay rule."
    }
}
