/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/main/java/com/example/brick_breaker_ball/GameplayAtlas.kt
 * المؤلف: mohamed alromaihi
 * الدوال الموجودة: `ball`، `brick`، `preview`، `intactBrickId`، `powerUpIcon`
 */

package com.example.brick_breaker_ball

import com.badlogic.gdx.graphics.g2d.TextureRegion

data class PaddleSkin(val whole: TextureRegion)

class GameplayAtlas(private val sheet: DetailedSpriteSheet) {
    val paddleNormal = PaddleSkin(sheet.region(SpriteId.PADDLE_NORMAL))
    val paddleLaser = PaddleSkin(sheet.region(SpriteId.PADDLE_LASER))
    val paddleSticky = PaddleSkin(sheet.region(SpriteId.PADDLE_STICKY))
    val laserProjectile: TextureRegion = sheet.region(SpriteId.LASER_BULLET)
    val electricFloorWire: TextureRegion = sheet.region(SpriteId.ELECTRIC_FLOOR_WIRE)
    val sparkFrames: List<TextureRegion> = (1..8).map { index ->
        sheet.region(SpriteId.valueOf("SPARK_FRAME_${index.toString().padStart(2, '0')}"))
    }

    /** ملاحظة صيانة: الدالة `ball` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun ball(ball: Ball): TextureRegion {
        val ability = when {
            ball.collisionMode == BallCollisionMode.PIERCING -> "PIERCING"
            ball.abilityFireCharge || ball.element == BallElement.FIRE || ball.element == BallElement.EXPLOSIVE -> "FIRE"
            else -> "NORMAL"
        }
        val size = when (ball.size) {
            BallSize.SMALL -> "SMALL"
            BallSize.DEFAULT -> "MEDIUM"
            BallSize.MEGA, BallSize.LARGE -> "LARGE"
        }
        return sheet.region(SpriteId.valueOf("BALL_${ability}_$size"))
    }

    /** ملاحظة صيانة: الدالة `brick` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun brick(brick: Brick, world: Int, useCampaignPalette: Boolean = true, colorBlind: Boolean = false, levelId: Int = 0): TextureRegion {
        val intact = intactBrickId(brick.type, world, brick.id, useCampaignPalette, colorBlind, levelId)
        val sprite = if (brick.health < brick.initialHealth) sheet.nextState(intact) ?: intact else intact
        return sheet.region(sprite)
    }

    /** ملاحظة صيانة: الدالة `preview` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun preview(type: BrickType, world: Int): TextureRegion = sheet.region(intactBrickId(type, world, type.ordinal * 17 + 5, useCampaignPalette = false, colorBlind = false))

    /** ملاحظة صيانة: الدالة `intactBrickId` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun intactBrickId(type: BrickType, world: Int, id: Int, useCampaignPalette: Boolean, colorBlind: Boolean, levelId: Int = 0): SpriteId {
        if (useCampaignPalette) CampaignBrickPalette.spriteFor(type, world, id, colorBlind, levelId)?.let { return it }
        return when (type) {
            BrickType.ROUGH_STONE -> SpriteId.BRICK_ROUGH_STONE

            BrickType.LIGHTNING_SPEED_PASS_THROUGH -> SpriteId.BRICK_LIGHTNING_SPEED_PASS_THROUGH

            BrickType.ELECTRIC_WHITE -> SpriteId.BRICK_ELECTRIC_WHITE_INTACT

            BrickType.CRYSTAL_BLUE -> SpriteId.BRICK_CRYSTAL_BLUE_INTACT

            BrickType.CRYSTAL_RED -> SpriteId.BRICK_CRYSTAL_RED_INTACT

            BrickType.CRYSTAL_PURPLE -> SpriteId.BRICK_CRYSTAL_PURPLE_INTACT

            BrickType.STONE_GRAY -> SpriteId.BRICK_STONE_GRAY_INTACT

            BrickType.BLACK_HOLE_TELEPORTER -> SpriteId.BRICK_BLACK_HOLE_TELEPORTER

            BrickType.RANDOM_INVENTORY_POWERUP -> SpriteId.BRICK_RANDOM_INVENTORY_POWERUP

            BrickType.CRYSTAL_MAROON -> SpriteId.BRICK_CRYSTAL_MAROON_INTACT

            BrickType.ARMORED_DARK -> SpriteId.BRICK_ARMORED_DARK_INTACT

            BrickType.CRYSTAL_PINK -> SpriteId.BRICK_CRYSTAL_PINK_INTACT

            BrickType.CRYSTAL_ORANGE -> SpriteId.BRICK_CRYSTAL_ORANGE_INTACT

            BrickType.CRYSTAL_GREEN -> SpriteId.BRICK_CRYSTAL_GREEN_INTACT

            BrickType.SPIKED_HAZARD -> SpriteId.BRICK_SPIKED_HAZARD

            BrickType.TRANSPARENT_SLOW_PASS_THROUGH -> SpriteId.BRICK_TRANSPARENT_SLOW_PASS_THROUGH

            BrickType.INDESTRUCTIBLE -> SpriteId.BRICK_STEEL_UNBREAKABLE

            BrickType.POWERUP_CARRIER -> SpriteId.BRICK_RANDOM_INVENTORY_POWERUP

            BrickType.EXPLOSIVE -> SpriteId.BRICK_BASIC_ONE_HIT

            BrickType.REGENERATING -> SpriteId.BRICK_CRYSTAL_GREEN_INTACT

            BrickType.GHOST -> SpriteId.BRICK_BASIC_ONE_HIT

            BrickType.MOVING_HORIZONTAL -> SpriteId.BRICK_BASIC_ONE_HIT

            BrickType.MOVING_VERTICAL -> SpriteId.BRICK_BASIC_ONE_HIT

            BrickType.SWITCH -> SpriteId.BRICK_BASIC_ONE_HIT

            BrickType.LOCKED, BrickType.BOSS_CORE -> SpriteId.BRICK_ARMORED_DARK_INTACT

            BrickType.KEY_BRICK -> SpriteId.BRICK_BASIC_ONE_HIT

            BrickType.CHAIN_BRICK -> SpriteId.BRICK_BASIC_ONE_HIT

            BrickType.ARMORED_TWO_HIT -> SpriteId.BRICK_STONE_GRAY_INTACT

            BrickType.ARMORED_THREE_HIT -> SpriteId.BRICK_ARMORED_DARK_INTACT

            BrickType.GLASS -> SpriteId.BRICK_BASIC_ONE_HIT

            BrickType.NORMAL_ONE_HIT -> SpriteId.BRICK_BASIC_ONE_HIT
        }
    }

    /** ملاحظة صيانة: الدالة `powerUpIcon` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun powerUpIcon(type: PowerUpType): TextureRegion {
        val id = requireNotNull(SpriteId.from(PowerUpCatalog.definitions.getValue(type).icon))
        return sheet.region(id)
    }
}
