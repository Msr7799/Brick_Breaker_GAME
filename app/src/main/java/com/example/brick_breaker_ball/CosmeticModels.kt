package com.example.brick_breaker_ball

data class BallSpriteDefinition(
    val groupIndex: Int,
    val groupName: String,
    val index: Int,
    val name: String,
    val spriteName: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
) {
    val id: String get() = "$groupName:$spriteName"
}

data class BallSpriteGroup(
    val groupIndex: Int,
    val groupName: String,
    val sprites: List<BallSpriteDefinition>,
)

data class PaddleSpriteDefinition(
    val sourceAtlas: String,
    val spriteNumber: Int,
    val name: String,
    val displayNameEn: String,
    val displayNameAr: String,
    val groupId: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
) {
    val id: String get() = "$sourceAtlas:$name"
}

object CosmeticDefaults {
    const val BALL_GROUP = "monsters"
    const val BALL_SPRITE = "monsters_skull"
    const val PADDLE_ID = "group2:paddle_normal_titanium_edge"
    const val WEAPON_PADDLE_ID = "group2:paddle_weapon_dual_pulse_cannon"
    const val STICKY_PADDLE_ID = "group2:paddle_sticky_nano_gel"
}

object CosmeticPaddleSelection {
    fun idForState(selectedPaddleId: String, laserActive: Boolean, stickyActive: Boolean): String = when {
        laserActive -> CosmeticDefaults.WEAPON_PADDLE_ID
        stickyActive -> CosmeticDefaults.STICKY_PADDLE_ID
        else -> selectedPaddleId
    }
}

data class PaddleVisualSlot(val centerX: Float, val width: Float)

object PaddleVisualLayout {
    fun slots(centerX: Float, physicalWidth: Float, dual: Boolean, gap: Float = 6f): List<PaddleVisualSlot> {
        if (!dual) return listOf(PaddleVisualSlot(centerX, physicalWidth))
        val offset = (physicalWidth + gap) / 2f
        return listOf(PaddleVisualSlot(centerX - offset, physicalWidth), PaddleVisualSlot(centerX + offset, physicalWidth))
    }
}
