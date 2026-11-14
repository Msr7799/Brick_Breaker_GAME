package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.math.Rectangle

/** Shared render/touch bounds. A single equipment tray replaces stacked information cards. */
internal class BallShowcaseLayout {
    val page = Rectangle()
    val header = Rectangle()
    val back = Rectangle()
    val help = Rectangle()
    val coins = Rectangle()
    val gems = Rectangle()
    val hero = Rectangle()
    val previous = Rectangle()
    val next = Rectangle()
    val ball = Rectangle()
    val pedestal = Rectangle()
    val name = Rectangle()
    val identity = Rectangle()
    val position = Rectangle()
    val tray = Rectangle()
    val progression = Rectangle()
    val unlockHint = Rectangle()
    val primary = Rectangle()
    val straight = Rectangle()
    val spread = Rectangle()
    val infoPanel = Rectangle()
    val infoClose = Rectangle()
    val infoBody = Rectangle()
    val tabs = Array(4) { Rectangle() }
    val stats = Array(3) { Rectangle() }
    var scale = 1f
        private set
    var wide = false
        private set
    private val last = FloatArray(4) { Float.NaN }
    private var lastModes: Boolean? = null

    fun update(left: Float, bottom: Float, width: Float, height: Float, showModes: Boolean) {
        if (last[0] == left && last[1] == bottom && last[2] == width && last[3] == height && lastModes == showModes) return
        last[0] = left; last[1] = bottom; last[2] = width; last[3] = height; lastModes = showModes
        wide = width / height.coerceAtLeast(1f) >= 1.12f
        val dw = if (wide) 1440f else 860f
        val dh = if (wide) 900f else 1552f
        scale = minOf(width / dw, height / dh).coerceAtLeast(.01f)
        val x = left + (width - dw * scale) / 2f
        val y = bottom + (height - dh * scale) / 2f
        fun place(rect: Rectangle, px: Float, py: Float, w: Float, h: Float) = rect.set(x + px * scale, y + py * scale, w * scale, h * scale)
        place(page, 0f, 0f, dw, dh)
        if (wide) {
            place(back, 24f, 802f, 80f, 80f); place(header, 122f, 794f, 658f, 96f); place(help, 1336f, 802f, 80f, 80f)
            place(coins, 824f, 808f, 226f, 64f); place(gems, 1074f, 808f, 226f, 64f)
            tabs.forEachIndexed { i, r -> place(r, 24f + i * 352f, 708f, 336f, 76f) }
            place(hero, 24f, 24f, 664f, 664f); place(tray, 712f, 284f, 704f, 404f)
            stats.forEachIndexed { i, r -> place(r, 744f + i * 216f, 458f, 208f, 198f) }
            place(progression, 800f, 324f, 528f, 64f); place(unlockHint, 800f, 232f, 528f, 36f)
            place(primary, 712f, 106f, if (showModes) 464f else 704f, 104f)
            place(straight, 1192f, 114f, 104f, 88f); place(spread, 1312f, 114f, 104f, 88f)
            place(infoPanel, 250f, 114f, 940f, 652f)
        } else {
            place(back, 20f, 1460f, 84f, 84f); place(header, 114f, 1452f, 632f, 96f); place(help, 756f, 1460f, 84f, 84f)
            place(coins, 140f, 1380f, 282f, 64f); place(gems, 438f, 1380f, 282f, 64f)
            tabs.forEachIndexed { i, r -> place(r, 20f + i * 209f, 1288f, 193f, 76f) }
            place(hero, 20f, 640f, 820f, 628f); place(tray, 20f, 238f, 820f, 382f)
            stats.forEachIndexed { i, r -> place(r, 60f + i * 252f, 384f, 236f, 206f) }
            place(progression, 152f, 272f, 556f, 64f); place(unlockHint, 152f, 176f, 556f, 40f)
            place(primary, if (showModes) 20f else 80f, 60f, if (showModes) 580f else 700f, 104f)
            place(straight, 616f, 68f, 104f, 88f); place(spread, 736f, 68f, 104f, 88f)
            place(infoPanel, 40f, 310f, 780f, 932f)
        }
        val s = scale
        val diameter = 360f * s
        ball.set(hero.x + (hero.width - diameter) / 2f, hero.y + hero.height * .60f - diameter / 2f, diameter, diameter)
        pedestal.set(hero.x + hero.width / 2f - 224f * s, ball.y - 42f * s, 448f * s, 100f * s)
        previous.set(hero.x + 12f * s, ball.y + diameter / 2f - 56f * s, 76f * s, 112f * s)
        next.set(hero.x + hero.width - 88f * s, previous.y, previous.width, previous.height)
        name.set(hero.x + 38f * s, hero.y + 106f * s, hero.width - 76f * s, 76f * s)
        identity.set(hero.x + hero.width / 2f - 150f * s, hero.y + 64f * s, 300f * s, 30f * s)
        position.set(hero.x + hero.width / 2f - 100f * s, hero.y + 16f * s, 200f * s, 30f * s)
        infoClose.set(infoPanel.x + infoPanel.width - 96f * s, infoPanel.y + infoPanel.height - 100f * s, 72f * s, 72f * s)
        infoBody.set(infoPanel.x + 56f * s, infoPanel.y + 62f * s, infoPanel.width - 112f * s, infoPanel.height - 216f * s)
    }

    fun contains(rect: Rectangle, x: Float, y: Float): Boolean {
        val minimum = (if (wide) 88f else 120f) * scale
        val ex = ((minimum - rect.width) / 2f).coerceAtLeast(0f)
        val ey = ((minimum - rect.height) / 2f).coerceAtLeast(0f)
        return x >= rect.x - ex && x <= rect.x + rect.width + ex && y >= rect.y - ey && y <= rect.y + rect.height + ey
    }
}
