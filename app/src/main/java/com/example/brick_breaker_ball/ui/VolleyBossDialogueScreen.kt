/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/ui/VolleyBossDialogueScreen.kt
 * الوظيفة: عرض حوار زعيم العالم.
 * الحسابات: توقيت النص وترتيب ظهوره.
 */

package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.utils.Align
import com.example.brick_breaker_ball.BrickBreakerGame
import com.example.brick_breaker_ball.assets.WorldVideoBackgrounds
import com.example.brick_breaker_ball.campaign.LevelDefinition
import com.example.brick_breaker_ball.campaign.VolleyStageDefinition
import com.example.brick_breaker_ball.campaign.VolleyStageRepository
import com.example.brick_breaker_ball.gameplay.GameplayMode
import com.example.brick_breaker_ball.gameplay.VolleySessionSnapshot

enum class VolleyBossDialogueMoment { WORLD_INTRO, FINAL_WARNING }

data class VolleyBossDialogueScript(
    val bossName: String,
    val title: String,
    val moment: VolleyBossDialogueMoment,
    val lines: List<String>
)

/** One authored personality for every supplied Volley boss. */
object VolleyBossDialogueCatalog {
    private data class Voice(
        val name: String,
        val intro: List<String>,
        val warning: List<String>
    )

    private val voices = listOf(
        Voice("THORN WARDEN", listOf("Another challenger enters my forest.", "Let us see how long your volley survives the roots."), listOf("You reached my heartwood... impressive.", "Now every thorn answers to me. Come and break.")),
        Voice("VENOM MATRIARCH", listOf("Breathe deeply, little striker. The wilds are already inside you.", "Cross my passages if you dare."), listOf("Your luck ends here.", "One final volley—and the venom claims everything.")),
        Voice("ABYSSAL ORACLE", listOf("The vortex showed me your arrival.", "It also showed me your defeat."), listOf("The current has delivered you to my throne.", "Aim carefully. The abyss never returns what it takes.")),
        Voice("DUNE REVENANT", listOf("These halls remember every fallen champion.", "Soon, they will remember you."), listOf("You disturbed the final seal.", "Face the storm you awakened.")),
        Voice("FROST TYRANT", listOf("Warm courage shatters quickly in my grave.", "Show me whether yours is different."), listOf("No more shelter. No more easy angles.", "I will freeze your last ball in flight.")),
        Voice("SOUL JAILER", listOf("Every bounce echoes through my prison.", "Make enough noise, and the lost will answer."), listOf("The chains are awake now.", "Your next volley belongs to the dead.")),
        Voice("LUNAR SENTINEL", listOf("Moonlight reveals every weakness.", "I have already seen yours."), listOf("The eclipse begins with this battle.", "Stand your ground, challenger.")),
        Voice("INFERNAL HERALD", listOf("Beyond this gate, even steel learns fear.", "Enter—and feed the fire."), listOf("You crossed the flames, but not their master.", "This gate closes behind you.")),
        Voice("ECLIPSE SOVEREIGN", listOf("Nine worlds bent before the eclipse.", "Your volley will be the final light I erase."), listOf("There is nowhere left to climb.", "Break me now—or watch every world go dark."))
    )

    fun forStage(stage: VolleyStageDefinition): VolleyBossDialogueScript? {
        val moment = when {
            stage.stage == 1 -> VolleyBossDialogueMoment.WORLD_INTRO
            stage.bossStage -> VolleyBossDialogueMoment.FINAL_WARNING
            else -> return null
        }
        val voice = voices[stage.world.coerceIn(1, voices.size) - 1]
        return VolleyBossDialogueScript(
            bossName = voice.name,
            title = if (moment == VolleyBossDialogueMoment.WORLD_INTRO) "WORLD CHALLENGE" else "FINAL WARNING",
            moment = moment,
            lines = if (moment == VolleyBossDialogueMoment.WORLD_INTRO) voice.intro else voice.warning
        )
    }
}

/** Deterministic typewriter/advance state, separated from rendering for regression tests. */
class VolleyBossDialogueFlow(
    val script: VolleyBossDialogueScript,
    private val charactersPerSecond: Float = 65f,
    reducedMotion: Boolean = false
) {
    var lineIndex: Int = 0
        private set
    private var visibleCharacters = if (reducedMotion) currentLine.length.toFloat() else 0f
    var complete: Boolean = false
        private set

    val currentLine: String get() = script.lines.getOrElse(lineIndex) { "" }
    val visibleText: String get() = currentLine.take(visibleCharacters.toInt().coerceIn(0, currentLine.length))
    val lineRevealed: Boolean get() = visibleCharacters >= currentLine.length

    fun update(delta: Float) {
        if (!complete && !lineRevealed) {
            visibleCharacters = (visibleCharacters + delta.coerceAtMost(.1f) * charactersPerSecond)
                .coerceAtMost(currentLine.length.toFloat())
        }
    }

    /** First action reveals the line; the next advances. Returns true when battle should begin. */
    fun revealOrAdvance(): Boolean {
        if (complete) return true
        if (!lineRevealed) {
            visibleCharacters = currentLine.length.toFloat()
            return false
        }
        if (lineIndex < script.lines.lastIndex) {
            lineIndex++
            visibleCharacters = 0f
            return false
        }
        complete = true
        return true
    }

    fun skip() {
        complete = true
    }
}

