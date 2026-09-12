/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/main/java/com/example/brick_breaker_ball/CosmeticModels.kt
 * المؤلف: mohamed alromaihi
 * الدوال الموجودة: `idForState`، `slots`
 */

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
    val height: Int
) {
    val id: String get() = "$groupName:$spriteName"
}

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
    val height: Int
) {
    val id: String get() = "$sourceAtlas:$name"
}

object CosmeticDefaults {
    const val BALL_GROUP = "individual"
    const val BALL_SPRITE = "38_chrome_mirror"
    const val PADDLE_ID = "group1:paddle_normal_titanium_edge"
    const val WEAPON_PADDLE_ID = "group1:paddle_weapon_dual_pulse_cannon"
    const val STICKY_PADDLE_ID = "group1:paddle_sticky_nano_gel"
}

object CosmeticPaddleSelection {
    /** ملاحظة صيانة: الدالة `idForState` تحوّل البيانات أو تبني المعرّف المتوافق مع بقية النظام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun idForState(
        selectedNormalPaddleId: String,
        selectedWeaponPaddleId: String,
        selectedStickyPaddleId: String,
        laserActive: Boolean,
        stickyActive: Boolean
    ): String = when {
        laserActive -> selectedWeaponPaddleId.ifBlank { CosmeticDefaults.WEAPON_PADDLE_ID }
        stickyActive -> selectedStickyPaddleId.ifBlank { CosmeticDefaults.STICKY_PADDLE_ID }
        else -> selectedNormalPaddleId.ifBlank { CosmeticDefaults.PADDLE_ID }
    }
}

data class PaddleVisualSlot(val centerX: Float, val width: Float)

object PaddleVisualLayout {
    /** ملاحظة صيانة: الدالة `slots` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun slots(centerX: Float, physicalWidth: Float, dual: Boolean, gap: Float = 6f): List<PaddleVisualSlot> {
        if (!dual) return listOf(PaddleVisualSlot(centerX, physicalWidth))
        val offset = (physicalWidth + gap) / 2f
        return listOf(PaddleVisualSlot(centerX - offset, physicalWidth), PaddleVisualSlot(centerX + offset, physicalWidth))
    }
}

/** One visual family across the three paddle gameplay states. */
data class PaddleStyleSet(
    val id: String,
    val displayName: String,
    val normal: PaddleSpriteDefinition,
    val weapon: PaddleSpriteDefinition,
    val sticky: PaddleSpriteDefinition
) {
    fun contains(paddleId: String): Boolean = paddleId == normal.id || paddleId == weapon.id || paddleId == sticky.id
}

/** Builds every discovered paddle sheet into validated Normal/Weapon/Sticky families. */
object PaddleStyleCatalog {
    fun build(paddles: List<PaddleSpriteDefinition>): List<PaddleStyleSet> = paddles
        .groupBy { it.sourceAtlas }
        .flatMap { (sourceAtlas, definitions) ->
            require(definitions.size % 3 == 0) { "$sourceAtlas paddle catalog must contain complete style trios" }
            definitions.chunked(3).map { trio ->
                require(trio.size == 3) { "$sourceAtlas paddle style is incomplete" }
                val normal =
                    requireNotNull(
                        trio.singleOrNull {
                            it.groupId.equals("normal", true)
                        }
                    ) { "Missing normal paddle in $sourceAtlas style trio" }
                val weapon =
                    requireNotNull(
                        trio.singleOrNull {
                            it.groupId.equals("weapon", true)
                        }
                    ) { "Missing weapon paddle in $sourceAtlas style trio" }
                val sticky =
                    requireNotNull(
                        trio.singleOrNull {
                            it.groupId.equals("sticky", true)
                        }
                    ) { "Missing sticky paddle in $sourceAtlas style trio" }
                PaddleStyleSet(
                    id = "$sourceAtlas:style:${normal.name.removePrefix("paddle_normal_")}",
                    displayName = normal.displayNameEn,
                    normal = normal,
                    weapon = weapon,
                    sticky = sticky
                )
            }
        }

    fun styleForPaddle(styles: List<PaddleStyleSet>, paddleId: String): PaddleStyleSet? = styles.firstOrNull { it.contains(paddleId) }
}
