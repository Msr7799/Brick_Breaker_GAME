/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/assets/SpriteId.kt
 * الوظيفة: معرّفات صور الطوب المستخدمة في لوحة الألوان.
 * الحسابات: مطابقة الاسم النصي مع المعرّف.
 */

package com.example.brick_breaker_ball.assets

enum class SpriteId(val value: String) {
    BRICK_ROUGH_STONE("brick_rough_stone"),
    BRICK_LIGHTNING_SPEED_PASS_THROUGH("brick_lightning_speed_pass_through"),
    BRICK_ELECTRIC_WHITE_INTACT("brick_electric_white_intact"),
    BRICK_CRYSTAL_BLUE_INTACT("brick_crystal_blue_intact"),
    BRICK_CRYSTAL_RED_INTACT("brick_crystal_red_intact"),
    BRICK_CRYSTAL_PURPLE_INTACT("brick_crystal_purple_intact"),
    BRICK_STONE_GRAY_INTACT("brick_stone_gray_intact"),
    BRICK_BLACK_HOLE_TELEPORTER("brick_black_hole_teleporter"),
    BRICK_RANDOM_INVENTORY_POWERUP("brick_random_inventory_powerup"),
    BRICK_ELECTRIC_WHITE_DAMAGED("brick_electric_white_damaged"),
    BRICK_CRYSTAL_BLUE_DAMAGED("brick_crystal_blue_damaged"),
    BRICK_CRYSTAL_RED_DAMAGED("brick_crystal_red_damaged"),
    BRICK_CRYSTAL_PURPLE_DAMAGED("brick_crystal_purple_damaged"),
    BRICK_STONE_GRAY_DAMAGED("brick_stone_gray_damaged"),
    BRICK_STEEL_UNBREAKABLE("brick_steel_unbreakable"),
    BRICK_CRYSTAL_MAROON_INTACT("brick_crystal_MAROON_intact"),
    BRICK_ARMORED_DARK_INTACT("brick_armored_dark_intact"),
    BRICK_CRYSTAL_PINK_INTACT("brick_crystal_pink_intact"),
    BRICK_CRYSTAL_ORANGE_INTACT("brick_crystal_orange_intact"),
    BRICK_CRYSTAL_GREEN_INTACT("brick_crystal_green_intact"),
    BRICK_BASIC_ONE_HIT("brick_basic_one_hit"),
    BRICK_SPIKED_HAZARD("brick_spiked_hazard"),
    BRICK_CRYSTAL_MAROON_DAMAGED("brick_crystal_MAROON_damaged"),
    BRICK_ARMORED_DARK_DAMAGED("brick_armored_dark_damaged"),
    BRICK_CRYSTAL_PINK_DAMAGED("brick_crystal_pink_damaged"),
    BRICK_CRYSTAL_ORANGE_DAMAGED("brick_crystal_orange_damaged"),
    BRICK_CRYSTAL_GREEN_DAMAGED("brick_crystal_green_damaged"),
    BRICK_TRANSPARENT_SLOW_PASS_THROUGH("brick_transparent_slow_pass_through");

    /** ملاحظة صيانة: الدالة `from` تحوّل البيانات أو تبني المعرّف المتوافق مع بقية النظام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    companion object {
        private val byValue = entries.associateBy(SpriteId::value)
        fun from(value: String) = byValue[value]
    }
}
