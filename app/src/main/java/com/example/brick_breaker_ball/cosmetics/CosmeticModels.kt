/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/cosmetics/CosmeticModels.kt
 * الوظيفة: نماذج الكرات المختارة وقيمها الافتراضية.
 * الحسابات: مطابقة اختيار الكرة ونطاق عرضها.
 */

package com.example.brick_breaker_ball.cosmetics

import com.example.brick_breaker_ball.campaign.GameSettings
import com.example.brick_breaker_ball.gameplay.GameplayMode

data class BallSpriteDefinition(
    val groupIndex: Int,
    val groupName: String,
    val index: Int,
    val name: String,
    val spriteName: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int
) {
    val id: String get() = "$groupName:$spriteName"
}

object CosmeticDefaults {
    const val BALL_GROUP = "individual"
    const val BALL_SPRITE = "38_chrome_mirror"
}

/** Volley customization stores its equipped ball separately from legacy preferences. */
object VolleyBallSelection {
    const val STARTER_SPRITE = "41_pearl_white"
    const val STARTER_GROUP = CosmeticDefaults.BALL_GROUP
    private var displaySource: List<BallSpriteDefinition>? = null
    private var displayOrder: List<BallSpriteDefinition> = emptyList()

    fun isStarter(ball: BallSpriteDefinition): Boolean = ball.groupName == STARTER_GROUP && ball.spriteName == STARTER_SPRITE

    fun selectionSprite(settings: GameSettings): String = if (settings.gameplayMode == GameplayMode.VOLLEY) {
        settings.selectedVolleyBallSpriteName
    } else {
        settings.selectedBallSpriteName
    }

    fun selectionGroup(settings: GameSettings): String = if (settings.gameplayMode == GameplayMode.VOLLEY) {
        settings.selectedVolleyBallGroupName
    } else {
        settings.selectedBallGroupName
    }

    fun isEquipped(settings: GameSettings, ball: BallSpriteDefinition): Boolean = selectionGroup(settings) == ball.groupName && selectionSprite(settings) == ball.spriteName

    fun canUse(mode: GameplayMode, ball: BallSpriteDefinition, normallyOwned: Boolean): Boolean = normallyOwned || (mode == GameplayMode.VOLLEY && isStarter(ball))

    fun inDisplayOrder(balls: List<BallSpriteDefinition>, mode: GameplayMode): List<BallSpriteDefinition> {
        if (mode != GameplayMode.VOLLEY) return balls
        if (displaySource !== balls) {
            val offers = balls.associateWith(BallCollectionCatalog::offer)
            val powers = balls.associateWith { BallCollectionCatalog.power(it.spriteName) }
            displayOrder = balls.sortedWith(compareBy<BallSpriteDefinition> { !isStarter(it) }
                .thenBy { offers.getValue(it).rarity.ordinal }
                .thenBy { powers.getValue(it).first }
                .thenBy { powers.getValue(it).second }
                .thenBy { offers.getValue(it).coins ?: offers.getValue(it).gems?.times(100) ?: 0 }
                .thenBy { it.index })
            displaySource = balls
        }
        return displayOrder
    }

    fun equip(settings: GameSettings, ball: BallSpriteDefinition) {
        if (settings.gameplayMode == GameplayMode.VOLLEY) {
            settings.selectedVolleyBallGroupName = ball.groupName
            settings.selectedVolleyBallSpriteName = ball.spriteName
        } else {
            settings.selectedBallGroupName = ball.groupName
            settings.selectedBallSpriteName = ball.spriteName
        }
    }
}
