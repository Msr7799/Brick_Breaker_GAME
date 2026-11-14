/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/assets/VolleyAssetCatalog.kt
 * الوظيفة: ربط تأثيرات Volley بالأيقونات والأصول.
 * الحسابات: اختيار المنطقة المناسبة حسب نوع العنصر.
 */

package com.example.brick_breaker_ball.assets

import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.example.brick_breaker_ball.campaign.VolleyStageRepository
import com.example.brick_breaker_ball.gameplay.Brick
import com.example.brick_breaker_ball.gameplay.BrickType
import com.example.brick_breaker_ball.gameplay.PowerUpType
import com.example.brick_breaker_ball.gameplay.VolleyBonusNodeType
import com.example.brick_breaker_ball.gameplay.VolleyBrickVisualFamily

enum class VolleyIcon { HEALTH, SHIELD, SWORD, MULTI_BALL, TIME, MAGNET, EXPAND, SPEED, FIRE, ICE, LIGHTNING, TARGET, QUESTION, COIN }
enum class VolleyEnvironment { FLOATING_ISLAND_GRASS, FLOATING_ISLAND_RUIN, PILLAR_FRAGMENT, RUIN_PILLAR_LARGE, STONE_FRAGMENT_SMALL, CHAIN_SHORT_A, CHAIN_SHORT_B, BANNER_BLUE, BANNER_RED_LARGE, BANNER_RED_SMALL, CHAIN_MID_A, CHAIN_MID_B, CHAIN_LONG_RIGHT, TORCH_FIRE, TORCH_BLUE, CRYSTAL_BLUE_LEFT, CRYSTAL_RED, CRYSTAL_PURPLE, CRYSTAL_BLUE_RIGHT, RUBBLE_CRYSTAL_LEFT, RUBBLE_CRYSTAL_RED, RUBBLE_CRYSTAL_BLUE }
enum class VolleyVfx { SMOKE_A, SMOKE_B, SMOKE_C, SPARK_BLUE, SPARK_ORANGE, SPARK_PURPLE, MAGIC_SHARDS, BEAM_BLUE, BEAM_CYAN, BEAM_PURPLE, BEAM_PINK, BEAM_RED, BEAM_ORANGE, BEAM_GOLD, GROUND_BURST_BLUE, GROUND_BURST_ORANGE, GROUND_BURST_PURPLE, GROUND_RING_BLUE, GROUND_SPIKES_ORANGE_A, GROUND_SPIKES_ORANGE_B, GROUND_SPIKES_RED, PORTAL_RING_PURPLE, PORTAL_RING_RED }

