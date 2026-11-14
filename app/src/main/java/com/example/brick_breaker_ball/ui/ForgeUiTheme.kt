/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/ui/ForgeUiTheme.kt
 * الوظيفة: قيم شكلية مشتركة للواجهة.
 * الحسابات: تحويل الحجم والمسافات إلى قياسات العرض.
 */

package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.graphics.Color

/** Legacy facade using the three-color Crimson / Ivory / Navy theme. */
object ForgeUiTheme {
    val BACKDROP = Color.valueOf("171A26FF")
    val SURFACE = Color.valueOf("202536F5")
    val SURFACE_GLASS = Color.valueOf("202536DB")
    val CARD = Color.valueOf("282D41FA")
    val CARD_BORDER = Color.valueOf("50586FEB")

    val BUTTON_NORMAL = Color.valueOf("282D41FF")
    val BUTTON_PRESSED = Color.valueOf("9C2831FF")
    val BUTTON_DISABLED = Color.valueOf("282D41C7")

    val MAROON = Color.valueOf("9C2831FF")
    val MAROON_SOFT = Color.valueOf("BE3A45FF")
    val ORANGE = Color.valueOf("9C2831FF")
    val GOLD = Color.valueOf("DED7CDFF")
    val VIOLET = Color.valueOf("9C2831FF")

    val SUCCESS = Color.valueOf("DED7CDFF")
    val DANGER = Color.valueOf("9C2831FF")
    val MUTED = Color.valueOf("9D98A0FF")
    val TEXT = Color.valueOf("DED7CDFF")
}
