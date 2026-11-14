package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Align
import com.example.brick_breaker_ball.BrickBreakerGame
import com.example.brick_breaker_ball.cosmetics.BallAbilityCatalog
import com.example.brick_breaker_ball.cosmetics.BallAbilityKind
import com.example.brick_breaker_ball.cosmetics.BallAbilityProfile
import com.example.brick_breaker_ball.cosmetics.BallCollectionCatalog
import com.example.brick_breaker_ball.cosmetics.BallCollectionOffer
import com.example.brick_breaker_ball.cosmetics.BallRarity
import com.example.brick_breaker_ball.cosmetics.BallSpriteDefinition
import com.example.brick_breaker_ball.cosmetics.VolleyBallSelection
import com.example.brick_breaker_ball.gameplay.GameplayMode
import kotlin.math.abs

/** Shared collection and Shop inspection surface; all game rules remain in the existing catalogs. */
open class BallShowcaseScreen(
    game: BrickBreakerGame,
    initialBallSpriteName: String? = null,
    private val returnToPausedGame: Boolean = false,
    private val shopReturnDestination: ShopReturnDestination = if (returnToPausedGame)
        ShopReturnDestination.PAUSED_GAME else ShopReturnDestination.MAIN_MENU,
    private val mode: GameplayMode = game.progress.settings.gameplayMode,
    private val onBack: ((BallSpriteDefinition) -> Unit)? = null
) : ForgeScreen(game) {
    private data class Entry(val ball: BallSpriteDefinition, val offer: BallCollectionOffer,
                             val profile: BallAbilityProfile)
    private val entries = VolleyBallSelection.inDisplayOrder(game.assets.cosmetics.balls, mode)
        .map { Entry(it, BallCollectionCatalog.offer(it), BallAbilityCatalog.profileForBall(it)) }
    private val groups = BallRarity.entries.map { rarity -> entries.filter { it.offer.rarity == rarity } }
    private val remembered = IntArray(4)
    private val initial = entries.firstOrNull { it.ball.spriteName == initialBallSpriteName }
        ?: entries.firstOrNull { VolleyBallSelection.isEquipped(game.progress.settings, it.ball) }
        ?: entries.first()
    private var rarity = initial.offer.rarity
    private var index = groups[rarity.ordinal].indexOf(initial).coerceAtLeast(0)
    private val current: Entry get() = groups[rarity.ordinal][index]
    private val preview = BallPowerPreview(game, returnToPausedGame)
    private val layout = BallShowcaseLayout()
    private val art = BallShowcaseArt()
    private val help = VisualHelpOverlay()
    private val background = TextureRegion(game.assets.splash)
    private val heroBackground = TextureRegion(game.assets.splash)
    private val measure = GlyphLayout()
    private val scratch = Rectangle()
    private val pageBounds = Rectangle()
    private val white = Color.valueOf("F2F4FFFF")
    private val muted = Color.valueOf("B4C2D8FF")
    private val ink = Color.valueOf("111827FF")
    private val face = Color.valueOf("283B59FF")
    private val border = Color.valueOf("516783FF")
    private val deep = Color.valueOf("18263EFF")
    private val positive = Color.valueOf("BBEF91FF")
    private val accents = arrayOf(Color.valueOf("8DDAF0FF"), Color.valueOf("91ACFFFF"),
        Color.valueOf("FFD286FF"), Color.valueOf("D7A8FFFF"))
    private val accent: Color get() = accents[rarity.ordinal]
    private val abilityLights = arrayOf(Color.valueOf("9DC9FFFF"), Color.valueOf("FFAC59FF"),
        Color.valueOf("83CFFFFF"), Color.valueOf("B8D9F0FF"), Color.valueOf("B594FFFF"),
        Color.valueOf("FFD589FF"), Color.valueOf("83E7DFFF"))
    private val ambient: Color get() = abilityLights[current.profile.kind.ordinal]
    private val icons = mapOf(
        "balls" to game.assets.visualUiArt.region("balls"),
        "damage" to game.assets.visualUiArt.region("damage"),
        "straight" to game.assets.visualUiArt.region("straight"),
        "spread" to game.assets.visualUiArt.region("spread"),
        "check" to game.assets.visualUiArt.region("check"),
        "help" to game.assets.visualUiArt.region("help"),
        "lock" to requireNotNull(game.assets.arcadeIcons.findRegion("assets_9cd24f93_033")),
        "fire" to requireNotNull(game.assets.arcadeIcons.findRegion("assets_9cd24f93_034")),
        "blast" to requireNotNull(game.assets.arcadeIcons.findRegion("assets_9cd24f93_078")),
        "orb" to requireNotNull(game.assets.arcadeIcons.findRegion("assets_9cd24f93_042")),
        "shield" to requireNotNull(game.assets.arcadeIcons.findRegion("assets_9cd24f93_052")),
        "star" to requireNotNull(game.assets.particles.findRegion("star")),
        "coin" to game.assets.currencyCoinIcon,
        "gem" to game.assets.currencyGemIcon
    )
    private var ballRegion: TextureRegion? = null
    private var owned = false
    private var equipped = false
    private var collectionText = ""
    private var positionText = ""
    private var identityText = ""
    private var statusText = ""
    private var requirement = ""
    private var priceText = ""
    private var abilityText = ""
    private var extraAbility = ""
    private var primaryText = ""
    private var primaryCaption = ""
    private var nextBoostText = ""
    private val statTitles = arrayOf("HIT DAMAGE", "VOLLEY SIZE", "SPECIAL", "BALL POWER")
    private val statIcons = arrayOf("damage", "balls", "blast", "star")
    private val statValues = Array(4) { "" }
    private val statCaptions = Array(4) { "" }
    private var specialIcon = "blast"
    private var cachedCoins = -1
    private var cachedGems = -1
    private var coinsText = ""
    private var gemsText = ""
    private var selectionAge = 1f
    private var transitionDirection = 1f
    private var clock = 0f
    private var touchActive = false
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var heroGesture = false
    private var touchDragged = false

    init { remembered[rarity.ordinal] = index; refresh() }

    override fun show() {
        refresh()
        installMouseWheelHandler { amount ->
            if (help.active || amount == 0f) false else { browse(if (amount > 0f) 1 else -1); true }
        }
    }

    override fun hide() { touchActive = false; removeMouseWheelHandler() }
    override fun pause() { touchActive = false }
    override fun resize(w: Int, h: Int) { super.resize(w, h); touchActive = false }
    override fun dispose() { art.dispose(); super.dispose() }

    private fun available(entry: Entry): Boolean = game.developmentAccess.canUseCosmetic(
        VolleyBallSelection.canUse(mode, entry.ball, game.cosmeticProgression.ownsBall(entry.ball)))

    private fun refresh() {
        val entry = current
        val p = entry.profile
        owned = available(entry)
        equipped = owned && VolleyBallSelection.isEquipped(game.progress.settings, entry.ball)
        ballRegion = game.assets.cosmetics.ballRegion(entry.ball)
        collectionText = "${entries.count(::available)} / ${entries.size} OWNED"
        positionText = "${index + 1} / ${groups[rarity.ordinal].size}  |  SWIPE TO EXPLORE"
        identityText = "${rarity.name}  |  RARITY TIER ${p.tier} / 4"
        statusText = when { equipped -> "EQUIPPED"; owned -> "OWNED"; else -> "LOCKED" }
        priceText = when {
            entry.offer.coins != null -> "${entry.offer.coins} COINS"
            entry.offer.gems != null -> "${entry.offer.gems} GEMS"
            else -> "CAMPAIGN REWARD"
        }
        requirement = entry.offer.stage?.let { "COMPLETE STAGE $it TO UNLOCK" }
            ?: if (entry.offer.playProductId != null) "ALSO AVAILABLE WITH GOOGLE PLAY" else "PERMANENT BALL UNLOCK"
        primaryText = when { equipped -> "EQUIPPED"; owned -> "EQUIP BALL"; canShop() -> "OPEN IN SHOP"; else -> "COMPLETE STAGE ${entry.offer.stage}" }
        primaryCaption = when { equipped -> "READY FOR YOUR NEXT VOLLEY"; owned -> "USE THIS BALL"; canShop() -> priceText; else -> "EARN THIS BALL BY PLAYING" }
        statValues[0] = preview.damage(p).toString()
        statValues[1] = preview.balls(p).toString()
        statValues[3] = "${p.baseHitDamage} / 12"
        statCaptions[0] = "STAGE ${preview.stageNumber}"
        statCaptions[1] = "+${p.startingBalls} FROM BALL"
        statCaptions[3] = "POWER RATING"
        when (p.kind) {
            BallAbilityKind.IMPACT_CORE -> {
                statValues[2] = "+${p.scoreBonusPercent}%"; statCaptions[2] = "BREAK SCORE"; specialIcon = "star"
                abilityText = "Each brick destroyed grants +${p.scoreBonusPercent}% score."
            }
            BallAbilityKind.INFERNO_BURST -> {
                statValues[2] = "${p.triggerEveryHits} HITS"; statCaptions[2] = "FIRE SPLASH"; specialIcon = "fire"
                abilityText = "Every ${p.triggerEveryHits} direct hits, a charged fire hit splashes nearby bricks."
            }
            BallAbilityKind.ARC_CHAIN -> {
                statValues[2] = "${p.triggerEveryHits} HITS"; statCaptions[2] = "CHAIN STRIKE"; specialIcon = "blast"
                abilityText = "Every ${p.triggerEveryHits} direct hits, strike ${p.chainTargets} nearby breakable brick."
            }
            BallAbilityKind.ARMOR_BREAKER, BallAbilityKind.VOID_PHASE -> {
                statValues[2] = "+${p.bonusDamage}"; statCaptions[2] = "EVERY ${p.triggerEveryHits} HITS"
                specialIcon = if (p.kind == BallAbilityKind.VOID_PHASE) "orb" else "shield"
                abilityText = "Every ${p.triggerEveryHits} direct hits, add +${p.bonusDamage} base damage to the struck brick."
            }
            BallAbilityKind.FORTUNE_CORE -> {
                statValues[2] = "+${p.fortuneBonusScore}"; statCaptions[2] = "${p.fortuneEveryBreaks} BRICKS"; specialIcon = "coin"
                abilityText = "Every ${p.fortuneEveryBreaks} bricks destroyed, earn +${p.fortuneBonusScore} score."
            }
            BallAbilityKind.MOMENTUM_CORE -> {
                statValues[2] = "+1 HIT"; statCaptions[2] = "PER ${p.ballsPerDamage} BALLS"; specialIcon = "balls"
                abilityText = "Adds one hit damage for every ${p.ballsPerDamage} balls in the volley."
            }
        }
        extraAbility = "MASTERY +${p.masteryScorePercent}% SCORE" +
            if (p.openingBreakerEveryVolleys > 0) "  |  BREAKER EVERY ${p.openingBreakerEveryVolleys} VOLLEYS" else ""
        nextBoostText = preview.nextAttackStage?.let {
            "${preview.campaignAttack}x > ${preview.campaignAttack + 1}x AT STAGE $it"
        } ?: "${preview.campaignAttack}x | FINAL CAMPAIGN BOOST REACHED"
        statIcons[2] = specialIcon
    }

    private fun canShop(): Boolean = current.offer.coins != null || current.offer.gems != null || current.offer.playProductId != null

    private fun browse(direction: Int) {
        val target = (index + direction).coerceIn(0, groups[rarity.ordinal].lastIndex)
        if (target == index) return
        index = target; remembered[rarity.ordinal] = index
        transitionDirection = direction.toFloat(); selectionAge = 0f
        refresh()
    }

    private fun selectRarity(value: BallRarity) {
        if (value == rarity || groups[value.ordinal].isEmpty()) return
        transitionDirection = if (value.ordinal > rarity.ordinal) 1f else -1f
        rarity = value; index = remembered[value.ordinal].coerceIn(0, groups[value.ordinal].lastIndex)
        selectionAge = 0f; refresh()
    }

    override fun render(delta: Float) {
        val reduced = game.progress.settings.reduceMotion
        val dt = delta.coerceIn(0f, .05f)
        if (!reduced) clock += dt
        selectionAge = if (reduced) 1f else (selectionAge + dt / .24f).coerceAtMost(1f)
        help.update(dt, reduced)
        val insets = safeInsets
        layout.update(visibleLeft + insets.left + 16f, visibleBottom + insets.bottom + 12f,
            (visibleWidth - insets.left - insets.right - 32f).coerceAtLeast(1f),
            (visibleHeight - insets.top - insets.bottom - 24f).coerceAtLeast(1f))
        pageBounds.set(visibleLeft, visibleBottom, visibleWidth, visibleHeight)
        beginStaticBackground(background, .80f)
        drawHeader()
        drawHero(reduced)
        drawStats()
        drawAbility()
        drawProgression()
        drawActions()
        help.focus.set(when (help.page) { 0 -> layout.hero; 1 -> layout.statsRegion; 2 -> layout.modesRegion; else -> layout.primary })
        help.draw(batch, game.assets, pageBounds, reduced, balls = preview.balls(current.profile),
            damage = preview.damage(current.profile), spreadAvailable = current.profile.shotgunAvailable)
        end()
        handleInput()
    }

    private fun drawHeader() {
        panel(layout.back, deep, border, pressed(layout.back))
        chevron(layout.back, false, true)
        text(game.assets.titleFont, "BALLS", part(layout.header, 0f, .28f, 1f, .72f), 47f, white, Align.left)
        text(game.assets.hudLabelFont, collectionText, part(layout.header, 0f, -.02f, 1f, .30f), 21f, muted, Align.left)
        icon("help", layout.help, .84f)
        if (cachedCoins != game.economy.coins) { cachedCoins = game.economy.coins; coinsText = cachedCoins.toString() }
        if (cachedGems != game.economy.gems) { cachedGems = game.economy.gems; gemsText = cachedGems.toString() }
        wallet(layout.coins, "coin", coinsText)
        wallet(layout.gems, "gem", gemsText)
        BallRarity.entries.forEach { value ->
            val rect = layout.tabs[value.ordinal]
            val selected = value == rarity
            panel(rect, if (selected) face else deep, if (selected) accents[value.ordinal] else border, pressed(rect))
            text(game.assets.hudLabelFont, value.name, rect, if (layout.wide) 26f else 23f,
                if (selected) accents[value.ordinal] else muted)
        }
    }

    private fun wallet(rect: Rectangle, key: String, value: String) {
        panel(rect, deep, border)
        icon(key, part(rect, .05f, .15f, .20f, .70f))
        text(game.assets.hudValueFont, value, part(rect, .28f, .05f, .66f, .90f), 29f, white)
    }

    private fun drawHero(reduced: Boolean) {
        panel(layout.hero, deep, accent)
        // Reuse the forest illustration, cropped to the hero area. It stays behind the ball.
        val h = layout.hero
        val margin = 20f * layout.scale
        val targetRatio = (h.width - margin * 2f) / (h.height - margin * 2f)
        val sourceWidth = game.assets.splash.width
        val sourceHeight = game.assets.splash.height
        val cropHeight = minOf(sourceHeight, (sourceWidth / targetRatio).toInt())
        val cropWidth = minOf(sourceWidth, (sourceHeight * targetRatio).toInt())
        heroBackground.setRegion((sourceWidth - cropWidth) / 2, (sourceHeight - cropHeight) / 2, cropWidth, cropHeight)
        batch.setColor(.31f, .38f, .53f, .57f)
        batch.draw(heroBackground, h.x + margin, h.y + margin, h.width - margin * 2f, h.height - margin * 2f)
        batch.setColor(ambient.r, ambient.g, ambient.b, .17f)
        batch.draw(game.assets.softGlowRegion, h.x + h.width * .09f, h.y + h.height * .14f, h.width * .82f, h.height * .82f)
        batch.color = Color.WHITE
        // Transparent radial light and an elliptical platform; no opaque glow atlas or square shadow.
        val ball = layout.ball
        val reveal = 1f - (1f - selectionAge) * (1f - selectionAge)
        val drift = if (reduced) 0f else MathUtils.sin(clock * 1.8f) * 5f * layout.scale
        val slide = (1f - reveal) * transitionDirection * 42f * layout.scale
        batch.setColor(ambient.r, ambient.g, ambient.b, .22f)
        batch.draw(game.assets.softGlowRegion, ball.x - ball.width * .27f, ball.y - ball.height * .27f,
            ball.width * 1.54f, ball.height * 1.54f)
        batch.color = Color.WHITE
        art.ring(batch, layout.pedestal, accent, .72f)
        ballRegion?.let { region ->
            val ratio = minOf(ball.width / region.regionWidth, ball.height / region.regionHeight)
            val w = region.regionWidth * ratio; val height = region.regionHeight * ratio
            batch.setColor(1f, 1f, 1f, .45f + .55f * reveal)
            batch.draw(region, ball.x + (ball.width - w) / 2f + slide,
                ball.y + (ball.height - height) / 2f + drift, w, height)
            batch.color = Color.WHITE
        }
        panel(layout.status, deep, if (owned) accent else border)
        icon(if (owned) "check" else "lock", part(layout.status, .06f, .14f, .17f, .72f))
        text(game.assets.hudLabelFont, statusText, part(layout.status, .25f, 0f, .70f, 1f), 23f, if (owned) accent else muted)
        arrow(layout.previous, false, index > 0)
        arrow(layout.next, true, index < groups[rarity.ordinal].lastIndex)
        panel(part(h, .04f, .012f, .92f, .275f), deep, deep, opacity = .92f)
        text(game.assets.titleFont, current.ball.name.uppercase(), layout.name, 47f, white)
        text(game.assets.hudLabelFont, identityText, layout.identity, 24f, accent)
        text(game.assets.hudLabelFont, positionText, layout.position, 20f, muted)
    }

    private fun arrow(rect: Rectangle, right: Boolean, enabled: Boolean) {
        panel(rect, deep, border, enabled && pressed(rect), opacity = if (enabled) .92f else .36f)
        chevron(rect, right, enabled)
    }

    private fun chevron(rect: Rectangle, right: Boolean, enabled: Boolean) {
        val cx = rect.x + rect.width / 2f
        val cy = rect.y + rect.height / 2f
        val halfW = 10f * layout.scale
        val halfH = 17f * layout.scale
        val direction = if (right) 1f else -1f
        batch.color = if (enabled) white else muted
        line(cx - halfW * direction, cy + halfH, cx + halfW * direction, cy)
        line(cx + halfW * direction, cy, cx - halfW * direction, cy - halfH)
        batch.color = Color.WHITE
    }

    private fun line(x1: Float, y1: Float, x2: Float, y2: Float) {
        val dx = x2 - x1; val dy = y2 - y1
        val thickness = 5f * layout.scale
        batch.draw(game.assets.solidUiRegion, x1, y1 - thickness / 2f, 0f, thickness / 2f,
            kotlin.math.sqrt(dx * dx + dy * dy), thickness, 1f, 1f,
            MathUtils.atan2(dy, dx) * MathUtils.radiansToDegrees)
    }

    private fun drawStats() {
        layout.stats.forEachIndexed { i, rect ->
            panel(rect, face, border)
            if (layout.wide) {
                icon(statIcons[i], part(rect, .04f, .25f, .20f, .58f))
                text(game.assets.hudLabelFont, statTitles[i], part(rect, .27f, .69f, .69f, .23f), 20f, muted)
                text(game.assets.hudValueFont, statValues[i], part(rect, .27f, .29f, .69f, .39f), 34f, white)
                text(game.assets.hudLabelFont, statCaptions[i], part(rect, .06f, .03f, .88f, .22f), 18f, accent)
            } else {
                icon(statIcons[i], part(rect, .30f, .65f, .40f, .28f))
                text(game.assets.hudLabelFont, statTitles[i], part(rect, .06f, .49f, .88f, .16f), 22f, muted)
                text(game.assets.hudValueFont, statValues[i], part(rect, .05f, .23f, .90f, .24f), 42f, white)
                text(game.assets.hudLabelFont, statCaptions[i], part(rect, .06f, .055f, .88f, .16f), 18f, accent, wrap = true)
            }
            if (i == 3) powerMeter(part(rect, .10f, if (layout.wide) .015f else .018f, .80f, .025f))
        }
    }

    private fun powerMeter(rect: Rectangle) {
        val gap = 3f * layout.scale
        val width = (rect.width - gap * 11f) / 12f
        repeat(12) { i ->
            batch.color = if (i < current.profile.baseHitDamage) accent else border
            batch.draw(game.assets.solidUiRegion, rect.x + i * (width + gap), rect.y, width, rect.height)
        }
        batch.color = Color.WHITE
    }

    private fun drawAbility() {
        val rect = layout.ability
        panel(rect, deep, border)
        icon(specialIcon, part(rect, .025f, .26f, .105f, .58f))
        text(game.assets.hudLabelFont, current.profile.title, part(rect, .15f, .70f, .81f, .23f), 25f, accent, Align.left)
        text(game.assets.hudLabelFont, abilityText, part(rect, .15f, .24f, .81f, .44f), 24f, white, Align.left, true)
        text(game.assets.hudLabelFont, extraAbility, part(rect, .15f, .04f, .81f, .18f), 17f, muted, Align.left)
    }

    private fun drawProgression() {
        val rect = layout.progression
        panel(rect, deep, border)
        if (owned) {
            text(game.assets.hudLabelFont, "CAMPAIGN ATTACK | STAGE ${preview.stageNumber}", part(rect, .04f, .57f, .92f, .30f), 23f, muted, Align.left)
            text(game.assets.hudValueFont, nextBoostText, part(rect, .04f, .19f, .92f, .33f), 27f, positive, Align.left)
            val target = preview.nextAttackStage
            val fraction = if (target == null) 1f else ((preview.stageNumber - (target - 12)).toFloat() / 12f).coerceIn(0f, 1f)
            val track = part(rect, .04f, .07f, .92f, .035f)
            batch.color = border; batch.draw(game.assets.solidUiRegion, track.x, track.y, track.width, track.height)
            batch.color = accent; batch.draw(game.assets.solidUiRegion, track.x, track.y, track.width * fraction, track.height)
            batch.color = Color.WHITE
        } else {
            icon(if (current.offer.gems != null) "gem" else "coin", part(rect, .03f, .21f, .09f, .58f))
            text(game.assets.hudValueFont, priceText, part(rect, .14f, .51f, .82f, .37f), 32f, accent, Align.left)
            text(game.assets.hudLabelFont, (if (current.offer.stage != null && canShop()) "OR " else "") + requirement,
                part(rect, .14f, .12f, .82f, .31f), 23f, muted, Align.left)
        }
    }

    private fun drawActions() {
        val active = !equipped && (owned || canShop())
        panel(layout.primary, if (active) accent else face, if (active) accent else border, active && pressed(layout.primary))
        val primaryIcon = if (owned) "check" else if (!canShop()) "lock"
            else if (current.offer.gems != null) "gem" else "coin"
        icon(primaryIcon, part(layout.primary, .035f, .22f, .085f, .60f))
        text(game.assets.hudLabelFont, primaryText, part(layout.primary, .13f, .42f, .82f, .40f), 34f, if (active) ink else white)
        text(game.assets.hudLabelFont, primaryCaption, part(layout.primary, .13f, .13f, .82f, .23f), 20f, if (active) ink else muted)
        val spread = owned && current.profile.shotgunAvailable && game.progress.isShotgunEnabled(current.ball.spriteName)
        modeButton(layout.straight, "straight", "STRAIGHT", owned && !spread, owned)
        modeButton(layout.spread, "spread", if (current.profile.shotgunAvailable) "SPREAD" else "SPREAD LOCKED",
            spread, owned && current.profile.shotgunAvailable)
    }

    private fun modeButton(rect: Rectangle, key: String, label: String, selected: Boolean, enabled: Boolean) {
        panel(rect, if (selected) face else deep, if (selected) accent else border, enabled && pressed(rect))
        icon(key, part(rect, .05f, .14f, .23f, .72f), if (enabled) 1f else .36f)
        text(game.assets.hudLabelFont, label, part(rect, .30f, .15f, .62f, .70f), 24f, if (enabled) white else muted)
        if (selected || !enabled) icon(if (selected) "check" else "lock", part(rect, .86f, .10f, .105f, .30f))
    }

    private fun handleInput() {
        if (help.active) {
            touchActive = false
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) help.tap(true)
            else if (Gdx.input.justTouched()) {
                val touch = touchPoint()
                help.tap(VisualHelpOverlay.closeBounds(pageBounds).contains(touch.x, touch.y))
            }
            return
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) { leave(); return }
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) browse(-1)
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) browse(1)
        val touch = touchPoint()
        if (Gdx.input.isTouched(1)) { touchActive = false; return }
        if (Gdx.input.justTouched() && Gdx.input.isTouched(0)) {
            touchStartX = touch.x; touchStartY = touch.y; touchActive = true
            touchDragged = false
            heroGesture = layout.hero.contains(touch.x, touch.y)
        } else if (touchActive && Gdx.input.isTouched(0)) {
            if (abs(touch.x - touchStartX) >= 24f * layout.scale || abs(touch.y - touchStartY) >= 24f * layout.scale) touchDragged = true
        } else if (touchActive && !Gdx.input.isTouched(0)) {
            touchActive = false
            val dx = touch.x - touchStartX; val dy = touch.y - touchStartY
            if (heroGesture && abs(dx) >= 78f * layout.scale && abs(dx) > abs(dy) * 1.3f) {
                browse(if (dx < 0f) 1 else -1)
            } else if (!touchDragged && abs(dx) < 24f * layout.scale && abs(dy) < 24f * layout.scale) {
                activate(touch.x, touch.y)
            }
        }
    }

    private fun activate(x: Float, y: Float) {
        when {
            layout.contains(layout.back, x, y) -> leave()
            layout.contains(layout.help, x, y) -> help.open(VisualHelpOverlay.Topic.BALLS)
            layout.tabs.any { layout.contains(it, x, y) } -> selectRarity(BallRarity.entries[layout.tabs.indexOfFirst { layout.contains(it, x, y) }])
            layout.contains(layout.previous, x, y) -> browse(-1)
            layout.contains(layout.next, x, y) -> browse(1)
            owned && layout.contains(layout.straight, x, y) -> game.progress.setShotgunEnabled(current.ball.spriteName, false)
            owned && current.profile.shotgunAvailable && layout.contains(layout.spread, x, y) -> game.progress.setShotgunEnabled(current.ball.spriteName, true)
            layout.contains(layout.primary, x, y) -> {
                if (owned && !equipped) {
                    VolleyBallSelection.equip(game.progress.settings, current.ball)
                    game.progress.saveSettings(); refresh()
                    game.uiFeedback.emit(UiFeedbackKind.CONFIRM)
                } else if (!owned && canShop()) {
                    game.navigate(ShopScreen(game, shopReturnDestination, mode, "BALLS", current.ball.spriteName))
                }
            }
        }
    }

    private fun leave() {
        touchActive = false
        if (onBack != null) onBack.invoke(current.ball)
        else if (returnToPausedGame) game.resumePausedGame(showLoading = false) else game.openMenu()
    }

    private fun pressed(rect: Rectangle): Boolean {
        if (!touchActive || touchDragged || help.active || !Gdx.input.isTouched(0)) return false
        val touch = touchPoint()
        return abs(touch.x - touchStartX) < 24f * layout.scale && abs(touch.y - touchStartY) < 24f * layout.scale &&
            layout.contains(rect, touchStartX, touchStartY) && layout.contains(rect, touch.x, touch.y)
    }

    private fun panel(rect: Rectangle, fill: Color, outline: Color, down: Boolean = false, opacity: Float = 1f) =
        art.panel(batch, rect, fill, outline, layout.scale, down, opacity)

    private fun part(rect: Rectangle, x: Float, y: Float, w: Float, h: Float): Rectangle =
        scratch.set(rect.x + rect.width * x, rect.y + rect.height * y, rect.width * w, rect.height * h)

    private fun icon(key: String, rect: Rectangle, opacity: Float = 1f) {
        val region = icons.getValue(key)
        val ratio = minOf(rect.width / region.regionWidth, rect.height / region.regionHeight)
        val w = region.regionWidth * ratio; val h = region.regionHeight * ratio
        batch.setColor(1f, 1f, 1f, opacity)
        batch.draw(region, rect.x + (rect.width - w) / 2f, rect.y + (rect.height - h) / 2f, w, h)
        batch.color = Color.WHITE
    }

    private fun text(font: BitmapFont, value: String, rect: Rectangle, size: Float, color: Color,
                     align: Int = Align.center, wrap: Boolean = false) {
        val oldX = font.data.scaleX; val oldY = font.data.scaleY
        val r = font.color.r; val g = font.color.g; val b = font.color.b; val a = font.color.a
        val baseHeight = font.data.capHeight / oldY
        val desired = size * layout.scale / baseHeight
        font.data.setScale(desired)
        measure.setText(font, value, color, rect.width, align, wrap)
        val fit = minOf(1f, rect.width / measure.width.coerceAtLeast(1f), rect.height * .94f / measure.height.coerceAtLeast(1f))
        if (fit < 1f) {
            font.data.setScale(desired * fit)
            measure.setText(font, value, color, rect.width, align, wrap)
        }
        font.draw(batch, measure, rect.x, rect.y + (rect.height + measure.height) / 2f)
        font.data.setScale(oldX, oldY); font.setColor(r, g, b, a)
    }
}