/** Typed bridge between Volley simulation identities and the numbered crop atlas. */
class VolleyAssetCatalog(private val atlas: TextureAtlas) {
    companion object {
        /** Clamps lookups to the supplied nine-world Volley campaign. */
        fun worldArtId(world: Int): Int = world.coerceIn(1, VolleyStageRepository.WORLDS_COUNT)

        fun familyFor(type: BrickType, world: Int): VolleyBrickVisualFamily = when (type) {
            BrickType.ROUGH_STONE -> VolleyBrickVisualFamily.ROUGH
            BrickType.REGENERATING -> VolleyBrickVisualFamily.REGENERATING
            BrickType.POWERUP_CARRIER, BrickType.RANDOM_INVENTORY_POWERUP -> VolleyBrickVisualFamily.CARRIER
            BrickType.ARMORED_TWO_HIT, BrickType.ARMORED_THREE_HIT, BrickType.ARMORED_DARK -> VolleyBrickVisualFamily.ARMORED
            BrickType.TRANSPARENT_SLOW_PASS_THROUGH, BrickType.LIGHTNING_SPEED_PASS_THROUGH -> VolleyBrickVisualFamily.ICE
            BrickType.EXPLOSIVE -> VolleyBrickVisualFamily.EXPLOSIVE
            BrickType.SWITCH -> VolleyBrickVisualFamily.SWITCH
            BrickType.GHOST -> VolleyBrickVisualFamily.GHOST
            BrickType.LOCKED, BrickType.KEY_BRICK -> VolleyBrickVisualFamily.LOCK
            BrickType.CHAIN_BRICK -> VolleyBrickVisualFamily.CHAIN
            BrickType.INDESTRUCTIBLE -> VolleyBrickVisualFamily.GATE
            BrickType.SPIKED_HAZARD -> VolleyBrickVisualFamily.SPIKE
            BrickType.BLACK_HOLE_TELEPORTER -> VolleyBrickVisualFamily.PORTAL
            BrickType.BOSS_CORE -> VolleyBrickVisualFamily.BOSS
            else -> standardFamily(world, type.ordinal)
        }

        private fun standardFamily(world: Int, salt: Int): VolleyBrickVisualFamily {
            // Keep ordinary bricks visually tied to the selected world. The previous global color
            // cycle made Verdant Ruins show red/purple bricks and made every board feel unrelated
            // to its background. These compact palettes deliberately reuse the numbered
            // brick crops while still giving each world two or three readable accents.
            val palette = when (world.coerceIn(1, VolleyStageRepository.WORLDS_COUNT)) {
                1 -> arrayOf(VolleyBrickVisualFamily.STANDARD_GREEN, VolleyBrickVisualFamily.STANDARD_BROWN, VolleyBrickVisualFamily.STANDARD_GOLD)
                2 -> arrayOf(VolleyBrickVisualFamily.STANDARD_CYAN, VolleyBrickVisualFamily.STANDARD_BLUE, VolleyBrickVisualFamily.STANDARD_GRAY)
                3 -> arrayOf(VolleyBrickVisualFamily.STANDARD_GOLD, VolleyBrickVisualFamily.STANDARD_ORANGE, VolleyBrickVisualFamily.STANDARD_BROWN)
                4 -> arrayOf(VolleyBrickVisualFamily.STANDARD_RED, VolleyBrickVisualFamily.STANDARD_ORANGE, VolleyBrickVisualFamily.STANDARD_GOLD)
                5 -> arrayOf(VolleyBrickVisualFamily.STANDARD_BLUE, VolleyBrickVisualFamily.STANDARD_PURPLE, VolleyBrickVisualFamily.STANDARD_CYAN)
                6 -> arrayOf(VolleyBrickVisualFamily.STANDARD_CYAN, VolleyBrickVisualFamily.STANDARD_BLUE, VolleyBrickVisualFamily.STANDARD_GREEN)
                7 -> arrayOf(VolleyBrickVisualFamily.STANDARD_PURPLE, VolleyBrickVisualFamily.STANDARD_PINK, VolleyBrickVisualFamily.STANDARD_CYAN)
                8 -> arrayOf(VolleyBrickVisualFamily.STANDARD_BROWN, VolleyBrickVisualFamily.STANDARD_ORANGE, VolleyBrickVisualFamily.STANDARD_RED)
                9 -> arrayOf(VolleyBrickVisualFamily.STANDARD_GOLD, VolleyBrickVisualFamily.STANDARD_CYAN, VolleyBrickVisualFamily.STANDARD_PURPLE)
                else -> arrayOf(VolleyBrickVisualFamily.STANDARD_PURPLE, VolleyBrickVisualFamily.STANDARD_GRAY, VolleyBrickVisualFamily.STANDARD_RED)
            }
            return palette[Math.floorMod(salt, palette.size)]
        }
    }

    fun icon(icon: VolleyIcon, glow: Boolean = false): TextureRegion {
        val base = 86 + icon.ordinal * 2 + if (glow) 1 else 0
        val suffix = icon.name.lowercase()
        return region("${base.toString().padStart(3, '0')}_icon_${suffix}${if (glow) "_glow" else ""}")
    }

    fun energyBar(white: Boolean, bottom: Boolean): TextureRegion {
        val number = when {
            !white && !bottom -> 114
            white && !bottom -> 115
            !white -> 116
            else -> 117
        }
        return region("${number}_energy_bar_${if (white) "white" else "blue"}_${if (bottom) "bottom" else "top"}")
    }

    /** Uses the stage reward tiles from the Volley sheet instead of HUD icons in-world. */
    fun bonusNode(type: VolleyBonusNodeType): TextureRegion = region(
        when (type) {
            VolleyBonusNodeType.BALL -> "092_icon_multi_ball"

            VolleyBonusNodeType.CROWN -> "041_tile_crown"

            VolleyBonusNodeType.GEMS -> "112_icon_coin"

            // old saved games show coin graphics too
            VolleyBonusNodeType.COINS -> "112_icon_coin"

            VolleyBonusNodeType.SHIELD -> "045_tile_shield"

            VolleyBonusNodeType.MYSTERY -> "047_tile_mystery"

            VolleyBonusNodeType.STAR -> "048_tile_star"

            VolleyBonusNodeType.SPEED -> "049_tile_speed"
        }
    )

    /** Volley owns a distinct fantasy icon language even when an effect reuses a canonical type id. */
    fun itemIcon(type: PowerUpType, glow: Boolean = false): TextureRegion = icon(
        when (type) {
            PowerUpType.LASER_AUTO_CHARGE -> VolleyIcon.LIGHTNING
            PowerUpType.EXTRA_LIFE -> VolleyIcon.HEALTH
            PowerUpType.EXPAND_PADDLE -> VolleyIcon.EXPAND
            PowerUpType.TIMED_BOMB_BRICKS -> VolleyIcon.TIME
            PowerUpType.RANDOM_GOOD -> VolleyIcon.QUESTION
            PowerUpType.FIRE_BALL -> VolleyIcon.FIRE
            PowerUpType.MAGNETIC_PADDLE -> VolleyIcon.MAGNET
            PowerUpType.SLOW_BALL -> VolleyIcon.ICE
            PowerUpType.MULTIBALL_PLUS_4, PowerUpType.MULTIBALL_15 -> VolleyIcon.MULTI_BALL
            PowerUpType.DUAL_PADDLE -> VolleyIcon.TARGET
            PowerUpType.MEGA_BALL, PowerUpType.ONE_HIT_ANY_BRICK -> VolleyIcon.SWORD
            PowerUpType.STICKY_PADDLE -> VolleyIcon.SHIELD
            else -> VolleyIcon.QUESTION
        },
        glow
    )