/** Portrait boss encounter shown before a world's opening and final Volley stages. */
class VolleyBossDialogueScreen(
    game: BrickBreakerGame,
    private val level: LevelDefinition,
    private val restoredSnapshot: VolleySessionSnapshot? = null
) : ForgeScreen(game) {
    private val stage = VolleyStageRepository.forLevel(level.id)
    private val script = requireNotNull(VolleyBossDialogueCatalog.forStage(stage))
    private val flow = VolleyBossDialogueFlow(script, reducedMotion = game.progress.settings.reduceMotion)
    private val skipBounds = Rectangle(698f, 1460f, 154f, 70f)
    private val continueBounds = Rectangle(150f, 815f, 600f, 92f)
    private var elapsed = 0f
    private var leaving = false
    private var bossAudioStarted = false

    override fun show() {
        game.applyGameplayOrientation(GameplayMode.VOLLEY)
        WorldVideoBackgrounds.hide()
        game.assets.volleyStageBackground(stage)
        game.assets.volleyBossPortrait(stage.world)
        bossAudioStarted = false
    }

    override fun hide() {
        game.assets.stopBossDialogueAudio()
    }

    override fun render(delta: Float) {
        elapsed += delta.coerceAtMost(.1f)
        flow.update(delta)

        beginStaticBackground(game.assets.volleyStageBackground(stage), .58f)
        drawEncounterHeader()
        drawBoss()
        drawSpeechBubble()
        val skip = button("SKIP", skipBounds.x, skipBounds.y, skipBounds.width, skipBounds.height, ForgeUiRenderer.GradientStyle.NEUTRAL)
        val actionLabel = when {
            !flow.lineRevealed -> "REVEAL MESSAGE"
            flow.lineIndex < script.lines.lastIndex -> "NEXT"
            else -> "ENTER BATTLE"
        }
        val action = button(actionLabel, continueBounds.x, continueBounds.y, continueBounds.width, continueBounds.height)
        end()

        // Rendering the first encounter frame before playing prevents a first-load audio miss.
        if (!bossAudioStarted) {
            bossAudioStarted = true
            game.assets.playBossDialogueAudio(
                script.moment,
                game.progress.settings.masterVolume * game.progress.settings.sfxVolume
            )
        }
        if (leaving) return
        if (tapped(skip) || Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            flow.skip()
            enterBattle()
        } else if (elapsed >= .28f && tapped(action) && flow.revealOrAdvance()) {
            enterBattle()
        }
    }

    private fun drawEncounterHeader() {
        val accent = if (script.moment == VolleyBossDialogueMoment.FINAL_WARNING) ForgeUiPalette.danger else ForgeUiPalette.primary
        batch.color = Color(accent).also { it.a = .20f }
        batch.draw(game.assets.softGlowRegion, 30f, 1360f, 840f, 190f)
        batch.color = Color.WHITE
        fittedText(
            if (script.moment == VolleyBossDialogueMoment.FINAL_WARNING) game.assets.buttonFont else game.assets.hudLabelFont,
            script.title,
            75f,
            1512f,
            750f,
            .66f
        )
        fittedText(game.assets.titleFont, script.bossName, 65f, 1438f, 770f, .78f)
    }

    private fun drawBoss() {
        val portrait = game.assets.volleyBossPortrait(stage.world)
        val maxW = 700f
        val maxH = 720f
        val scale = minOf(maxW / portrait.regionWidth, maxH / portrait.regionHeight)
        val width = portrait.regionWidth * scale
        val height = portrait.regionHeight * scale
        val fromRight = (stage.world + script.moment.ordinal) % 2 == 1
        val raw = if (game.progress.settings.reduceMotion) 1f else (elapsed / .52f).coerceIn(0f, 1f)
        val enter = 1f - (1f - raw) * (1f - raw) * (1f - raw)
        val restingX = if (fromRight) 900f - width + 35f else -35f
        val startX = if (fromRight) 940f else -width - 40f
        val x = MathUtils.lerp(startX, restingX, enter)
        val y = 100f + if (game.progress.settings.reduceMotion) 0f else MathUtils.sin(elapsed * 2.8f) * 5f
        batch.color = Color(0f, .75f, 1f, .12f * enter)
        batch.draw(game.assets.softGlowRegion, x - 70f, y - 35f, width + 140f, height + 100f)
        batch.color = Color(1f, 1f, 1f, enter)
        batch.draw(portrait, x, y, width, height)
        batch.color = Color.WHITE
    }

    private fun drawSpeechBubble() {
        val bubble = Rectangle(62f, 950f, 776f, 380f)
        batch.color = Color(0.025f, .055f, .10f, .94f)
        batch.draw(game.assets.solidUiRegion, bubble.x, bubble.y, bubble.width, bubble.height)
        batch.color = Color(ForgeUiPalette.primary).also { it.a = .28f }
        batch.draw(game.assets.softGlowRegion, bubble.x - 18f, bubble.y - 18f, bubble.width + 36f, bubble.height + 36f)
        batch.color = Color.WHITE

        fittedText(game.assets.hudLabelFont, script.bossName, bubble.x + 48f, bubble.y + bubble.height - 48f, bubble.width - 96f, .62f)
        val oldColor = Color(game.assets.bodyFont.color)
        game.assets.bodyFont.color = ForgeUiPalette.textPrimary
        game.assets.bodyFont.draw(
            batch,
            flow.visibleText + if (!flow.lineRevealed && (elapsed * 4f).toInt() % 2 == 0) "|" else "",
            bubble.x + 58f,
            bubble.y + bubble.height - 116f,
            bubble.width - 116f,
            Align.left,
            true
        )
        game.assets.bodyFont.color = oldColor
        fittedText(
            game.assets.smallFont,
            "MESSAGE ${flow.lineIndex + 1}/${script.lines.size}",
            bubble.x + 50f,
            bubble.y + 45f,
            bubble.width - 100f,
            .43f
        )
    }

    private fun enterBattle() {
        if (leaving) return
        leaving = true
        game.navigate(VolleyGameScreen(game, level, restoredSnapshot), ScreenTransitionStyle.FADE_THROUGH)
    }
}
