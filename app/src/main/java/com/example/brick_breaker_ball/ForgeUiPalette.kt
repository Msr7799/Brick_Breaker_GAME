package com.example.brick_breaker_ball

import com.badlogic.gdx.graphics.Color

/** Unified BrickBreakerBall UI palette — Midnight Forge. */
object ForgeUiPalette {

    // Core neutral family — the base of every generated UI surface.
    val neutralBlack = Color.valueOf("080D17FF")
    val neutralDark = Color.valueOf("101828FF")
    val neutralDark2 = Color.valueOf("182230FF")
    val neutralDark3 = Color.valueOf("1D2939FF")
    val neutralBorder = Color.valueOf("344054FF")
    val neutralBlackAlpha = Color.valueOf("080D17CC")

    // Primary accent — burnt forge orange.
    val primaryDark = Color.valueOf("7A2E0BFF")
    val primary = Color.valueOf("CA3500FF")
    val primaryLight = Color.valueOf("F9703EFF")

    // Secondary accent — deep crimson. Use sparingly for special/rare emphasis.
    val crimsonDark = Color.valueOf("510B12FF")
    val crimson = Color.valueOf("9F0712FF")
    val crimsonLight = Color.valueOf("D92D3AFF")

    // Positive semantic accent — olive green. Reserved for owned/equipped/success states.
    val successDark = Color.valueOf("263F00FF")
    val success = Color.valueOf("3C6300FF")
    val successLight = Color.valueOf("669900FF")

    // Text.
    val textPrimary = Color.valueOf("F2F4F7FF")
    val textSecondary = Color.valueOf("D0D5DDFF")
    val muted = Color.valueOf("98A2B3FF")

    // Disabled.
    val disabledDark = Color.valueOf("171D24FF")
    val disabled = Color.valueOf("34405499")
    val disabledLight = Color.valueOf("475467FF")

    // Semantic danger remains red and is not used as a general decorative accent.
    val danger = Color.valueOf("D64545FF")

    // Glass / overlays.
    val glassOverlay = Color.valueOf("080D17CC")
    val glassPanel = Color.valueOf("101828EB")

    // Compatibility aliases used by existing screens. These intentionally map the old
    // purple/maroon/gold naming to the new cohesive Midnight Forge palette.
    val deepNavy = neutralBlack
    val steel = neutralDark2
    val button = neutralDark2

    // Keep the gameplay/pause tint as authored; the image-based pause/menu/settings art is untouched.
    val pauseButton = Color.valueOf("C95A0AFF")
    val buttonPressed = Color.valueOf("D16201FF")

    val purpleDark = primaryDark
    val purple = primary
    val purpleLight = primaryLight

    val maroonDark = crimsonDark
    val maroon = crimson
    val maroonLight = crimsonLight

    // Legacy gold now resolves to the warm primary family, avoiding bright yellow clashes.
    val goldDark = primaryDark
    val gold = primary
    val goldLight = primaryLight
}