    fun environment(asset: VolleyEnvironment): TextureRegion = region(environmentNames.getValue(asset))
    fun vfx(asset: VolleyVfx): TextureRegion = region(vfxNames.getValue(asset))
    fun accessibilityMarker(brick: Brick): String = if (brick.volleyShieldLock) {
        "SH"
    } else {
        when (brick.volleyVisualFamily ?: familyFor(brick.type, 1)) {
            VolleyBrickVisualFamily.STANDARD_GRAY -> "GY"
            VolleyBrickVisualFamily.STANDARD_BROWN -> "BR"
            VolleyBrickVisualFamily.STANDARD_ORANGE -> "OR"
            VolleyBrickVisualFamily.STANDARD_RED -> "RD"
            VolleyBrickVisualFamily.STANDARD_GOLD -> "AU"
            VolleyBrickVisualFamily.STANDARD_GREEN -> "GN"
            VolleyBrickVisualFamily.STANDARD_CYAN -> "CY"
            VolleyBrickVisualFamily.STANDARD_BLUE -> "BL"
            VolleyBrickVisualFamily.STANDARD_PURPLE -> "PU"
            VolleyBrickVisualFamily.STANDARD_PINK -> "PK"
            VolleyBrickVisualFamily.ROUGH -> "RF"
            VolleyBrickVisualFamily.REGENERATING -> "RG"
            VolleyBrickVisualFamily.CARRIER -> "+"
            VolleyBrickVisualFamily.ARMORED -> "AR"
            VolleyBrickVisualFamily.ICE -> "IC"
            VolleyBrickVisualFamily.EXPLOSIVE -> "EX"
            VolleyBrickVisualFamily.SWITCH -> "SW"
            VolleyBrickVisualFamily.GHOST -> "GH"
            VolleyBrickVisualFamily.LOCK -> "LK"
            VolleyBrickVisualFamily.SHIELD_LOCK -> "SH"
            VolleyBrickVisualFamily.CHAIN -> "CH"
            VolleyBrickVisualFamily.GATE -> "GT"
            VolleyBrickVisualFamily.SPIKE -> "HZ"
            VolleyBrickVisualFamily.PORTAL -> "PT"
            VolleyBrickVisualFamily.BOSS -> "B"
        }
    }

    private val environmentNames = VolleyEnvironment.entries.associateWith { asset ->
        val number = 118 + asset.ordinal
        "${number.toString().padStart(3, '0')}_${asset.name.lowercase()}"
    }
    private val vfxNames = mapOf(
        VolleyVfx.SMOKE_A to "140_smoke_cloud_a", VolleyVfx.SMOKE_B to "141_smoke_cloud_b", VolleyVfx.SMOKE_C to "142_smoke_cloud_c",
        VolleyVfx.SPARK_BLUE to "143_spark_blue", VolleyVfx.SPARK_ORANGE to "144_spark_orange", VolleyVfx.SPARK_PURPLE to "145_spark_purple",
        VolleyVfx.MAGIC_SHARDS to "146_magic_shards_purple", VolleyVfx.BEAM_BLUE to "147_beam_blue", VolleyVfx.BEAM_CYAN to "148_beam_cyan",
        VolleyVfx.BEAM_PURPLE to "149_beam_purple", VolleyVfx.BEAM_PINK to "150_beam_pink", VolleyVfx.BEAM_RED to "151_beam_red",
        VolleyVfx.BEAM_ORANGE to "152_beam_orange", VolleyVfx.BEAM_GOLD to "153_beam_gold", VolleyVfx.GROUND_BURST_BLUE to "154_ground_burst_blue",
        VolleyVfx.GROUND_BURST_ORANGE to "155_ground_burst_orange", VolleyVfx.GROUND_BURST_PURPLE to "156_ground_burst_purple",
        VolleyVfx.GROUND_RING_BLUE to "157_ground_ring_blue", VolleyVfx.GROUND_SPIKES_ORANGE_A to "158_ground_spikes_orange_a",
        VolleyVfx.GROUND_SPIKES_ORANGE_B to "159_ground_spikes_orange_b", VolleyVfx.GROUND_SPIKES_RED to "160_ground_spikes_red",
        VolleyVfx.PORTAL_RING_PURPLE to "161_portal_ring_purple", VolleyVfx.PORTAL_RING_RED to "162_portal_ring_red"
    )

    private fun region(name: String): TextureRegion = requireNotNull(atlas.findRegion(name)) { "Missing Volley region: $name" }
}
