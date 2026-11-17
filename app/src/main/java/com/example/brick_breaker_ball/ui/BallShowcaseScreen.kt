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

/** Shared collection and Shop inspection; rules, prices and saves remain in their existing owners. */
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
    private enum class InfoTopic { OVERVIEW, DAMAGE, BALLS, ABILITY, POWER, CAMPAIGN, SHOT }
    private val entries = VolleyBallSelection.inDisplayOrder(game.assets.cosmetics.balls, mode)
        .map { Entry(it, BallCollectionCatalog.offer(it), BallAbilityCatalog.profileForBall(it)) }
    private val groups = BallRarity.entries.map { value -> entries.filter { it.offer.rarity == value } }
    private val remembered = IntArray(4)
    private val initial = entries.firstOrNull { it.ball.spriteName == initialBallSpriteName }
        ?: entries.firstOrNull { VolleyBallSelection.isEquipped(game.progress.settings, it.ball) }
        ?: entries.first()
    private var rarity = initial.offer.rarity
    private var index = groups[rarity.ordinal].indexOf(initial).coerceAtLeast(0)
    private val current: Entry get() = groups[rarity.ordinal][index]
    private val preview = BallPowerPreview(game, returnToPausedGame)
    private val showShotPatterns: Boolean get() = !preview.usesReferenceCombat && current.profile.shotgunAvailable
    private val layout = BallShowcaseLayout()
    private val art = BallShowcaseArt(game.assets)
    private val measure = GlyphLayout()
    private val scratch = Rectangle()
    private val cream = Color.valueOf("FFF0CCFF")
    private val muted = Color.valueOf("D1BD99FF")
    private val gold = Color.valueOf("F4CD78FF")
    private val track = Color.valueOf("3B3027FF")
    private val positive = Color.valueOf("D5F1A1FF")
    private val accents = arrayOf(Color.valueOf("D5CBB4FF"), Color.valueOf("89C7D1FF"),
        Color.valueOf("F4C46FFF"), Color.valueOf("CF9FE2FF"))
    private val accent: Color get() = accents[rarity.ordinal]
    private val abilityLights = arrayOf(Color.valueOf("9DC9FFFF"), Color.valueOf("FFAC59FF"),
        Color.valueOf("83CFFFFF"), Color.valueOf("B8D9F0FF"), Color.valueOf("B594FFFF"),
        Color.valueOf("FFD589FF"), Color.valueOf("83E7DFFF"))
    private val ambient: Color get() = if (preview.usesReferenceCombat) accent else abilityLights[current.profile.kind.ordinal]
    private val arrowRegion = game.assets.boards2.requireRegion("orange_back_arrow_button")
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
    private var positionText = ""
    private var ballName = ""
    private var unlockText = ""
    private var priceText = ""
    private var primaryText = ""
    private var campaignText = ""
    private var campaignMultiplierText = ""
    private val statValues = Array(3) { "" }
    private val statUnits = arrayOf("HIT", "TOTAL", "")
    private var abilityCadence = ""
    private var abilityText = ""
    private var specialIcon = "blast"
    private var infoTopic: InfoTopic? = null
    private var informationTitle = ""
    private var informationBody = ""
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
    private var touchHeld = 0f

    init { remembered[rarity.ordinal] = index; refresh() }

    override fun show() {
        refresh()
        installMouseWheelHandler { amount ->
            if (infoTopic != null || amount == 0f) false else { browse(if (amount > 0f) 1 else -1); true }
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
        ballName = entry.ball.name.uppercase()
        positionText = "${index + 1} / ${groups[rarity.ordinal].size}"
        priceText = (entry.offer.coins ?: entry.offer.gems)?.toString() ?: ""
        unlockText = if (!owned && entry.offer.stage != null)
            "${if (canShop()) "OR " else ""}STAGE ${entry.offer.stage}" else ""
        primaryText = when { equipped -> "EQUIPPED"; owned -> "EQUIP"; canShop() -> "UNLOCK"; else -> "STAGE ${entry.offer.stage}" }
        statValues[0] = preview.damage(p).toString()
        statValues[1] = preview.balls(p).toString()
        abilityText = p.description
        abilityCadence = ""
        if (preview.usesReferenceCombat) {
            statValues[2] = preview.playerLevel.toString()
            statUnits[2] = if (returnToPausedGame) "RUN LEVEL" else "LEVEL"
            specialIcon = "star"
            abilityText = "Ball appearances do not change campaign damage or volley size. Your player level and equipped ORBs determine those values."
        } else when (p.kind) {
            BallAbilityKind.IMPACT_CORE -> {
                statValues[2] = "+${p.scoreBonusPercent}%"; statUnits[2] = "SCORE"; specialIcon = "star"
            }
            BallAbilityKind.INFERNO_BURST, BallAbilityKind.ARC_CHAIN -> {
                statValues[2] = p.triggerEveryHits.toString(); statUnits[2] = "HITS"
                specialIcon = if (p.kind == BallAbilityKind.INFERNO_BURST) "fire" else "blast"
            }
            BallAbilityKind.ARMOR_BREAKER, BallAbilityKind.VOID_PHASE -> {
                statValues[2] = "+${p.bonusDamage}"; statUnits[2] = "BASE HIT"
                abilityCadence = "${p.triggerEveryHits} HITS"
                specialIcon = if (p.kind == BallAbilityKind.VOID_PHASE) "orb" else "shield"
            }
            BallAbilityKind.FORTUNE_CORE -> {
                statValues[2] = "+${p.fortuneBonusScore}"; statUnits[2] = "SCORE"
                abilityCadence = "${p.fortuneEveryBreaks} BRICKS"; specialIcon = "star"
            }
            BallAbilityKind.MOMENTUM_CORE -> {
                statValues[2] = "+1"; statUnits[2] = "BASE HIT"
                abilityCadence = "${p.ballsPerDamage} BALLS"; specialIcon = "balls"
            }
        }
        campaignText = if (preview.usesReferenceCombat) "${preview.playerExperience} / ${preview.nextLevelExperience} XP"
            else preview.nextAttackStage?.let { "${preview.stageNumber} / $it" } ?: "MAX"
        campaignMultiplierText = if (preview.usesReferenceCombat) "XP" else "${preview.campaignAttack}x"
    }

    private fun canShop(): Boolean = current.offer.coins != null || current.offer.gems != null || current.offer.playProductId != null

    private fun browse(direction: Int) {
        val target = (index + direction).coerceIn(0, groups[rarity.ordinal].lastIndex)
        if (target == index) return
        index = target; remembered[rarity.ordinal] = index
        transitionDirection = direction.toFloat(); selectionAge = 0f; refresh()
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
        val insets = safeInsets
        layout.update(visibleLeft + insets.left + 16f, visibleBottom + insets.bottom + 12f,
            (visibleWidth - insets.left - insets.right - 32f).coerceAtLeast(1f),
            (visibleHeight - insets.top - insets.bottom - 24f).coerceAtLeast(1f), showShotPatterns)
        beginStaticBackground(game.assets.worldMapImage(1, mode), .64f)
        drawHeader()
        drawHero(reduced)
        art.tray(batch, layout.tray, layout.scale)
        drawStats()
        drawProgression()
        drawActions()
        if (infoTopic != null) drawInformation()
        end()
        handleInput()
    }

    private fun drawHeader() {
        arrow(layout.back, false, true)
        art.title(batch, layout.header, layout.scale)
        text(game.assets.titleFont, "BALLS", part(layout.header, .10f, .13f, .80f, .74f), 47f, cream)
        icon("help", layout.help, if (pressed(layout.help)) .75f else 1f)
        if (cachedCoins != game.economy.coins) { cachedCoins = game.economy.coins; coinsText = cachedCoins.toString() }
        if (cachedGems != game.economy.gems) { cachedGems = game.economy.gems; gemsText = cachedGems.toString() }
        wallet(layout.coins, "coin", coinsText); wallet(layout.gems, "gem", gemsText)
        BallRarity.entries.forEach { value ->
            val rect = layout.tabs[value.ordinal]; val selected = value == rarity
            art.stone(batch, rect, layout.scale, pressed(rect), selected)
            text(game.assets.hudLabelFont, value.name, part(rect, .07f, .20f, .86f, .65f),
                if (layout.wide) 26f else 23f, if (selected) cream else muted)
            if (selected) {
                val mark = part(rect, .24f, .13f, .52f, .035f)
                batch.color = accents[value.ordinal]
                batch.draw(game.assets.solidUiRegion, mark.x, mark.y, mark.width, mark.height)
                batch.color = Color.WHITE
            }
        }
    }

    private fun wallet(rect: Rectangle, key: String, value: String) {
        art.stone(batch, rect, layout.scale)
        icon(key, part(rect, .05f, .15f, .20f, .70f))
        text(game.assets.hudValueFont, value, part(rect, .28f, .12f, .63f, .76f), 29f, cream)
    }

    private fun drawHero(reduced: Boolean) {
        val ball = layout.ball
        val reveal = 1f - (1f - selectionAge) * (1f - selectionAge)
        val drift = if (reduced) 0f else MathUtils.sin(clock * 1.8f) * 5f * layout.scale
        val slide = (1f - reveal) * transitionDirection * 42f * layout.scale
        // Smooth alpha light only: never the opaque atlas glow that produced square shadows.
        batch.setColor(ambient.r, ambient.g, ambient.b, .24f)
        batch.draw(game.assets.softGlowRegion, ball.x - ball.width * .30f, ball.y - ball.height * .30f,
            ball.width * 1.60f, ball.height * 1.60f)
        batch.color = Color.WHITE
        art.ring(batch, layout.pedestal, accent)
        ballRegion?.let { region ->
            val ratio = minOf(ball.width / region.regionWidth, ball.height / region.regionHeight)
            val w = region.regionWidth * ratio; val h = region.regionHeight * ratio
            batch.setColor(1f, 1f, 1f, .45f + .55f * reveal)
            batch.draw(region, ball.x + (ball.width - w) / 2f + slide,
                ball.y + (ball.height - h) / 2f + drift, w, h)
            batch.color = Color.WHITE
        }
        arrow(layout.previous, false, index > 0)
        arrow(layout.next, true, index < groups[rarity.ordinal].lastIndex)
        text(game.assets.titleFont, ballName, layout.name, 47f, cream)
        icon("star", part(layout.identity, 0f, .02f, .10f, .96f))
        if (preview.usesReferenceCombat)
            text(game.assets.hudLabelFont, rarity.name, part(layout.identity, .15f, .02f, .85f, .96f), 21f, accent)
        else powerMeter(part(layout.identity, .15f, .28f, .85f, .44f))
        text(game.assets.hudLabelFont, positionText, layout.position, 20f, muted)
        // Keep the preview's ownership state visible while browsing, beside its identity.
        val status = Rectangle(layout.hero.x + layout.hero.width - 166f * layout.scale,
            layout.identity.y - 2f * layout.scale, 128f * layout.scale, 34f * layout.scale)
        text(game.assets.hudLabelFont, when {
            preview.usesReferenceCombat -> ""
            equipped -> "EQUIPPED"
            owned -> "OWNED"
            else -> "LOCKED"
        }, status, 19f, if (equipped) positive else if (owned) cream else muted)
    }

    private fun arrow(rect: Rectangle, right: Boolean, enabled: Boolean) {
        val ratio = minOf(rect.width / arrowRegion.regionWidth, rect.height / arrowRegion.regionHeight) * .88f
        val w = arrowRegion.regionWidth * ratio; val h = arrowRegion.regionHeight * ratio
        val x = rect.x + (rect.width - w) / 2f; val y = rect.y + (rect.height - h) / 2f
        val dim = if (enabled && pressed(rect)) .76f else 1f
        batch.setColor(dim, dim, dim, if (enabled) 1f else .25f)
        // Mirror the draw, never mutate the atlas region shared with other screens.
        batch.draw(arrowRegion, if (right) x + w else x, y, if (right) -w else w, h)
        batch.color = Color.WHITE
    }

    private fun drawStats() {
        layout.stats.forEachIndexed { i, rect ->
            val key = when (i) { 0 -> "damage"; 1 -> "balls"; else -> specialIcon }
            icon(key, part(rect, .31f, .61f, .38f, .35f), if (pressed(rect)) .75f else 1f)
            text(game.assets.hudValueFont, statValues[i], part(rect, .04f, .29f, .92f, .30f), 48f, cream)
            text(game.assets.hudLabelFont, statUnits[i], part(rect, .04f, .13f, .92f, .15f), 21f, gold)
            if (i == 2 && abilityCadence.isNotEmpty())
                text(game.assets.hudLabelFont, abilityCadence, part(rect, .03f, -.03f, .94f, .14f), 17f, muted)
        }
        if (unlockText.isNotEmpty()) text(game.assets.hudLabelFont, unlockText, layout.unlockHint, 23f, gold)
    }

    private fun powerMeter(rect: Rectangle) {
        val gap = 4f * layout.scale; val width = (rect.width - gap * 11f) / 12f
        repeat(12) { i ->
            batch.color = if (i < current.profile.baseHitDamage) accent else track
            batch.draw(game.assets.solidUiRegion, rect.x + i * (width + gap), rect.y, width, rect.height)
        }
        batch.color = Color.WHITE
    }

    private fun drawProgression() {
        val rect = layout.progression
        icon(if (preview.usesReferenceCombat) "star" else "damage", part(rect, 0f, .23f, .09f, .70f))
        text(game.assets.hudValueFont, campaignMultiplierText, part(rect, .10f, .22f, .18f, .70f), 31f, positive)
        text(game.assets.hudLabelFont, campaignText, part(rect, .36f, .29f, .52f, .54f), 22f, muted)
        icon("help", part(rect, .92f, .32f, .07f, .51f))
        val target = preview.nextAttackStage
        val fraction = if (preview.usesReferenceCombat)
            (preview.playerExperience.toDouble() / preview.nextLevelExperience.toDouble()).coerceIn(0.0, 1.0).toFloat()
        else if (target == null) 1f else ((preview.stageNumber - (target - 12)).toFloat() / 12f).coerceIn(0f, 1f)
        val bar = part(rect, .35f, .10f, .54f, .07f)
        batch.color = track; batch.draw(game.assets.solidUiRegion, bar.x, bar.y, bar.width, bar.height)
        batch.color = gold; batch.draw(game.assets.solidUiRegion, bar.x, bar.y, bar.width * fraction, bar.height)
        batch.color = Color.WHITE
    }

    private fun drawActions() {
        val active = !equipped && (owned || canShop())
        art.action(batch, layout.primary, layout.scale, active, active && pressed(layout.primary))
        val price = !owned && canShop() && priceText.isNotEmpty()
        icon(if (equipped) "check" else if (owned) "balls" else "lock", part(layout.primary, .06f, .20f, .10f, .60f))
        text(game.assets.hudLabelFont, primaryText, part(layout.primary, .18f, .18f, if (price) .38f else .72f, .64f),
            34f, cream)
        if (price) {
            icon(if (current.offer.gems != null) "gem" else "coin", part(layout.primary, .59f, .25f, .10f, .50f))
            text(game.assets.hudValueFont, priceText, part(layout.primary, .71f, .20f, .22f, .60f), 30f, cream)
        }
        if (showShotPatterns) {
            val spread = game.progress.isShotgunEnabled(current.ball.spriteName)
            modeButton(layout.straight, "straight", owned && !spread)
            modeButton(layout.spread, "spread", owned && spread)
        }
    }

    private fun modeButton(rect: Rectangle, key: String, selected: Boolean) {
        art.stone(batch, rect, layout.scale, pressed(rect), selected)
        icon(key, part(rect, .14f, .19f, .72f, .69f), if (owned) 1f else .45f)
        if (selected) {
            val pip = part(rect, .36f, .10f, .28f, .05f)
            batch.color = gold; batch.draw(game.assets.solidUiRegion, pip.x, pip.y, pip.width, pip.height)
            batch.color = Color.WHITE
        }
    }

    private fun openInformation(topic: InfoTopic) {
        touchActive = false; infoTopic = topic
        val p = current.profile
        val offer = current.offer
        val unlock = if (owned) "This ball is permanently available. Equip it with the main button."
            else listOfNotNull(offer.coins?.let { "$it coins" }, offer.gems?.let { "$it gems" },
                offer.stage?.let { "complete campaign stage $it" }, offer.playProductId?.let { "Google Play purchase" })
                .joinToString(" OR ") + ". UNLOCK opens the focused Shop; it does not spend currency here."
        informationTitle = when (topic) {
            InfoTopic.OVERVIEW -> "BALL GUIDE"; InfoTopic.DAMAGE -> "HIT DAMAGE"; InfoTopic.BALLS -> "VOLLEY SIZE"
            InfoTopic.ABILITY -> if (preview.usesReferenceCombat) "PLAYER LEVEL" else p.title
            InfoTopic.POWER -> if (preview.usesReferenceCombat) "BALL APPEARANCE" else "BALL STRENGTH"
            InfoTopic.CAMPAIGN -> if (preview.usesReferenceCombat) "PLAYER EXPERIENCE" else "CAMPAIGN ATTACK"
            InfoTopic.SHOT -> "SHOT PATTERN"
        }
        informationBody = if (preview.usesReferenceCombat) when (topic) {
            InfoTopic.OVERVIEW -> "${current.ball.name} / ${rarity.name}\n\n" +
                "Choose your ball's appearance here. Player level and equipped ORBs determine campaign strength; rarity does not add damage or balls.\n\n" +
                "Tap a statistic for details. Swipe the ball or use the arrows to browse. " +
                "\n\n" + unlock
            InfoTopic.DAMAGE -> "${statValues[0]} standard damage per hit.\n\n" +
                "Calculated from player level ${preview.playerLevel} and equipped ORBs, including saved run damage bonuses when viewing a paused game.\n\n" +
                "Critical hits, temporary effects, enemy armor and boss protection are resolved during play. Changing the ball's appearance does not increase damage."
            InfoTopic.BALLS -> "${statValues[1]} TOTAL balls ${if (returnToPausedGame) "in the paused volley" else "at the start of a new run"}.\n\n" +
                "The starting total comes from player level and equipped ORBs. Pickups can change it during a run. This appearance contributes no extra balls."
            InfoTopic.ABILITY -> "Player level ${preview.playerLevel}.\n\n" +
                "Defeat campaign enemies to earn XP. Player level is independent of stage number and ball rarity. Equipped ORBs provide combat abilities.\n\n" +
                "The damage and total-ball statistics show your current campaign baseline."
            InfoTopic.POWER -> "${rarity.name} appearance.\n\n" +
                "Rarity describes the collectible appearance, not an attack multiplier or an upgrade level. All ball appearances use the same player and ORB combat rules.\n\n" + unlock
            InfoTopic.CAMPAIGN -> "Account level ${preview.accountPlayerLevel}: ${preview.playerExperience} / ${preview.nextLevelExperience} XP toward the next player level.\n\n" +
                (if (returnToPausedGame) "This paused round retains its starting player level ${preview.playerLevel}.\n\n" else "") +
                "XP comes from defeated campaign enemies. Player XP is separate from stage progress and collectible rarity.\n\n" +
                (if (returnToPausedGame) "A resumed run keeps the player level recorded when it began; new level bonuses apply to your next run." else
                    "Damage and starting balls are recalculated from player level and equipped ORBs when a new run begins.")
            InfoTopic.SHOT -> "Campaign volleys follow your aim.\n\n" +
                "Ball appearances do not change the firing pattern. Equipped ORBs provide supported combat effects."
        } else when (topic) {
            InfoTopic.OVERVIEW -> "${current.ball.name} / ${rarity.name}\n\n" +
                "Tap the hammer, balls, special ability or strength pips for their meaning. The small attack bar tracks campaign milestones.\n\n" +
                "Swipe the ball or use the arrows to browse. ${if (p.shotgunAvailable) "The two small buttons choose Straight or Spread; hold either for help.\n\n" else ""}" + unlock
            InfoTopic.DAMAGE -> "${statValues[0]} standard damage per hit at stage ${preview.stageNumber}.\n\n" +
                "Ball base hit: ${p.baseHitDamage}. Campaign attack: ${preview.campaignAttack}x. This preview includes Momentum and saved flat damage or active Mega when applicable.\n\n" +
                "It is a standard-hit baseline, not a guaranteed value against every target. Ability triggers, ORB effects, armor and boss protection are resolved during play."
            InfoTopic.BALLS -> "${statValues[1]} TOTAL balls in the preview volley.\n\n" +
                "This ball contributes +${p.startingBalls} starting balls. The total also includes the stage allowance, equipped ORBs and saved run changes when returning from a paused game.\n\n" +
                "The total is capped at 160. It is not the number added by this ball alone."
            InfoTopic.ABILITY -> "$abilityText\n\n" +
                "Mastery: +${p.masteryScorePercent}% score. Score bonuses award points; they do not increase hit damage or wallet currency." +
                (if (p.openingBreakerEveryVolleys > 0) "\n\nOpening breaker: every ${p.openingBreakerEveryVolleys} volleys." else "")
            InfoTopic.POWER -> "${p.baseHitDamage} / 12 base strength.\n\n" +
                "The pips compare this ball's innate hit strength across the collection. They are not an upgrade level or upgrade progress.\n\n" +
                "${rarity.name} is its rarity. Stage attack and ORB effects are separate. This game has no individual ball upgrade purchase."
            InfoTopic.CAMPAIGN -> "Stage ${preview.stageNumber} / ${preview.campaignAttack}x attack.\n\n" +
                (preview.nextAttackStage?.let { "Next: ${preview.campaignAttack + 1}x at stage $it. The small bar shows progress toward that milestone." }
                    ?: "The final campaign attack milestone is reached.") +
                "\n\nThis existing campaign multiplier applies to all balls. It is earned through campaign progress, not a paid upgrade for the selected ball."
            InfoTopic.SHOT -> "Straight sends the volley along your aim. Spread fans the volley out.\n\n" +
                "Tap a pattern button to save it for this ball. The gold underline marks the active pattern. Both patterns use the same ball count.\n\n" +
                (if (owned) "This ball supports both patterns." else "Unlock this ball before changing its pattern.")
        }
    }

    private fun drawInformation() {
        batch.setColor(0f, 0f, 0f, .78f)
        batch.draw(game.assets.solidUiRegion, visibleLeft, visibleBottom, visibleWidth, visibleHeight)
        batch.color = Color.WHITE
        art.tray(batch, layout.infoPanel, layout.scale)
        text(game.assets.titleFont, informationTitle, part(layout.infoPanel, .07f, .85f, .74f, .10f), 33f, gold)
        art.stone(batch, layout.infoClose, layout.scale)
        text(game.assets.hudValueFont, "X", layout.infoClose, 28f, cream)
        text(game.assets.hudLabelFont, informationBody, layout.infoBody, if (layout.wide) 26f else 29f,
            cream, Align.left, true)
    }

    private fun handleInput() {
        if (infoTopic != null) {
            touchActive = false
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) infoTopic = null
            else if (Gdx.input.justTouched()) {
                val touch = touchPoint()
                if (layout.contains(layout.infoClose, touch.x, touch.y) || !layout.infoPanel.contains(touch.x, touch.y)) infoTopic = null
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
            touchDragged = false; touchHeld = 0f; heroGesture = layout.hero.contains(touch.x, touch.y)
        } else if (touchActive && Gdx.input.isTouched(0)) {
            if (abs(touch.x - touchStartX) >= 24f * layout.scale || abs(touch.y - touchStartY) >= 24f * layout.scale) touchDragged = true
            touchHeld += Gdx.graphics.deltaTime.coerceIn(0f, .05f)
            if (!touchDragged && touchHeld >= .60f && showShotPatterns &&
                (layout.contains(layout.straight, touchStartX, touchStartY) || layout.contains(layout.spread, touchStartX, touchStartY)))
                openInformation(InfoTopic.SHOT)
        } else if (touchActive && !Gdx.input.isTouched(0)) {
            touchActive = false
            val dx = touch.x - touchStartX; val dy = touch.y - touchStartY
            if (heroGesture && abs(dx) >= 78f * layout.scale && abs(dx) > abs(dy) * 1.3f) browse(if (dx < 0f) 1 else -1)
            else if (!touchDragged && abs(dx) < 24f * layout.scale && abs(dy) < 24f * layout.scale) activate(touch.x, touch.y)
        }
    }

    private fun activate(x: Float, y: Float) {
        when {
            layout.contains(layout.back, x, y) -> leave()
            layout.contains(layout.help, x, y) -> openInformation(InfoTopic.OVERVIEW)
            layout.tabs.any { layout.contains(it, x, y) } -> selectRarity(BallRarity.entries[layout.tabs.indexOfFirst { layout.contains(it, x, y) }])
            layout.contains(layout.previous, x, y) -> browse(-1)
            layout.contains(layout.next, x, y) -> browse(1)
            layout.stats.any { layout.contains(it, x, y) } -> openInformation(when (layout.stats.indexOfFirst { layout.contains(it, x, y) }) {
                0 -> InfoTopic.DAMAGE; 1 -> InfoTopic.BALLS; else -> InfoTopic.ABILITY })
            layout.contains(layout.identity, x, y) -> openInformation(InfoTopic.POWER)
            layout.contains(layout.progression, x, y) -> openInformation(InfoTopic.CAMPAIGN)
            showShotPatterns && layout.contains(layout.straight, x, y) -> {
                if (owned) game.progress.setShotgunEnabled(current.ball.spriteName, false) else openInformation(InfoTopic.SHOT)
            }
            showShotPatterns && layout.contains(layout.spread, x, y) -> {
                if (owned) game.progress.setShotgunEnabled(current.ball.spriteName, true) else openInformation(InfoTopic.SHOT)
            }
            layout.contains(layout.primary, x, y) -> {
                if (owned && !equipped) {
                    VolleyBallSelection.equip(game.progress.settings, current.ball)
                    game.progress.saveSettings(); refresh(); game.uiFeedback.emit(UiFeedbackKind.CONFIRM)
                } else if (!owned && canShop()) game.navigate(ShopScreen(game, shopReturnDestination, mode, "BALLS", current.ball.spriteName))
            }
        }
    }

    private fun leave() {
        touchActive = false
        if (onBack != null) onBack.invoke(current.ball)
        else if (returnToPausedGame) game.resumePausedGame(showLoading = false) else game.openMenu()
    }

    private fun pressed(rect: Rectangle): Boolean {
        if (!touchActive || touchDragged || infoTopic != null || !Gdx.input.isTouched(0)) return false
        val touch = touchPoint()
        return abs(touch.x - touchStartX) < 24f * layout.scale && abs(touch.y - touchStartY) < 24f * layout.scale &&
            layout.contains(rect, touchStartX, touchStartY) && layout.contains(rect, touch.x, touch.y)
    }

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
