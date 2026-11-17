package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.NinePatch
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Align
import com.example.brick_breaker_ball.assets.GameAssets

/** Picture controls use the same rendered fantasy materials as the game. */
internal object VisualGameUi {
    enum class Icon { BALLS, HIT, STRAIGHT, SPREAD, CHECK, LOCK, BACK, NEXT, EQUIP, DETAILS,
        SHIELD, ORB, BLAST, GRID, COIN, SORT, FILTER, SELL, PLUS, CLOSE, STAR, SPEED, RECALL }
    private val measure = com.badlogic.gdx.graphics.g2d.GlyphLayout()
    private val cream = Color(1f, .94f, .77f, 1f)
    private val gold = Color(1f, .76f, .25f, 1f)
    private val red = Color(1f, .34f, .25f, 1f)
    private var owner: GameAssets? = null
    private lateinit var stone: NinePatch
    private lateinit var darkButton: NinePatch
    private lateinit var greenButton: NinePatch
    const val MAX_POWER = 12

    private fun prepare(assets: GameAssets) {
        if (owner === assets) return
        stone = NinePatch(assets.boards2.requireRegion("stone_button_style_02"), 24, 24, 20, 20)
        fun face(name: String) = NinePatch(assets.ballCollectionButtons.requireRegion(name), 100, 100, 28, 28).apply {
            setLeftWidth(28f); setRightWidth(28f); setTopHeight(16f); setBottomHeight(16f)
        }
        darkButton = face("wide_dark")
        greenButton = face("wide_green")
        owner = assets
    }

    fun orbEffect(family: String): Icon = when (family) {
        "READY" -> Icon.BALLS
        "SPLIT" -> Icon.SPREAD
        "LUCK" -> Icon.COIN
        "SECONDW" -> Icon.SHIELD
        "EXPLOSIVE", "LIGHTNING" -> Icon.BLAST
        else -> Icon.HIT
    }

    fun panel(batch: SpriteBatch, assets: GameAssets, rect: Rectangle, selected: Boolean = false,
              opacity: Float = 1f) {
        prepare(assets)
        if (selected) {
            batch.setColor(1f, .67f, .16f, .20f * opacity)
            batch.draw(assets.softGlowRegion, rect.x - 8f, rect.y - 8f, rect.width + 16f, rect.height + 16f)
        }
        batch.setColor(1f, if (selected) .93f else .83f, if (selected) .73f else .72f, opacity)
        stone.draw(batch, rect.x, rect.y, rect.width, rect.height)
        batch.color = Color.WHITE
    }

    fun actionButton(batch: SpriteBatch, assets: GameAssets, rect: Rectangle, selected: Boolean) {
        prepare(assets)
        batch.color = Color.WHITE
        (if (selected) greenButton else darkButton).draw(batch, rect.x, rect.y, rect.width, rect.height)
    }

    fun number(batch: SpriteBatch, assets: GameAssets, text: String, rect: Rectangle,
               scale: Float = .65f, color: Color = cream) {
        val font = assets.hudValueFont
        val oldX = font.data.scaleX; val oldY = font.data.scaleY
        val oldColor = Color(font.color)
        font.color = color
        font.data.setScale(scale)
        measure.setText(font, text)
        font.data.setScale(scale * minOf(1f, rect.width / measure.width.coerceAtLeast(1f),
            rect.height * .80f / measure.height.coerceAtLeast(1f)))
        measure.setText(font, text)
        font.draw(batch, text, rect.x, rect.y + (rect.height + measure.height) / 2f, rect.width, Align.center, false)
        font.data.setScale(oldX, oldY); font.color = oldColor
    }

    private fun sprite(batch: SpriteBatch, region: TextureRegion, rect: Rectangle,
                       opacity: Float, rotation: Float = 0f, flip: Boolean = false) {
        val scale = minOf(rect.width / region.regionWidth, rect.height / region.regionHeight)
        val w = region.regionWidth * scale; val h = region.regionHeight * scale
        val shade = if (opacity < .6f) .55f else 1f
        batch.setColor(shade, shade, shade, opacity)
        batch.draw(region, rect.x + (rect.width - w) / 2f, rect.y + (rect.height - h) / 2f,
            w / 2f, h / 2f, w, h, if (flip) -1f else 1f, 1f, rotation)
        batch.color = Color.WHITE
    }

