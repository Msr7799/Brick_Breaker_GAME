/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/main/java/com/example/brick_breaker_ball/DetailedSpriteSheet.kt
 * المؤلف: mohamed alromaihi
 * الدوال الموجودة: `from`، `region`، `nextState`، `previousState`، `dispose`
 */

package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.JsonReader

enum class SpriteId(val value: String) {
    PADDLE_NORMAL("paddle_normal"),
    PADDLE_LASER("paddle_laser"),
    PADDLE_STICKY("paddle_sticky"),
    POWERUP_LASER_AUTO_CHARGE("powerup_laser_auto_charge"),
    POWERUP_EXTRA_LIFE("powerup_extra_life"),
    POWERUP_KILL_PLAYER("powerup_kill_player"),
    POWERUP_EXPAND_PADDLE("powerup_expand_paddle"),
    POWERUP_TIMED_BOMB_BRICKS("powerup_timed_bomb_bricks"),
    POWERUP_RANDOM_POSITIVE("powerup_random_positive"),
    POWERUP_SHRINK_BALL("powerup_shrink_ball"),
    POWERUP_SHRINK_PADDLE("powerup_shrink_paddle"),
    POWERUP_FIREBALL("powerup_fireball"),
    POWERUP_MAGNET_PADDLE("powerup_magnet_paddle"),
    POWERUP_SLOW_BALL("powerup_slow_ball"),
    POWERUP_MULTIBALL_PLUS_4("powerup_multiball_plus_4"),
    POWERUP_DUAL_PADDLE("powerup_dual_paddle"),
    POWERUP_SPEED_BALL("powerup_speed_ball"),
    POWERUP_INSTANT_KILL_BALL("powerup_instant_kill_ball"),
    POWERUP_BIG_BALL("powerup_big_ball"),
    POWERUP_STICKY_PADDLE("powerup_sticky_paddle"),
    POWERUP_ONE_HIT_ANY_BRICK("powerup_one_hit_any_brick"),
    POWERUP_GHOST_BALL("powerup_ghost_ball"),
    POWERUP_MULTIBALL_15("powerup_multiball_15"),
    BALL_FIRE_SMALL("ball_fire_small"),
    BALL_NORMAL_SMALL("ball_normal_small"),
    BALL_PIERCING_SMALL("ball_piercing_small"),
    LASER_BULLET("laser_bullet"),
    BALL_FIRE_MEDIUM("ball_fire_medium"),
    BALL_NORMAL_MEDIUM("ball_normal_medium"),
    BALL_PIERCING_MEDIUM("ball_piercing_medium"),
    BALL_FIRE_LARGE("ball_fire_large"),
    BALL_NORMAL_LARGE("ball_normal_large"),
    BALL_PIERCING_LARGE("ball_piercing_large"),
    SPARK_FRAME_01("spark_frame_01"),
    SPARK_FRAME_02("spark_frame_02"),
    SPARK_FRAME_03("spark_frame_03"),
    SPARK_FRAME_04("spark_frame_04"),
    SPARK_FRAME_05("spark_frame_05"),
    SPARK_FRAME_06("spark_frame_06"),
    SPARK_FRAME_07("spark_frame_07"),
    SPARK_FRAME_08("spark_frame_08"),
    ELECTRIC_FLOOR_WIRE("electric_floor_wire"),
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

data class SpriteMetadata(
    val id: SpriteId,
    val category: String,
    val x: Int,
    val y: Int,
    val width: Int,
    val height: Int,
    val nextState: SpriteId?,
    val previousState: SpriteId?
)

class DetailedSpriteSheet(imagePath: String = "sprites/spritesheet.png", manifestPath: String = "sprites/sprite-map-detailed.json") {
    val texture = Texture(Gdx.files.internal(imagePath)).apply {
        // Exact source rectangles have no extrusion. Nearest prevents sampling a neighboring sprite.
        setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest)
        setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
    }
    val metadata: Map<SpriteId, SpriteMetadata>
    private val regions: Map<SpriteId, TextureRegion>

    init {
        val root = JsonReader().parse(Gdx.files.internal(manifestPath))
        val atlas = root.get("atlas")
        require(texture.width == 1254 && texture.height == 1254) { "Detailed sprite sheet must be 1254x1254" }
        require(atlas.getInt("width") == texture.width && atlas.getInt("height") == texture.height)
        val sprites = root.get("sprites")
        require(sprites.size == 70) { "Detailed sprite manifest must contain exactly 70 sprites" }
        val parsed = linkedMapOf<SpriteId, SpriteMetadata>()
        sprites.forEach { item ->
            val rawId = item.getString("id")
            val id = requireNotNull(SpriteId.from(rawId)) { "Unknown typed sprite id: $rawId" }
            require(id !in parsed) { "Duplicate sprite id: $rawId" }
            val rect = item.get("rect")
            val x = rect.getInt("x")
            val y = rect.getInt("y")
            val width = rect.getInt("width")
            val height = rect.getInt("height")
            require(width > 0 && height > 0 && x >= 0 && y >= 0 && x + width <= texture.width && y + height <= texture.height)
            parsed[id] = SpriteMetadata(
                id,
                item.getString("category"),
                x,
                y,
                width,
                height,
                item.getString("nextState", null)?.let(SpriteId::from),
                item.getString("previousState", null)?.let(SpriteId::from)
            )
        }
        require(parsed.keys == SpriteId.entries.toSet()) { "Typed sprite ids and manifest ids differ" }
        val rejected = root.get("rejectedDetections")
        require(rejected.size == 7)
        metadata = parsed
        // TextureRegion uses the same top-left pixel convention for source rectangles; no Y conversion is needed.
        regions = parsed.mapValues { (_, value) -> TextureRegion(texture, value.x, value.y, value.width, value.height) }
    }

    /** ملاحظة صيانة: الدالة `region` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun region(id: SpriteId): TextureRegion = regions.getValue(id)

    /** ملاحظة صيانة: الدالة `nextState` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun nextState(id: SpriteId): SpriteId? = metadata.getValue(id).nextState

    /** ملاحظة صيانة: الدالة `previousState` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun previousState(id: SpriteId): SpriteId? = metadata.getValue(id).previousState

    /** ملاحظة صيانة: الدالة `dispose` تنظّف الحالة أو الموارد المرتبطة بهذه المسؤولية بأمان؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun dispose() = texture.dispose()
}
