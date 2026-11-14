/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/ui/ForgeUiPalette.kt
 * الوظيفة: ألوان الواجهة الموحدة.
 * الحسابات: لا توجد معادلات.
 */

package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.graphics.Color

/**
 * Unified BrickBreakerBall UI palette — Crimson Ivory Navy.
 *
 * The three source colors are sampled from the supplied reference image:
 *  - Navy   #282D41
 *  - Ivory  #DED7CD
 *  - Crimson #9C2831
 *
 * Supporting shades are tonal derivatives only; no cyan/orange/green UI accents are introduced.
 */
object ForgeUiPalette {
    val navy = Color.valueOf("282D41FF")
    val ivory = Color.valueOf("DED7CDFF")
    val crimsonBase = Color.valueOf("9C2831FF")

    // Navy surfaces.
    val neutralBlack = Color.valueOf("171A26FF")
    val neutralDark = Color.valueOf("202536FF")
    val neutralDark2 = navy
    val neutralDark3 = Color.valueOf("343B55FF")
    val neutralBorder = Color.valueOf("50586FFF")
    val neutralBlackAlpha = Color.valueOf("171A26CC")

    // Primary interaction = crimson.
    val primaryDark = Color.valueOf("6F1C23FF")
    val primary = crimsonBase
    val primaryLight = Color.valueOf("BE3A45FF")

    // Secondary / premium interaction = ivory.
    val crimsonDark = Color.valueOf("BDB6ADFF")
    val crimson = ivory
    val crimsonLight = Color.valueOf("F0ECE5FF")

    // Positive state intentionally stays inside the same three-color family.
    // A separate green status is used ONLY for the selected equipped ball button.
    val equippedBallGreenDark = Color.valueOf("0B4D2CFF")
    val equippedBallGreen = Color.valueOf("146735FF")
    val equippedBallGreenLight = Color.valueOf("1D673EFF")
    val successDark = Color.valueOf("BDB6ADFF")
    val success = ivory
    val successLight = Color.valueOf("F0ECE5FF")

    // Text.
    val textPrimary = ivory
    val textSecondary = Color.valueOf("C9C2B9FF")
    val muted = Color.valueOf("9D98A0FF")

    // Disabled surfaces remain navy-family only.
    val disabledDark = Color.valueOf("1B1F2DFF")
    val disabled = Color.valueOf("282D4199")
    val disabledLight = Color.valueOf("50586FFF")

    val danger = crimsonBase

    // Glass / overlays.
    val glassOverlay = Color.valueOf("171A26CC")
    val glassPanel = Color.valueOf("202536EB")

    // Compatibility aliases.
    val deepNavy = neutralBlack
    val steel = neutralDark2
    val button = neutralDark2

    val pauseButton = primary
    val buttonPressed = primaryLight

    val purpleDark = primaryDark
    val purple = primary
    val purpleLight = primaryLight

    val maroonDark = primaryDark
    val maroon = primary
    val maroonLight = primaryLight

    val goldDark = Color.valueOf("BDB6ADFF")
    val gold = ivory
    val goldLight = Color.valueOf("F0ECE5FF")
}