    fun icon(batch: SpriteBatch, assets: GameAssets, icon: Icon, rect: Rectangle, opacity: Float = 1f) {
        if (icon == Icon.PLUS || icon == Icon.CLOSE) {
            number(batch, assets, if (icon == Icon.PLUS) "+" else "X", rect, 1.10f,
                Color(if (icon == Icon.PLUS) gold else red).also { it.a = opacity })
            return
        }
        if (icon == Icon.SORT || icon == Icon.SPEED) {
            val arrow = requireNotNull(assets.atlasUiIcon("speed"))
            sprite(batch, arrow, Rectangle(rect.x, rect.y, rect.width * .48f, rect.height), opacity,
                if (icon == Icon.SPEED) -90f else 0f)
            sprite(batch, arrow, Rectangle(rect.x + rect.width * .50f, rect.y, rect.width * .48f, rect.height),
                opacity, if (icon == Icon.SPEED) -90f else 180f)
            return
        }
        val region = when (icon) {
            Icon.BALLS -> assets.visualUiArt.region("balls")
            Icon.HIT -> assets.visualUiArt.region("damage")
            Icon.STRAIGHT -> assets.visualUiArt.region("straight")
            Icon.SPREAD -> assets.visualUiArt.region("spread")
            Icon.CHECK, Icon.EQUIP -> assets.visualUiArt.region("check")
            Icon.DETAILS, Icon.FILTER -> assets.visualUiArt.region("details")
            Icon.LOCK -> assets.arcadeIcons.findRegion("assets_9cd24f93_033")
            Icon.SHIELD -> assets.arcadeIcons.findRegion("assets_9cd24f93_052")
            Icon.ORB -> assets.arcadeIcons.findRegion("assets_9cd24f93_042")
            Icon.BLAST -> assets.arcadeIcons.findRegion("assets_9cd24f93_078")
            Icon.GRID -> assets.arcadeIcons.findRegion("assets_9cd24f93_022")
            Icon.COIN, Icon.SELL -> assets.currencyCoinIcon
            Icon.BACK, Icon.NEXT -> assets.boards2.requireRegion("orange_back_arrow_button")
            Icon.STAR -> assets.particles.findRegion("star")
            Icon.RECALL -> assets.atlasUiIcon("recall")
            else -> error("Unmapped game icon: $icon")
        }
        sprite(batch, requireNotNull(region) { "Missing colored icon: $icon" }, rect, opacity, flip = icon == Icon.NEXT)
        if (icon == Icon.SELL) number(batch, assets, "-",
            Rectangle(rect.x + rect.width * .60f, rect.y, rect.width * .40f, rect.height * .45f), .70f, red)
    }

    fun equippedBadge(batch: SpriteBatch, assets: GameAssets, rect: Rectangle) {
        icon(batch, assets, Icon.CHECK, rect)
    }

    /** Highlight ring for an actually equipped item; selection glow in panel() stays separate. */
    fun equippedRing(batch: SpriteBatch, assets: GameAssets, rect: Rectangle) {
        prepare(assets)
        batch.setColor(.35f, 1f, .45f, .30f)
        batch.draw(assets.softGlowRegion, rect.x - 6f, rect.y - 6f, rect.width + 12f, rect.height + 12f)
        batch.setColor(.45f, 1f, .52f, 1f)
        val t = 3f
        batch.draw(assets.solidUiRegion, rect.x, rect.y, rect.width, t)
        batch.draw(assets.solidUiRegion, rect.x, rect.y + rect.height - t, rect.width, t)
        batch.draw(assets.solidUiRegion, rect.x, rect.y, t, rect.height)
        batch.draw(assets.solidUiRegion, rect.x + rect.width - t, rect.y, t, rect.height)
        batch.color = Color.WHITE
    }

    fun helpButton(batch: SpriteBatch, assets: GameAssets, rect: Rectangle) {
        sprite(batch, assets.visualUiArt.region("help"), rect, 1f)
    }

    fun meter(batch: SpriteBatch, assets: GameAssets, rect: Rectangle, value: Int, reveal: Float = 1f) {
        val gap = maxOf(1f, rect.width * .009f)
        val width = (rect.width - gap * (MAX_POWER - 1)) / MAX_POWER
        repeat(MAX_POWER) { i ->
            val lit = i < value.coerceIn(0, MAX_POWER) && i + 1 <= value * reveal
            val x = rect.x + i * (width + gap)
            batch.setColor(.30f, .16f, .07f, 1f)
            batch.draw(assets.solidUiRegion, x, rect.y, width, rect.height)
            batch.setColor(if (lit) .20f else .18f, if (lit) .74f else .20f, if (lit) .92f else .20f, 1f)
            batch.draw(assets.solidUiRegion, x + 1f, rect.y + 1f, (width - 2f).coerceAtLeast(.5f), rect.height - 2f)
            batch.setColor(if (lit) .73f else .35f, if (lit) .97f else .33f, if (lit) 1f else .27f, 1f)
            batch.draw(assets.solidUiRegion, x + 1f, rect.y + rect.height * .63f,
                (width - 2f).coerceAtLeast(.5f), (rect.height * .20f).coerceAtLeast(1f))
        }
        batch.color = Color.WHITE
    }

    fun stats(batch: SpriteBatch, assets: GameAssets, rect: Rectangle, balls: Int, damage: Int,
              reveal: Float = 1f, power: Int = damage) {
        val side = minOf(rect.height * .96f, rect.width * .15f)
        icon(batch, assets, Icon.BALLS, Rectangle(rect.x, rect.y, side, rect.height))
        number(batch, assets, "$balls", Rectangle(rect.x + side, rect.y, rect.width * .18f, rect.height), .72f)
        val start = rect.x + rect.width * .35f
        icon(batch, assets, Icon.HIT, Rectangle(start, rect.y, side, rect.height))
        number(batch, assets, "$damage", Rectangle(start + side, rect.y, rect.width * .13f, rect.height), .72f)
        meter(batch, assets, Rectangle(rect.x + rect.width * .67f, rect.y + rect.height * .24f,
            rect.width * .33f, rect.height * .52f), power, reveal)
    }

    fun modeButton(batch: SpriteBatch, assets: GameAssets, rect: Rectangle, spread: Boolean,
                   selected: Boolean, unlocked: Boolean = true) {
        actionButton(batch, assets, rect, selected)
        icon(batch, assets, if (spread) Icon.SPREAD else Icon.STRAIGHT,
            Rectangle(rect.x + rect.width * .15f, rect.y + rect.height * .07f,
                rect.width * .70f, rect.height * .86f), if (unlocked) 1f else .40f)
        if (selected || !unlocked) icon(batch, assets, if (unlocked) Icon.CHECK else Icon.LOCK,
            Rectangle(rect.x + rect.width * .79f, rect.y + rect.height * .08f,
                rect.width * .17f, rect.height * .38f))
    }
}
