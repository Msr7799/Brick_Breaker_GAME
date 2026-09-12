package com.example.brick_breaker_ball

data class PowerUpInfo(val fullName: String, val shortName: String, val description: String)

/** Player-facing descriptions for the complete annotated 20-talisman set plus legacy aliases. */
object PowerUpInfoRepository {
    private val explicit = mapOf(
        PowerUpType.LASER_AUTO_CHARGE to
            PowerUpInfo("LASER AUTO CHARGE", "AUTO LASER", "Auto-fires twin lasers and recharges fast."),
        PowerUpType.EXTRA_LIFE to PowerUpInfo("EXTRA LIFE", "EXTRA LIFE", "Adds one extra life."),
        PowerUpType.KILL_PADDLE to
            PowerUpInfo("KILL PLAYER", "KILL PLAYER", "Hazard: destroys the paddle and costs one life."),
        PowerUpType.EXPAND_PADDLE to
            PowerUpInfo("EXPAND PADDLE", "WIDE PADDLE", "Widens the paddle. Stacks up to four times."),
        PowerUpType.TIMED_BOMB_BRICKS to
            PowerUpInfo(
                "TIMED BOMB BRICKS",
                "TIMED BOMBS",
                "Arms hit bricks with timed bombs that blast nearby bricks."
            ),
        PowerUpType.RANDOM_GOOD to
            PowerUpInfo(
                "RANDOM POSITIVE",
                "RANDOM GOOD",
                "Activates one random positive power-up."
            ),
        PowerUpType.SHRINK_BALL to
            PowerUpInfo("SHRINK BALL", "SHRINK BALL", "Hazard: shrinks every ball for this level."),
        PowerUpType.SHRINK_PADDLE to
            PowerUpInfo("SHRINK PADDLE", "SMALL PADDLE", "Hazard: makes the paddle one size smaller."),
        PowerUpType.FIRE_BALL to
            PowerUpInfo(
                "FIRE BALL",
                "FIRE BALL",
                "Fireballs destroy targets and scorch nearby bricks."
            ),
        PowerUpType.MAGNETIC_PADDLE to
            PowerUpInfo(
                "MAGNETIC PADDLE",
                "MAGNET",
                "Pulls descending balls toward the paddle."
            ),
        PowerUpType.SLOW_BALL to
            PowerUpInfo("SLOW BALL", "SLOW BALL", "Temporarily slows active balls."),
        PowerUpType.MULTIBALL_PLUS_4 to
            PowerUpInfo("+4 BALLS", "+4 BALLS", "Adds four balls, up to the 15-ball limit."),
        PowerUpType.DUAL_PADDLE to
            PowerUpInfo("DUAL PADDLE", "DUAL PADDLE", "Adds a second paddle for a short time."),
        PowerUpType.FAST_BALL to
            PowerUpInfo("FAST BALL", "FAST BALL", "Hazard: temporarily speeds up active balls."),
        PowerUpType.INSTANT_KILL_BALL to
            PowerUpInfo(
                "BALL-KILLING SPIKES",
                "BALL KILL",
                "Hazard: turns three bricks into ball-killing spikes."
            ),
        PowerUpType.MEGA_BALL to PowerUpInfo("MEGA BALL", "MEGA BALL", "Temporarily makes every active ball larger."),
        PowerUpType.STICKY_PADDLE to
            PowerUpInfo("STICKY PADDLE", "STICKY", "Catches balls so you can aim and release them."),
        PowerUpType.ONE_HIT_ANY_BRICK to
            PowerUpInfo(
                "ONE-HIT BALL",
                "ONE HIT",
                "Breaks any breakable brick in one hit."
            ),
        PowerUpType.GHOST_BALL to
            PowerUpInfo("GHOST BALL", "GHOST BALL", "Special: passes through bricks without hitting them."),
        PowerUpType.MULTIBALL_15 to
            PowerUpInfo("15 BALLS", "15 BALLS", "Creates balls until 15 are active.")
    )

    fun info(type: PowerUpType): PowerUpInfo = explicit[type] ?: PowerUpInfo(
        fullName = type.name.replace('_', ' '),
        shortName = when (type) {
            PowerUpType.PIERCING_BALL -> "PHASE BALL"
            PowerUpType.SET_OFF_EXPLODING -> "DETONATOR"
            PowerUpType.LEVEL_WARP -> "LEVEL WARP"
            PowerUpType.ZAP_BRICKS -> "ZAP BRICKS"
            PowerUpType.MULTI_BALL -> "MULTIBALL"
            PowerUpType.LASER_PADDLE -> "LASER PADDLE"
            PowerUpType.EXPAND_EXPLODING -> "BLAST RADIUS"
            PowerUpType.EIGHT_BALL -> "EIGHT BALL"
            PowerUpType.SUPER_SHRINK -> "SUPER SHRINK"
            PowerUpType.FALLING_BRICKS -> "FALLING BRICKS"
            else -> type.name.replace('_', ' ')
        },
        description = legacyDescription(type)
    )

    private fun legacyDescription(type: PowerUpType): String = when (type) {
        PowerUpType.PIERCING_BALL -> "Passes through breakable bricks; steel still reflects it."
        PowerUpType.SET_OFF_EXPLODING -> "Detonates all explosive bricks in chain reactions."
        PowerUpType.LEVEL_WARP -> "Instantly completes a non-boss level."
        PowerUpType.ZAP_BRICKS -> "Makes eligible tough bricks break in one hit."
        PowerUpType.MULTI_BALL -> "Doubles active balls, up to eight."
        PowerUpType.TRIPLE_BALL -> "Triples active balls up to the ball limit."
        PowerUpType.EIGHT_BALL -> "Creates up to eight balls at varied angles."
        PowerUpType.LASER_PADDLE -> "Temporarily equips twin laser cannons."
        PowerUpType.EXPLOSIVE_BALL -> "Ball hits create small explosions."
        PowerUpType.EXPAND_EXPLODING -> "Increases explosion radius for this level."
        PowerUpType.FALLING_BRICKS -> "Hazard: pushes breakable bricks downward."
        PowerUpType.SUPER_SHRINK -> "Hazard: shrinks the paddle to minimum width."
        PowerUpType.BOTTOM_SHIELD, PowerUpType.PADDLE_SHIELD -> "Adds a temporary bottom shield."
        PowerUpType.SCORE_X2 -> "Temporarily doubles score."
        PowerUpType.SCORE_X3 -> "Temporarily triples score."
        PowerUpType.POWERUP_MAGNET -> "Pulls falling power-ups toward the paddle."
        PowerUpType.INVERT_CONTROLS -> "Hazard: temporarily reverses controls."
        PowerUpType.SLIPPERY_PADDLE -> "Hazard: temporarily makes the paddle slippery."
        PowerUpType.POWERUP_JAM -> "Hazard: temporarily blocks power-up activation."
        PowerUpType.RANDOM_BAD -> "Activates one random negative effect."
        else -> "Activates ${type.name.replace('_', ' ').lowercase()} according to its gameplay rule."
    }
}
