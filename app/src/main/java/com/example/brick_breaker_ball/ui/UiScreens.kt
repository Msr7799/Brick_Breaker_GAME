/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/ui/UiScreens.kt
 * الوظيفة: شاشات القائمة والنتائج والإعدادات.
 * الحسابات: تخطيط العناصر بحسب الاتجاه وحدود اللمس.
 */

package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.InputAdapter
import com.badlogic.gdx.InputMultiplexer
import com.badlogic.gdx.InputProcessor
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.utils.TimeUtils
import com.badlogic.gdx.utils.viewport.ExtendViewport
import com.example.brick_breaker_ball.BrickBreakerGame
import com.example.brick_breaker_ball.BuildConfig
import com.example.brick_breaker_ball.app.DevelopmentAccess
import com.example.brick_breaker_ball.assets.WorldVideoBackgrounds
import com.example.brick_breaker_ball.campaign.DEFAULT_MASTER_VOLUME
import com.example.brick_breaker_ball.campaign.LevelDefinition
import com.example.brick_breaker_ball.campaign.VolleyStageRepository
import com.example.brick_breaker_ball.campaign.VolleyDifficulty
import com.example.brick_breaker_ball.campaign.WorldDefinition
import com.example.brick_breaker_ball.campaign.WorldMapSelectionPolicy
import com.example.brick_breaker_ball.cosmetics.BallAbilityCatalog
import com.example.brick_breaker_ball.cosmetics.BallSpriteDefinition
import com.example.brick_breaker_ball.cosmetics.CosmeticUnlock
import com.example.brick_breaker_ball.economy.CampaignEconomyReward
import com.example.brick_breaker_ball.gameplay.VolleyOrbDefinition
import com.example.brick_breaker_ball.economy.EconomyStore
import com.example.brick_breaker_ball.gameplay.Ball
import com.example.brick_breaker_ball.gameplay.Brick
import com.example.brick_breaker_ball.gameplay.CampaignCatalog
import com.example.brick_breaker_ball.gameplay.GameplayMode
import kotlin.math.abs

abstract class ForgeScreen(protected val game: BrickBreakerGame) : ScreenAdapter() {
    companion object {
        const val DESIGN_WIDTH = 900f
        const val DESIGN_HEIGHT = 1600f
    }

    protected val camera = OrthographicCamera()
    protected val viewport = ExtendViewport(DESIGN_WIDTH, DESIGN_HEIGHT, camera)
    protected val batch = SpriteBatch()
    private val p = Vector3()
    private val fittedTextLayout = GlyphLayout()
    private var previousInputProcessor: InputProcessor? = null
    private var mouseWheelProcessor: InputProcessor? = null
    private var mouseWheelWrapper: InputProcessor? = null
    private var presentationStartNanos: Long = 0L

    private fun ensurePresentationClock() {
        if (presentationStartNanos == 0L) presentationStartNanos = TimeUtils.nanoTime()
    }

    /** Screen titles/actions appear fully ready; ONLY the central navigation veil animates routes. */
    @Suppress("UNUSED_PARAMETER")
    protected fun presentationProgress(delay: Float = 0f, duration: Float = .28f): Float = 1f

    protected fun selectionPulse(): Float {
        if (game.progress.settings.reduceMotion) return 0f
        ensurePresentationClock()
        val age = (TimeUtils.nanoTime() - presentationStartNanos) / 1_000_000_000f
        return .5f + .5f * MathUtils.sin(age * 3.4f)
    }

    protected val visibleWidth: Float get() = viewport.worldWidth
    protected val visibleHeight: Float get() = viewport.worldHeight
    protected val visibleLeft: Float get() = camera.position.x - visibleWidth / 2f
    protected val visibleBottom: Float get() = camera.position.y - visibleHeight / 2f
    protected val isLandscape: Boolean get() = false
    protected val safeInsets: UiSafeInsets get() = UiMetrics.safeInsets(viewport)
    protected val uiTextScale: Float get() = UiMetrics.textScale(if (isLandscape) UiOrientation.LANDSCAPE else UiOrientation.PORTRAIT)

    /** Shared wide-screen grid: 94% usable width with a safe edge gutter and a tablet cap. */
    protected fun responsiveContentWidth(maxWidth: Float = 2700f, edgeGutter: Float = 72f): Float {
        val insets = safeInsets
        return if (isLandscape) {
            minOf(visibleWidth - insets.left - insets.right - edgeGutter * 2f, maxWidth).coerceAtLeast(DESIGN_WIDTH)
        } else {
            (DESIGN_WIDTH - insets.left - insets.right).coerceAtLeast(760f)
        }
    }

    protected fun responsiveContentLeft(maxWidth: Float = 2700f, edgeGutter: Float = 72f): Float {
        val insets = safeInsets
        val usableLeft = visibleLeft + insets.left + if (isLandscape) edgeGutter else 0f
        val usableRight = visibleLeft + visibleWidth - insets.right - if (isLandscape) edgeGutter else 0f
        val width = responsiveContentWidth(maxWidth, edgeGutter)
        return (usableLeft + usableRight - width) / 2f
    }

    /** Keeps cinematic backgrounds atmospheric instead of competing with readable UI surfaces. */
    protected fun drawLandscapeScrim(alpha: Float = .54f) {
        if (!isLandscape) return
        batch.setColor(ForgeUiPalette.neutralBlack.r, ForgeUiPalette.neutralBlack.g, ForgeUiPalette.neutralBlack.b, alpha)
        batch.draw(game.assets.solidUiRegion, visibleLeft, visibleBottom, visibleWidth, visibleHeight)
        batch.color = Color.WHITE
    }

    init {
        camera.position.set(DESIGN_WIDTH / 2f, DESIGN_HEIGHT / 2f, 0f)
        camera.update()
    }

    protected fun installMouseWheelHandler(handler: (amountY: Float) -> Boolean) {
        removeMouseWheelHandler()
        previousInputProcessor = Gdx.input.inputProcessor
        mouseWheelProcessor = object : InputAdapter() {
            override fun scrolled(amountX: Float, amountY: Float): Boolean = handler(amountY)
        }
        mouseWheelWrapper = previousInputProcessor?.let { existing ->
            InputMultiplexer(mouseWheelProcessor, existing)
        } ?: mouseWheelProcessor
        Gdx.input.inputProcessor = mouseWheelWrapper
    }

    protected fun removeMouseWheelHandler() {
        if (Gdx.input.inputProcessor === mouseWheelWrapper) {
            Gdx.input.inputProcessor = previousInputProcessor
        }
        mouseWheelWrapper = null
        mouseWheelProcessor = null
        previousInputProcessor = null
    }

    /** Draw a background so phones and tablets fill the whole physical display without stretching. */
    protected fun drawFullscreen(region: TextureRegion) {
        val sourceRatio = region.regionWidth.toFloat() / region.regionHeight.toFloat()
        val targetRatio = visibleWidth / visibleHeight
        val cropped = TextureRegion(region)
        if (sourceRatio < targetRatio) {
            val sourceHeight = (region.regionWidth / targetRatio).toInt().coerceIn(1, region.regionHeight)
            cropped.setRegion(
                region.regionX,
                region.regionY + (region.regionHeight - sourceHeight) / 2,
                region.regionWidth,
                sourceHeight
            )
        } else if (sourceRatio > targetRatio) {
            val sourceWidth = (region.regionHeight * targetRatio).toInt().coerceIn(1, region.regionWidth)
            cropped.setRegion(
                region.regionX + (region.regionWidth - sourceWidth) / 2,
                region.regionY,
                sourceWidth,
                region.regionHeight
            )
        }
        batch.draw(cropped, visibleLeft, visibleBottom, visibleWidth, visibleHeight)
    }

    protected fun drawFullscreen(texture: Texture) = drawFullscreen(TextureRegion(texture))

    protected fun drawFullscreenPanel(region: TextureRegion) {
        batch.draw(region, visibleLeft, visibleBottom, visibleWidth, visibleHeight)
    }

    /** ملاحظة صيانة: الدالة `begin` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    protected fun begin(world: Int = 1) {
        ensurePresentationClock()
        WorldVideoBackgrounds.hide()
        Gdx.gl.glClearColor(.003f, .008f, .025f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        viewport.apply()
        batch.projectionMatrix = camera.combined
        batch.begin()
        batch.setColor(1f, 1f, 1f, .86f)
        drawFullscreen(game.assets.worldFallbackBg(world))
        batch.setColor(ForgeUiPalette.navy.r, ForgeUiPalette.navy.g, ForgeUiPalette.navy.b, .40f)
        drawFullscreenPanel(game.assets.solidUiRegion)
        batch.color = Color.WHITE
    }

    /** ملاحظة صيانة: الدالة `begin` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    protected fun begin(background: String) = begin(
        when (background) {
            "world_2_crystal" -> 2
            "world_3_magma" -> 3
            "world_4_zerog" -> 4
            else -> 1
        }
    )

    /** Draws a supplied static background without touching portrait/video behaviour. */
    protected fun beginStaticBackground(background: TextureRegion, overlayAlpha: Float = .20f) {
        ensurePresentationClock()
        WorldVideoBackgrounds.hide()
        Gdx.gl.glClearColor(.003f, .008f, .025f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        viewport.apply()
        batch.projectionMatrix = camera.combined
        batch.begin()
        batch.color = Color.WHITE
        drawFullscreen(background)
        if (overlayAlpha > 0f) {
            batch.setColor(0f, 0f, 0f, overlayAlpha)
            drawFullscreenPanel(game.assets.solidUiRegion)
        }
        batch.color = Color.WHITE
    }

    /** ملاحظة صيانة: الدالة `title` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    protected fun title(s: String, y: Float = 1480f) {
        val p = presentationProgress(0f, .32f)
        val oldColor = Color(game.assets.titleFont.color)
        game.assets.titleFont.color = Color(oldColor.r, oldColor.g, oldColor.b, (.40f + .60f * p).coerceIn(0f, 1f))
        // TANHEADLINE has no bullet glyph; keep campaign titles readable.
        fittedText(
            game.assets.titleFont,
            s.replace('•', '|'),
            35f,
            y + 20f * (1f - p),
            830f,
            UiTextRole.TITLE.baseMultiplier * (.96f + .04f * p)
        )
        game.assets.titleFont.color = oldColor
    }

    /** ملاحظة صيانة: الدالة `button` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    protected fun button(
        text: String,
        x: Float,
        y: Float,
        w: Float = 650f,
        h: Float = 100f,
        style: ForgeUiRenderer.GradientStyle = ForgeUiRenderer.GradientStyle.PRIMARY
    ): Rectangle {
        val rect = Rectangle(x, y, w, h)
        val point = touchPoint()
        val pressed = Gdx.input.isTouched && UiMetrics.containsExpanded(rect, point.x, point.y)
        val baseVisual = Pressable.visualRect(rect, pressed, game.progress.settings.reduceMotion)
        val staggerDelay = .045f + (kotlin.math.abs(text.hashCode()) % 5) * .018f
        val enter = presentationProgress(staggerDelay, .26f)
        val visual = Rectangle(baseVisual.x, baseVisual.y - 22f * (1f - enter), baseVisual.width, baseVisual.height)
        if (pressed) {
            batch.color = Color(ForgeUiPalette.primary).also { it.a = .16f }
            batch.draw(game.assets.particles.findRegion("glow"), visual.x - 12f, visual.y - 12f, visual.width + 24f, visual.height + 24f)
            batch.color = Color.WHITE
        }
        game.assets.uiRenderer.drawGradientButton(batch, visual, style)
        game.assets.buttonFont.color = game.assets.uiRenderer.labelColor(style)
        fittedText(
            game.assets.buttonFont, text,
            visual.x + 10f, visual.y + visual.height * .66f,
            visual.width - 20f, UiTextRole.BUTTON.baseMultiplier,
            visual.height * .72f
        )
        game.assets.buttonFont.color = ForgeUiPalette.textPrimary
        return rect
    }

    /** Uses the supplied lock artwork on a bright badge so it stays visible over dark previews. */
    protected fun drawLockBadge(x: Float, y: Float, size: Float) {
        game.assets.uiRenderer.drawRoundedGradientButton(
            batch,
            Rectangle(x, y, size, size),
            ForgeUiRenderer.GradientStyle.PRIMARY
        )
        val inset = size * .16f
        batch.color = Color.WHITE
        batch.draw(game.assets.lockIcon, x + inset, y + inset, size - inset * 2f, size - inset * 2f)
    }

    /** ملاحظة صيانة: الدالة `fittedText` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    protected fun fittedText(
        font: BitmapFont, text: String, x: Float, y: Float, width: Float,
        maxScale: Float = 1f, maxHeight: Float = Float.POSITIVE_INFINITY
    ) {
        val oldX = font.data.scaleX
        val oldY = font.data.scaleY
        val desired = maxScale * uiTextScale
        val drawWidth = width.coerceAtLeast(1f)
        font.data.setScale(desired)
        fittedTextLayout.setText(font, text)
        val widthFit = drawWidth / fittedTextLayout.width.coerceAtLeast(1f)
        val heightFit = maxHeight / fittedTextLayout.height.coerceAtLeast(1f)
        font.data.setScale(desired * minOf(1f, widthFit, heightFit))
        font.draw(batch, text, x, y, drawWidth, Align.center, false)
        font.data.setScale(oldX, oldY)
    }

    /** ملاحظة صيانة: الدالة `starIcon` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    protected fun starIcon(x: Float, y: Float, size: Float) {
        batch.color = Color.WHITE
        batch.draw(game.assets.starIcon, x, y, size, size)
    }

    /** ملاحظة صيانة: الدالة `worldDoneIcon` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    protected fun worldDoneIcon(x: Float, y: Float, size: Float) {
        batch.color = Color.WHITE
        batch.draw(game.assets.worldDoneIcon, x, y, size, size)
    }

    /** ملاحظة صيانة: الدالة `tapped` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    protected fun tapped(r: Rectangle): Boolean {
        if (game.uiMotion.active || !Gdx.input.justTouched()) return false
        p.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
        viewport.unproject(p)
        val hit = UiMetrics.containsExpanded(r, p.x, p.y)
        if (hit) game.uiFeedback.emit(UiFeedbackKind.TAP)
        return hit
    }

    /** ملاحظة صيانة: الدالة `touchPoint` تحوّل البيانات أو تبني المعرّف المتوافق مع بقية النظام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    protected fun touchPoint(): Vector3 {
        p.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
        viewport.unproject(p)
        return p
    }

    /** ملاحظة صيانة: الدالة `end` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    protected fun end() {
        batch.end()
    }

    /** ملاحظة صيانة: الدالة `resize` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun resize(w: Int, h: Int) {
        viewport.update(w, h, false)
        camera.position.set(DESIGN_WIDTH / 2f, DESIGN_HEIGHT / 2f, 0f)
        camera.update()
    }

    /** ملاحظة صيانة: الدالة `dispose` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun dispose() = batch.dispose()
}

class SplashScreen(game: BrickBreakerGame) : ForgeScreen(game) {
    private var time = 0f
    private var leaving = false
    private var exitRequestedAt: Float? = null

    /** ملاحظة صيانة: الدالة `show` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun show() {
        WorldVideoBackgrounds.showSplash()
        game.assets.warmMainMenuAssets()
    }

    /** ملاحظة صيانة: الدالة `render` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun render(delta: Float) {
        time += delta
        val videoVisible = WorldVideoBackgrounds.isVideoVisible()
        Gdx.gl.glClearColor(.018f, .026f, .043f, if (videoVisible) 0f else 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        if (exitRequestedAt == null && (time >= 7.65f || (time >= .65f && Gdx.input.justTouched()))) {
            exitRequestedAt = time
        }
        val requestedAt = exitRequestedAt
        if (!leaving && requestedAt != null &&
            (game.platformActions.appUpdateCheckComplete || time - requestedAt >= 3f)
        ) {
            leaving = true
            // Keep the current splash frame visible during fade-out; hide its video in hide().
            game.syncDailyRewards()
            val updateVersion = game.platformActions.availableAppUpdateVersionCode
            if (updateVersion != null) game.navigate(AppUpdateScreen(game, updateVersion))
            else game.openFirstEntryOrMenu()
        }
    }

    /** ملاحظة صيانة: الدالة `hide` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun hide() {
        WorldVideoBackgrounds.hide()
    }
}

class MainMenuScreen(game: BrickBreakerGame) : ForgeScreen(game) {
    private val confirmation = ConfirmationCoordinator()
    private val walletPeek = WalletBalancePeek()
    private val walletPeekBounds = Rectangle()
    private var pressedConfirmationButton = 0
    private var dailyRewardToastTime = 0f
    private val dailyRewardFx = RewardFxSystem(game)
    private var dailyRewardFxStarted = false
    private val portraitMenuJson by lazy {
        JsonReader().parse(Gdx.files.internal("backgrounds/start-screen/main-screen/main-screen-FINAIL.json"))
    }

    private data class MenuTargets(
        val info: Rectangle,
        val sound: Rectangle,
        val newGame: Rectangle,
        val settings: Rectangle,
        val exit: Rectangle,
        val primary: Rectangle,
        val worldMap: Rectangle,
        val charmsBag: Rectangle,
        val shop: Rectangle,
        val customize: Rectangle,
        val volleyEditor: Rectangle,
        val gemsPlus: Rectangle,
        val coinsPlus: Rectangle,
        val gemsValue: Rectangle,
        val coinsValue: Rectangle,
        val share: Rectangle,
        val youtube: Rectangle,
        val development: Rectangle?
    )

    private companion object {
        const val REFERENCE_WIDTH = 1440f
        const val REFERENCE_HEIGHT = 2930f
    }

    /** ملاحظة صيانة: الدالة `show` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun show() {
        syncMainMenuVideo()
        startMenuMusic()
    }

    /** ملاحظة صيانة: الدالة `hide` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun hide() {
        WorldVideoBackgrounds.hide()
        game.assets.stopMenuMusic()
    }

    /** ملاحظة صيانة: الدالة `pause` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun pause() = game.assets.stopMenuMusic()

    /** ملاحظة صيانة: الدالة `resume` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun resume() {
        syncMainMenuVideo()
        startMenuMusic()
    }

    private fun syncMainMenuVideo() {
        WorldVideoBackgrounds.showMenu()
    }

    /** ملاحظة صيانة: الدالة `startMenuMusic` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun startMenuMusic() = game.assets.startMenuMusic(
        game.progress.settings.masterVolume * game.progress.settings.musicVolume
    )

    /** يحافظ على نسبة 9:16 الأصلية داخل الشاشات الطويلة حتى لا تتمدّد الأزرار عموديًا. */
    private fun mainMenuFrame(): Rectangle {
        val scale = minOf(visibleWidth / REFERENCE_WIDTH, visibleHeight / REFERENCE_HEIGHT)
        val width = REFERENCE_WIDTH * scale
        val height = REFERENCE_HEIGHT * scale
        return Rectangle(
            visibleLeft + (visibleWidth - width) / 2f,
            visibleBottom + (visibleHeight - height) / 2f,
            width,
            height
        )
    }

    /** يرسم الخلفية مرة واحدة داخل إطار 9:16؛ تبقى المساحة الإضافية داكنة بلا تكرار أو تمطيط. */

    /** يحوّل إحداثيات خريطة MAIN_SCREEN ذات الأصل العلوي إلى مساحة LibGDX ذات الأصل السفلي. */
    private fun referenceScreenRect(
        frame: Rectangle,
        x: Float,
        top: Float,
        width: Float,
        height: Float
    ): Rectangle {
        val scale = frame.width / REFERENCE_WIDTH
        return Rectangle(
            frame.x + x * scale,
            frame.y + (REFERENCE_HEIGHT - top - height) * scale,
            width * scale,
            height * scale
        )
    }

    /** يرسم مكوّنًا مستقلًا في موضعه الأصلي من main-bg.json ويعيد منطقة اللمس المطابقة. */

    /** Uses the exact top-origin coordinates supplied in main-screen.json. */
    private fun portraitAuthoredRect(frame: Rectangle, name: String): Rectangle {
        val node = portraitMenuJson.get("frames").get(name).get("frame")
        return referenceScreenRect(
            frame,
            node.getFloat("x"),
            node.getFloat("y"),
            node.getFloat("w"),
            node.getFloat("h")
        )
    }

    /**
     * The Continue button and its controller artwork are already baked into the menu
     * PNG/MP4. Only replace the opaque CENTER LABEL when no saved run exists.
     * Drawing the complete new-game image scaled to the Continue frame duplicates the
     * controllers and makes START look warped. Crop and placement live in the same
     * authored JSON map as the button's touch bounds, for both orientations.
     */
    private fun drawAuthoredNewGameLabel(frame: Rectangle) {
        val label = portraitMenuJson.get("overlays")?.get("new-game-label") ?: return
        val source = label.get("source")
        val target = label.get("target")
        val texture = game.assets.mainMenuComponentTexture("new-game-btn-final")
        val patch = TextureRegion(
            texture,
            source.getInt("x"),
            source.getInt("y"),
            source.getInt("w"),
            source.getInt("h")
        )
        val destination = referenceScreenRect(
            frame,
            target.getFloat("x"),
            target.getFloat("y"),
            target.getFloat("w"),
            target.getFloat("h")
        )
        batch.color = Color.WHITE
        batch.draw(patch, destination.x, destination.y, destination.width, destination.height)
    }

    private fun drawPortraitMenu(returningPlayer: Boolean): MenuTargets {
        val frame = mainMenuFrame()

        batch.color = Color.WHITE
        if (!WorldVideoBackgrounds.isVideoVisible()) {
            batch.draw(game.assets.mainMenuPortraitAuthored, frame.x, frame.y, frame.width, frame.height)
        }

        val info = portraitAuthoredRect(frame, "info")
        val sound = portraitAuthoredRect(frame, "music-btn")
        val primary = portraitAuthoredRect(frame, "continue&new-game-btn")
        val settings = portraitAuthoredRect(frame, "settings-btn")
        val worldMap = portraitAuthoredRect(frame, "worlds-map")
        val charmsBag = portraitAuthoredRect(frame, "charms-bag")
        val shop = portraitAuthoredRect(frame, "shop")
        val customize = portraitAuthoredRect(frame, "balls")
        val volleyEditor = portraitAuthoredRect(frame, "level-editor-btn")
        val gems = portraitAuthoredRect(frame, "the-dimon-value")
        val gemsPlus = portraitAuthoredRect(frame, "the-dimon-pius-btn")
        val coins = portraitAuthoredRect(frame, "the-coins-value")
        val coinsPlus = portraitAuthoredRect(frame, "the-coins-pluse-btn")
        val share = portraitAuthoredRect(frame, "share-my-app-btn")
        val youtube = portraitAuthoredRect(frame, "youtube")
        if (!returningPlayer) drawAuthoredNewGameLabel(frame)

        // حالة التشغيل مرسومة في الخلفية؛ صورة الكتم تتجاوز حافتها قليلاً حتى لا يظهر إطار الزر الأصلي.
        // منطقة اللمس تبقى مرتبطة بزر music-btn الأصلي في الحالتين.
        if (game.progress.settings.masterVolume <= 0f) {
            batch.color = Color.WHITE
            val offIcon = game.assets.mainMenuComponentTexture("sound-off-btn-final")
            val target = requireNotNull(portraitMenuJson.get("overlays")?.get("sound-off")?.get("target")) {
                "Portrait menu is missing the sound-off overlay target"
            }
            val destination = referenceScreenRect(
                frame,
                target.getFloat("x"),
                target.getFloat("y"),
                target.getFloat("w"),
                target.getFloat("h")
            )
            batch.draw(offIcon, destination.x, destination.y, destination.width, destination.height)
        }

        drawWalletValues(gems, coins)
        val peekWidth = minOf(600f, visibleWidth - safeInsets.left - safeInsets.right - 40f)
        walletPeekBounds.set(visibleLeft + visibleWidth - safeInsets.right - peekWidth - 20f,
            minOf(gems.y, coins.y) - 100f, peekWidth, 82f)
        val development = drawDeveloperAccessButton(frame)

        val none = Rectangle(-10_000f, -10_000f, 0f, 0f)
        return MenuTargets(
            info = info,
            sound = sound,
            newGame = none,
            settings = settings,
            exit = none,
            primary = primary,
            worldMap = worldMap,
            charmsBag = charmsBag,
            shop = shop,
            customize = customize,
            volleyEditor = volleyEditor,
            gemsPlus = gemsPlus,
            coinsPlus = coinsPlus,
            gemsValue = gems,
            coinsValue = coins,
            share = share,
            youtube = youtube,
            development = development
        )
    }

    /** زر المطور القديم: يظهر في Debug وQA فقط ويبدّل الحالة المحفوظة مباشرة. */
    private fun drawDeveloperAccessButton(frame: Rectangle): Rectangle? {
        if (!DevelopmentAccess.DEVELOPER_ACCESS || !BuildConfig.DEVELOPER_ACCESS_ALLOWED) return null
        val rect = portraitAuthoredRect(frame, "developer-btn")
        val style = if (game.developmentAccess.enabled) {
            ForgeUiRenderer.GradientStyle.SUCCESS
        } else {
            ForgeUiRenderer.GradientStyle.NEUTRAL
        }
        game.assets.uiRenderer.drawGradientButton(batch, rect, style)
        game.assets.hudLabelFont.color = game.assets.uiRenderer.labelColor(style)
        fittedText(
            game.assets.hudLabelFont,
            if (game.developmentAccess.enabled) "DEV ON" else "DEV OFF",
            rect.x + 3f,
            rect.y + rect.height * .67f,
            rect.width - 6f,
            .55f
        )
        game.assets.hudLabelFont.color = Color.WHITE
        return rect
    }

    private fun drawWalletValues(gems: Rectangle, coins: Rectangle) {
        batch.color = Color.WHITE
        fittedText(game.assets.hudValueFont, WalletText.compact(game.economy.gems), gems.x, gems.y + gems.height * .72f, gems.width, .62f)
        fittedText(game.assets.hudValueFont, WalletText.compact(game.economy.coins), coins.x, coins.y + coins.height * .72f, coins.width, .62f)
    }

    private fun drawMenuPressFeedback(targets: MenuTargets) {
        // Main-menu primary cards are pre-rendered artwork. Any generated glow/panel around them
        // creates the exact ugly rectangular halo the user called out, especially in portrait.
        // Keep the menu art clean and let tap feedback come from navigation sound/vibration only.
        return
    }

    /** ملاحظة صيانة: الدالة `render` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun render(delta: Float) {
        game.syncDailyRewards()
        game.openDailyRewardIfDue()
        walletPeek.update(delta)
        syncMainMenuVideo()
        dailyRewardFx.update(delta)
        if (!dailyRewardFxStarted && game.pendingDailyWalletReward != null) {
            dailyRewardFxStarted = true
            dailyRewardFx.trigger(
                kind = RewardFxSystem.Kind.DAILY,
                icons = listOf(TextureRegion(game.assets.starIcon), TextureRegion(game.assets.landscapeWorldMapStarOn)),
                centerX = 450f,
                centerY = 310f,
                targetX = if (isLandscape) visibleLeft + visibleWidth - 170f else 450f,
                targetY = visibleBottom + visibleHeight - 120f,
                accent = ForgeUiPalette.gold
            )
            game.uiFeedback.emit(UiFeedbackKind.CONFIRM)
        }
        val menuVideoVisible = WorldVideoBackgrounds.isVideoVisible()
        Gdx.gl.glClearColor(0f, 0f, 0f, if (menuVideoVisible) 0f else 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        viewport.apply()
        batch.projectionMatrix = camera.combined
        batch.begin()
        batch.color = Color.WHITE
        val returningPlayer = game.progress.hasStartedGame || game.progress.totalStars() > 0 ||
            game.progress.stars(1) > 0 || game.hasPausedGame()
        val targets = drawPortraitMenu(returningPlayer)
        drawMenuPressFeedback(targets)

        game.pendingDailyWalletReward?.let { reward ->
            dailyRewardToastTime += delta.coerceAtMost(.1f)
            if (dailyRewardToastTime < 7f) {
                val toastWidth = if (isLandscape) 760f else 700f
                val toastX = DESIGN_WIDTH / 2f - toastWidth / 2f
                val toastY = visibleBottom + 48f
                game.assets.uiRenderer.drawGradientBorderPanel(
                    batch,
                    Rectangle(toastX, toastY, toastWidth, 92f),
                    ForgeUiRenderer.GradientStyle.SUCCESS,
                    4f
                )
                fittedText(
                    game.assets.hudLabelFont,
                    if (reward.streak == EconomyStore.DAILY_REWARD_DAYS) {
                        "DAY 14 JACKPOT  •  +${reward.coins} COINS  •  +${reward.gems} GEMS"
                    } else {
                        "DAILY STREAK ${reward.streak}/${EconomyStore.DAILY_REWARD_DAYS}  •  +${reward.coins} COINS  •  +${reward.gems} GEMS"
                    },
                    toastX + 18f,
                    toastY + 58f,
                    toastWidth - 36f,
                    .68f
                )
            } else {
                game.pendingDailyWalletReward = null
                dailyRewardFxStarted = false
            }
        }

        dailyRewardFx.draw(batch)
        walletPeek.draw(batch, game.assets, walletPeekBounds, game.economy.coins, game.economy.gems,
            game.progress.settings.reduceMotion)

        val pendingConfirmation = confirmation.pending
        if (pendingConfirmation != null) {
            val hitboxes = BoardConfirmationRenderer.draw(
                batch,
                game.assets,
                game.assets.bodyFont,
                Rectangle(visibleLeft, visibleBottom, visibleWidth, visibleHeight),
                pendingConfirmation
            )
            end()
            when {
                Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) -> {
                    pressedConfirmationButton = 0
                    confirmation.cancel()
                }

                Gdx.input.justTouched() -> {
                    val point = touchPoint()
                    pressedConfirmationButton = when {
                        hitboxes.yes.contains(point.x, point.y) -> 1
                        hitboxes.no.contains(point.x, point.y) -> 2
                        else -> 0
                    }
                }

                !Gdx.input.isTouched && pressedConfirmationButton != 0 -> {
                    val selected = pressedConfirmationButton
                    pressedConfirmationButton = 0
                    val point = touchPoint()
                    if (selected == 1 && hitboxes.yes.contains(point.x, point.y)) confirmation.confirm()
                    if (selected == 2 && hitboxes.no.contains(point.x, point.y)) confirmation.cancel()
                }
            }
            return
        }
        end()
        when {
            targets.development?.let { tapped(it) } == true -> {
                val enabled = game.developmentAccess.toggle()
                game.cosmeticProgression.reconcile(game.progress, enabled)
                game.uiFeedback.emit(UiFeedbackKind.CONFIRM)
            }

            tapped(targets.settings) -> game.navigate(SettingsScreen(game), ScreenTransitionStyle.FADE_THROUGH)

            tapped(targets.sound) -> {
                game.progress.settings.masterVolume = if (game.progress.settings.masterVolume > 0f) 0f else DEFAULT_MASTER_VOLUME
                game.progress.saveSettings()
                if (game.progress.settings.masterVolume > 0f) startMenuMusic() else game.assets.stopMenuMusic()
            }

            tapped(targets.newGame) -> confirmation.request(
                UiConfirmation(
                    UiConfirmationPurpose.NEW_GAME,
                    "Start at Level 1? Your stars, unlocked levels and best scores stay safe.",
                    onConfirm = { game.startNewGame() }
                )
            )

            tapped(targets.info) -> game.navigate(RulesScreen(game), ScreenTransitionStyle.FADE_THROUGH)

            tapped(targets.exit) -> confirmation.request(
                UiConfirmation(
                    UiConfirmationPurpose.QUIT,
                    "Close Brick Breaker Ball? Your saved progress stays safe.",
                    onConfirm = { Gdx.app.exit() }
                )
            )

            tapped(targets.primary) -> if (returningPlayer && game.hasPausedGame()) {
                game.resumePausedGame()
            } else {
                game.play(if (returningPlayer) game.progress.nextRecommendedLevel() else 1)
            }

            tapped(targets.worldMap) -> game.navigate(WorldMapScreen(game), ScreenTransitionStyle.FADE_THROUGH)

            tapped(targets.volleyEditor) -> game.navigate(VolleyLevelEditorScreen(game), ScreenTransitionStyle.FADE_THROUGH)

            tapped(targets.shop) -> game.navigate(ShopScreen(game, ShopReturnDestination.MAIN_MENU), ScreenTransitionStyle.FADE_THROUGH)

            tapped(targets.gemsPlus) || tapped(targets.coinsPlus) ->
                game.navigate(ShopScreen(game, ShopReturnDestination.MAIN_MENU), ScreenTransitionStyle.FADE_THROUGH)

            tapped(targets.gemsValue) -> walletPeek.show(coins = false)

            tapped(targets.coinsValue) -> walletPeek.show(coins = true)

            tapped(targets.share) -> game.platformActions.shareGame()

            tapped(targets.youtube) -> game.platformActions.openGameplayVideo()

            tapped(targets.charmsBag) -> game.setScreen(
                VolleyOrbsScreen(game, ShopReturnDestination.MAIN_MENU, GameplayMode.VOLLEY, returnToShop = false, backToBag = false)
            )

            tapped(targets.customize) -> game.navigate(CustomizationScreen(game), ScreenTransitionStyle.FADE_THROUGH)
        }
    }
}

class WorldMapScreen(game: BrickBreakerGame, initialWorld: Int? = null) : ForgeScreen(game) {
    private companion object {
        const val HEADER_BOTTOM = 1400f
        const val FOOTER_TOP = 150f
        const val CONTENT_BOTTOM_PADDING = 30f
        const val CARD_START_Y = 1060f
        const val CARD_ROW_SPACING = 330f
        const val CARD_WIDTH = 380f
        const val CARD_HEIGHT = 292f
    }

    private var scrollOffset = 0f
    private var touchActive = false
    private var touchMoved = false
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var touchLastX = 0f
    private var touchLastY = 0f
    private var scrollVelocity = 0f
    private val cards = mutableListOf<Pair<Rectangle, Int>>()
    private val campaignMode = game.progress.settings.gameplayMode
    private val campaignWorlds = CampaignCatalog.worlds(campaignMode)

    private val maxScroll: Float
        get() {
            val columns = if (isLandscape) 4 else 2
            val rows = (campaignWorlds.size + columns - 1) / columns
            val startY = if (isLandscape) 805f else CARD_START_Y
            val rowSpacing = if (isLandscape) 535f else CARD_ROW_SPACING
            val lastRowY = startY - (rows - 1) * rowSpacing
            return (FOOTER_TOP + CONTENT_BOTTOM_PADDING - lastRowY).coerceAtLeast(0f)
        }

    override fun show() {
        installMouseWheelHandler(::handleMouseWheel)
    }

    override fun hide() {
        removeMouseWheelHandler()
    }

    /** ملاحظة صيانة: الدالة `drawCover` ترسم العناصر المطلوبة مع الحفاظ على ترتيب طبقات العرض؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun drawCover(region: TextureRegion, x: Float, y: Float, width: Float, height: Float) {
        val sourceRatio = region.regionWidth.toFloat() / region.regionHeight
        val targetRatio = width / height
        val cropped = TextureRegion(region)
        if (sourceRatio < targetRatio) {
            val sourceHeight = (region.regionWidth / targetRatio).toInt().coerceAtLeast(1)
            cropped.setRegion(region.regionX, region.regionY + (region.regionHeight - sourceHeight) / 2, region.regionWidth, sourceHeight)
        } else if (sourceRatio > targetRatio) {
            val sourceWidth = (region.regionHeight * targetRatio).toInt().coerceAtLeast(1)
            cropped.setRegion(region.regionX + (region.regionWidth - sourceWidth) / 2, region.regionY, sourceWidth, region.regionHeight)
        }
        batch.draw(cropped, x, y, width, height)
    }

    /** ملاحظة صيانة: الدالة `mapCard` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun mapCard(
        world: WorldDefinition,
        unlocked: Boolean,
        selected: Boolean,
        x: Float,
        y: Float,
        width: Float = CARD_WIDTH,
        height: Float = CARD_HEIGHT
    ): Rectangle {
        val imageHeight = height - 80f
        val preview = game.assets.worldMapImage(world.id, campaignMode)
        if (selected) {
            game.assets.uiRenderer.drawGradientBorderPanel(
                batch,
                Rectangle(x - 9f, y - 9f, width + 18f, height + 18f),
                ForgeUiRenderer.GradientStyle.PRIMARY,
                7f
            )
        }
        game.assets.uiRenderer.drawGradientBorderPanel(
            batch,
            Rectangle(x, y, width, height),
            if (selected) ForgeUiRenderer.GradientStyle.PRIMARY else ForgeUiRenderer.GradientStyle.NEUTRAL,
            5f
        )
        drawCover(preview, x + 6f, y + 74f, width - 12f, imageHeight)
        if (!unlocked) {
            batch.color = Color(0f, 0f, 0f, .56f)
            batch.draw(game.assets.solidUiRegion, x + 6f, y + 74f, width - 12f, imageHeight)
            drawLockBadge(x + width - 78f, y + height - 78f, 64f)
        }
        batch.color = Color(ForgeUiPalette.navy).also { it.a = .94f }
        batch.draw(game.assets.solidUiRegion, x + 6f, y + 6f, width - 12f, 70f)
        batch.color = Color.WHITE
        fittedText(game.assets.bodyFont, "${world.id}. ${world.name}", x + 12f, y + 61f, width - 24f, .79f)
        val first = CampaignCatalog.firstLevel(campaignMode, world.id)
        val last = CampaignCatalog.lastLevel(campaignMode, world.id)
        val completed = (first..last).count { game.progress.stars(it) > 0 }
        val earnedStars = (first..last).sumOf { game.progress.stars(it).coerceIn(0, 3) }
        val stagesPerWorld = CampaignCatalog.worldLevelCount(campaignMode, world.id)
        val progress = "$completed/$stagesPerWorld LEVELS"
        fittedText(game.assets.smallFont, if (unlocked) progress else "LOCKED • TAP TO PREVIEW", x + 12f, y + 27f, width - 24f, .66f)
        // One compact counter stays inside the card, even after every stage is cleared.
        val starBadgeWidth = (width * .5f).coerceIn(140f, 190f)
        val starBadgeX = x + (width - starBadgeWidth) / 2f
        game.assets.uiRenderer.drawGradientBorderPanel(
            batch,
            Rectangle(starBadgeX, y + 84f, starBadgeWidth, 46f),
            ForgeUiRenderer.GradientStyle.NEUTRAL,
            2f
        )
        starIcon(starBadgeX + 10f, y + 91f, 32f)
        fittedText(
            game.assets.smallFont, "$earnedStars/${stagesPerWorld * 3}",
            starBadgeX + 50f, y + 117f, starBadgeWidth - 60f, .82f, 30f
        )
        return Rectangle(x, y, width, height)
    }

    /** يبدأ قص شبكة العوالم بين الهيدر والفوتر الثابتين. */
    private fun beginWorldContentClip() {
        batch.flush()
        val bottom = Vector3(visibleLeft, FOOTER_TOP, 0f)
        val top = Vector3(visibleLeft + visibleWidth, HEADER_BOTTOM, 0f)
        viewport.project(bottom)
        viewport.project(top)
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST)
        Gdx.gl.glScissor(
            bottom.x.toInt(),
            bottom.y.toInt(),
            (top.x - bottom.x).toInt().coerceAtLeast(1),
            (top.y - bottom.y).toInt().coerceAtLeast(1)
        )
    }

    /** ينهي قص شبكة العوالم قبل رسم الهيدر والفوتر. */
    private fun endWorldContentClip() {
        batch.flush()
        Gdx.gl.glDisable(GL20.GL_SCISSOR_TEST)
    }

    /** يعيد فقط الجزء القابل للمس من البطاقة داخل حاوية التمرير. */
    private fun visibleWorldCard(rect: Rectangle): Rectangle? {
        val bottom = maxOf(rect.y, FOOTER_TOP)
        val top = minOf(rect.y + rect.height, HEADER_BOTTOM)
        if (top <= bottom) return null
        return Rectangle(rect.x, bottom, rect.width, top - bottom)
    }

    /** ملاحظة صيانة: الدالة `drawRoute` ترسم العناصر المطلوبة مع الحفاظ على ترتيب طبقات العرض؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun drawRoute() {
        val points = campaignWorlds.indices.map { index ->
            val row = index / 2
            val leftToRight = row % 2 == 0
            val visualColumn = if (leftToRight) index % 2 else 1 - index % 2
            (245f + visualColumn * 405f) to (1206f - row * CARD_ROW_SPACING + scrollOffset)
        }
        val glow = game.assets.particles.findRegion("glow")
        points.zipWithNext().forEach { (start, end) ->
            repeat(15) { step ->
                val t = step / 14f
                val x = start.first + (end.first - start.first) * t
                val y = start.second + (end.second - start.second) * t
                batch.color = Color(ForgeUiPalette.primaryLight.r, ForgeUiPalette.primaryLight.g, ForgeUiPalette.primaryLight.b, .72f)
                batch.draw(glow, x - 10f, y - 10f, 20f, 20f)
            }
        }
        batch.color = Color.WHITE
    }

    override fun render(delta: Float) {
        advanceScrollInertia(delta)
        val portraitWorld = game.progress.currentPlayingWorld.coerceIn(1, campaignWorlds.size)
        beginStaticBackground(game.assets.volleyWorldBackground(portraitWorld), overlayAlpha = .16f)
        drawLandscapeScrim(.42f)
        cards.clear()
        beginWorldContentClip()
        if (!isLandscape) drawRoute()
        val landscapeWidth = responsiveContentWidth()
        val landscapeLeft = responsiveContentLeft()
        val landscapeGap = 28f
        val landscapeCardWidth = (landscapeWidth - landscapeGap * 3f) / 4f
        val landscapeCardHeight = 490f
        val selectedWorldId = campaignWorlds.lastOrNull {
            game.developmentAccess.canSelectWorld(it.id, game.progress.unlockedLevel, campaignMode)
        }?.id ?: 1
        campaignWorlds.forEachIndexed { index, world ->
            val row = if (isLandscape) index / 4 else index / 2
            val col = if (isLandscape) {
                index % 4
            } else if (row % 2 == 0) {
                index % 2
            } else {
                1 - index % 2
            }
            val x = if (isLandscape) landscapeLeft + col * (landscapeCardWidth + landscapeGap) else 55f + col * 405f
            val y = if (isLandscape) 805f - row * 535f + scrollOffset else CARD_START_Y - row * CARD_ROW_SPACING + scrollOffset
            val firstLevel = CampaignCatalog.firstLevel(campaignMode, world.id)
            val lastLevel = CampaignCatalog.lastLevel(campaignMode, world.id)
            val completed = (firstLevel..lastLevel).count { game.progress.stars(it) > 0 }
            val unlocked = game.developmentAccess.canSelectWorld(world.id, game.progress.unlockedLevel, campaignMode)
            val cardWidth = if (isLandscape) landscapeCardWidth else CARD_WIDTH
            val cardHeight = if (isLandscape) landscapeCardHeight else CARD_HEIGHT
            val rect = mapCard(world, unlocked, world.id == selectedWorldId, x, y, cardWidth, cardHeight)
            visibleWorldCard(rect)?.let { cards += it to world.id }
            if (completed == CampaignCatalog.worldLevelCount(campaignMode, world.id)) {
                worldDoneIcon(x + cardWidth - 58f, y + cardHeight - 58f, 48f)
            }
        }
        endWorldContentClip()
        game.assets.uiRenderer.drawGradientPanel(batch, Rectangle(visibleLeft, 1400f, visibleWidth, 200f), ForgeUiRenderer.GradientStyle.NEUTRAL)
        title("WORLD MAP", 1520f)
        fittedText(game.assets.smallFont, "PLAY ANY STAGE • CLEAR THE WORLD TO UNLOCK THE NEXT", visibleLeft + 60f, 1430f, visibleWidth - 120f, .82f)
        game.assets.uiRenderer.drawGradientPanel(batch, Rectangle(visibleLeft, 0f, visibleWidth, 170f), ForgeUiRenderer.GradientStyle.NEUTRAL)
        val backWidth = if (isLandscape) 480f else 400f
        val mainMenu = button("BACK", if (isLandscape) landscapeLeft else 250f, 35f, backWidth, 100f, ForgeUiRenderer.GradientStyle.NEUTRAL)
        if (isLandscape) {
            val completedWorlds = campaignWorlds.count { world ->
                (CampaignCatalog.firstLevel(campaignMode, world.id)..CampaignCatalog.lastLevel(campaignMode, world.id))
                    .all { game.progress.stars(it) > 0 }
            }
            val progressWidth = 760f
            val progressX = landscapeLeft + landscapeWidth - progressWidth
            game.assets.uiRenderer.drawGradientPanel(
                batch,
                Rectangle(progressX, 35f, progressWidth, 100f),
                ForgeUiRenderer.GradientStyle.NEUTRAL
            )
            fittedText(game.assets.hudLabelFont, "WORLD PROGRESS", progressX + 20f, 108f, progressWidth - 40f, .76f)
            fittedText(game.assets.smallFont, "$completedWorlds / ${campaignWorlds.size} WORLDS CLEARED", progressX + 20f, 65f, progressWidth - 40f, .70f)
        }
        end()
        if (tapped(mainMenu)) {
            game.openMenu()
            return
        }
        handleTouch()
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK)) game.openMenu()
    }

    /** ملاحظة صيانة: الدالة `handleTouch` تعالج الحدث أو الطلب وتحدّث الحالة المرتبطة به؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun handleTouch() {
        val isTouched = Gdx.input.isTouched
        if (Gdx.input.justTouched()) {
            val point = touchPoint()
            touchActive = point.y in FOOTER_TOP..HEADER_BOTTOM
            touchMoved = false
            scrollVelocity = 0f
            touchStartX = point.x
            touchStartY = point.y
            touchLastX = point.x
            touchLastY = point.y
        }
        if (touchActive && isTouched) {
            val point = touchPoint()
            val delta = point.y - touchLastY
            scrollOffset = (scrollOffset + delta).coerceIn(0f, maxScroll)
            scrollVelocity = delta / Gdx.graphics.deltaTime.coerceIn(1f / 240f, .05f)
            touchLastX = point.x
            touchLastY = point.y
            if (abs(point.y - touchStartY) > 24f || abs(point.x - touchStartX) > 24f) touchMoved = true
        } else if (touchActive) {
            if (!touchMoved) {
                cards.firstOrNull { it.first.contains(touchLastX, touchLastY) }?.let {
                    game.setScreen(LevelSelectScreen(game, it.second, campaignMode))
                }
            }
            touchActive = false
        }
    }

    private fun advanceScrollInertia(delta: Float) {
        if (touchActive || abs(scrollVelocity) < 24f) {
            if (!touchActive) scrollVelocity = 0f
            return
        }
        val frameDelta = delta.coerceIn(0f, .05f)
        val nextOffset = (scrollOffset + scrollVelocity * frameDelta).coerceIn(0f, maxScroll)
        if (nextOffset == scrollOffset) {
            scrollVelocity = 0f
            return
        }
        scrollOffset = nextOffset
        scrollVelocity *= kotlin.math.exp(-5.5f * frameDelta)
    }

    private fun handleMouseWheel(amountY: Float): Boolean {
        if (amountY == 0f) return false
        val point = touchPoint()
        if (point.y !in FOOTER_TOP..HEADER_BOTTOM) return false
        scrollOffset = (scrollOffset + amountY * 260f).coerceIn(0f, maxScroll)
        return true
    }
}

/**
 * يعرض مراحل Volley بأزرار خفيفة مع قفل ونجوم مستقلة لكل مرحلة.
 */
class LevelSelectScreen(
    game: BrickBreakerGame,
    private val world: Int,
    private val campaignMode: GameplayMode = game.progress.settings.gameplayMode
) : ForgeScreen(game) {
    private val campaignWorld = CampaignCatalog.worlds(campaignMode)[world - 1]
    private val stageCount = CampaignCatalog.worldLevelCount(campaignMode, world)
    private val firstStageId = CampaignCatalog.firstLevel(campaignMode, world)
    // The preview decodes in the background; read it each frame so the ready image replaces the fallback.
    private val background get() = game.assets.worldMapImage(world, campaignMode)
    private val stageRects = Array(stageCount) { index ->
        Rectangle(75f + index % 3 * 260f, 1160f - index / 3 * 184f, 230f, 125f)
    }
    private val difficultyTabs = Array(VolleyDifficulty.entries.size) { index ->
        Rectangle(75f + index * 260f, 1320f, 230f, 62f)
    }

    override fun render(delta: Float) {
        // A single small world preview is shared by the whole screen; cards draw no stage images.
        beginStaticBackground(background, overlayAlpha = .10f)
        title(campaignWorld.name, 1515f)
        val worldUnlocked = game.developmentAccess.canSelectWorld(world, game.progress.unlockedLevel, campaignMode)
        val worldSubtitle = if (worldUnlocked) campaignWorld.subtitle else "CLEAR ALL STAGES IN THE PREVIOUS WORLD"
        fittedText(game.assets.smallFont, worldSubtitle, 45f, 1430f, 810f, .75f)

        VolleyDifficulty.entries.forEachIndexed { index, difficulty ->
            val tab = difficultyTabs[index]
            val selected = game.progress.selectedDifficulty == difficulty
            game.assets.uiRenderer.drawGradientButton(
                batch, tab,
                if (selected) ForgeUiRenderer.GradientStyle.PRIMARY else ForgeUiRenderer.GradientStyle.NEUTRAL
            )
            batch.color = Color.WHITE
            fittedText(game.assets.smallFont, difficulty.name, tab.x + 8f, tab.y + 42f, tab.width - 16f, .68f)
            if (selected) batch.draw(game.assets.solidUiRegion, tab.x + 10f, tab.y + 5f, tab.width - 20f, 4f)
        }
        for (index in 0 until stageCount) {
            val id = firstStageId + index
            val rect = stageRects[index]
            val open = game.developmentAccess.canSelectLevel(id, game.progress.unlockedLevel, campaignMode)
            button(
                (index + 1).toString(), rect.x, rect.y, rect.width, rect.height,
                if (open) ForgeUiRenderer.GradientStyle.PRIMARY else ForgeUiRenderer.GradientStyle.DISABLED
            )
            if (!open) drawLockBadge(rect.x + 170f, rect.y + 66f, 48f)
            val earnedStars = game.progress.stars(id, campaignMode).coerceIn(0, 3)
            repeat(3) { star ->
                val icon = if (star < earnedStars) game.assets.landscapeWorldMapStarOn
                    else game.assets.landscapeWorldMapStarOff
                batch.draw(icon, rect.x + 139f + star * 27f, rect.y + 24f, 26f, 26f)
            }
        }
        val back = button("BACK", 250f, 120f, 400f, 90f)
        end()

        val selectedDifficultyIndex = difficultyTabs.indexOfFirst { tapped(it) }
        if (selectedDifficultyIndex >= 0) {
            game.progress.selectedDifficulty = VolleyDifficulty.entries[selectedDifficultyIndex]
        } else {
            val selectedIndex = stageRects.indexOfFirst { tapped(it) }
            if (selectedIndex >= 0) {
                val id = firstStageId + selectedIndex
                if (game.developmentAccess.canSelectLevel(id, game.progress.unlockedLevel, campaignMode)) game.play(id)
            }
        }
        if (tapped(back) || Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            game.navigate(WorldMapScreen(game, world))
        }
    }
}

class ResultsScreen(
    game: BrickBreakerGame,
    private val level: LevelDefinition,
    private val score: Int,
    private val stars: Int,
    private val unlocks: List<CosmeticUnlock> = emptyList(),
    private val economyReward: CampaignEconomyReward = CampaignEconomyReward(0, 0, false, false),
    private val campaignMode: GameplayMode = game.progress.settings.gameplayMode,
    private val orbLoot: List<VolleyOrbDefinition> = emptyList()
) : ForgeScreen(game) {
    private var revealTime = 0f
    private var inputArmed = false
    private var celebrationTriggered = false
    private val rewardFx = RewardFxSystem(game)

    override fun show() {
        revealTime = 0f
        inputArmed = false
        celebrationTriggered = false
        val settings = game.progress.settings
        // Victory cue uses audio/level_up.ogg and respects SFX controls.
        game.assets.playLevelUp(settings.masterVolume * settings.sfxVolume)
        if (settings.haptics) Gdx.input.vibrate(28)
    }

    private fun reveal(start: Float, duration: Float): Float {
        val raw = ((revealTime - start) / duration).coerceIn(0f, 1f)
        return raw * raw * (3f - 2f * raw)
    }

    private fun drawAnimatedText(
        font: BitmapFont,
        text: String,
        x: Float,
        y: Float,
        width: Float,
        alpha: Float,
        scale: Float = 1f,
        color: Color? = null,
        maxHeight: Float? = null
    ) {
        if (alpha <= 0f) return
        val oldColor = Color(font.color)
        val oldScaleX = font.data.scaleX
        val oldScaleY = font.data.scaleY
        val baseColor = color ?: oldColor
        font.color = Color(baseColor.r, baseColor.g, baseColor.b, alpha)
        font.data.setScale(oldScaleX * scale * uiTextScale, oldScaleY * scale * uiTextScale)
        val measured = GlyphLayout(font, text)
        val widthFit = if (measured.width > 0f) width * .94f / measured.width else 1f
        val heightFit = if (maxHeight != null && measured.height > 0f)
            maxHeight / measured.height else 1f
        val fit = minOf(1f, widthFit, heightFit)
        if (fit < 1f) {
            font.data.setScale(oldScaleX * scale * uiTextScale * fit, oldScaleY * scale * uiTextScale * fit)
        }
        font.draw(batch, text, x, y, width, Align.center, false)
        font.data.setScale(oldScaleX, oldScaleY)
        font.color = oldColor
    }

    /** Victory screen: title, stars, score, unlocked cosmetics and controls all reveal in sequence. */
    override fun render(delta: Float) {
        revealTime = if (game.progress.settings.reduceMotion) {
            2.4f
        } else {
            (revealTime + delta.coerceAtMost(.1f)).coerceAtMost(2.4f)
        }
        rewardFx.update(delta)
        beginStaticBackground(
            game.assets.volleyStageBackground(VolleyStageRepository.forLevel(level.id)),
            overlayAlpha = if (isLandscape) .10f else .46f
        )
        val atlas = game.assets.boards2
        val insets = safeInsets
        val board = atlas.contain(
            "scroll_board",
            Rectangle(
                visibleLeft + insets.left + 24f,
                visibleBottom + insets.bottom + 24f,
                visibleWidth - insets.left - insets.right - 48f,
                visibleHeight - insets.top - insets.bottom - 48f
            )
        )
        batch.color = Color.WHITE
        batch.draw(atlas.requireRegion("scroll_board"), board.x, board.y, board.width, board.height)
        val titleArea = atlas.placement("scroll_board", "scroll_board_title_area", board)
        val content = atlas.placement("scroll_board", "scroll_board_content_area", board)
        if (!celebrationTriggered && revealTime >= .28f) {
            celebrationTriggered = true
            val rewardIcons = buildList<TextureRegion> {
                // مكافآت Volley كرات فقط؛ تثبيت النوع هنا يمنع الرجوع إلى فرع بادل محذوف.
                unlocks.filterIsInstance<CosmeticUnlock.Ball>().take(3).forEach { unlock ->
                    game.assets.cosmetics.ballRegion(unlock.ball)?.let(::add)
                }
                orbLoot.take(3).forEach { orb -> add(game.assets.orbIcon(orb.id)) }
                if (isEmpty()) add(TextureRegion(game.assets.starIcon))
            }
            rewardFx.trigger(
                kind = if (unlocks.isEmpty()) RewardFxSystem.Kind.DAILY else RewardFxSystem.Kind.UNLOCK,
                icons = rewardIcons,
                centerX = 450f,
                centerY = 1195f,
                targetX = 760f,
                targetY = 1460f,
                accent = if (unlocks.isEmpty()) ForgeUiPalette.gold else ForgeUiPalette.primaryLight,
                duration = 1.45f
            )
        }

        val titleReveal = reveal(0f, .30f)
        drawAnimatedText(
            game.assets.titleFont,
            "LEVEL COMPLETE",
            titleArea.x + 8f,
            titleArea.y + titleArea.height * .72f - 12f * (1f - titleReveal),
            titleArea.width - 16f,
            titleReveal,
            .89f + .08f * titleReveal
        )

        val iconSize = minOf(content.width * .15f, content.height * .14f)
        val gap = content.width * .035f
        val starsX = content.x + (content.width - 3f * iconSize - 2f * gap) / 2f
        repeat(3) { index ->
            val earned = index < stars
            val starReveal = reveal(.12f + index * .12f, .34f)
            if (starReveal > 0f) {
                val pop = if (earned) .75f + .25f * starReveal else .82f
                val drawSize = iconSize * pop
                val baseX = starsX + index * (iconSize + gap)
                batch.color = if (earned) {
                    Color(1f, 1f, 1f, starReveal)
                } else {
                    Color(.30f, .30f, .30f, .58f * starReveal)
                }
                batch.draw(
                    game.assets.starIcon,
                    baseX + (iconSize - drawSize) / 2f,
                    content.y + content.height * .78f + (iconSize - drawSize) / 2f,
                    drawSize,
                    drawSize
                )
                batch.color = Color.WHITE
            }
        }

        val scoreReveal = reveal(.38f, .30f)
        val scorePlate = Rectangle(
            content.x + content.width * .13f,
            content.y + content.height * .63f,
            content.width * .74f,
            content.height * .10f
        )
        batch.color = Color(1f, 1f, 1f, scoreReveal)
        batch.draw(
            atlas.requireRegion("ornate_button_blank"),
            scorePlate.x,
            scorePlate.y,
            scorePlate.width,
            scorePlate.height
        )
        batch.color = Color.WHITE
        drawAnimatedText(
            game.assets.hudValueFont,
            "SCORE  $score",
            scorePlate.x + 12f,
            scorePlate.y + scorePlate.height * .68f,
            scorePlate.width - 24f,
            scoreReveal,
            .86f,
            Color.WHITE
        )

        val walletReveal = reveal(.46f, .28f)
        if (walletReveal > 0f) {
            val chipWidth = content.width * .39f
            val chipHeight = content.height * .085f
            val gapWidth = content.width * .035f
            val startX = content.x + (content.width - chipWidth * 2f - gapWidth) * .5f
            listOf(
                economyReward.coins to game.assets.currencyCoinIcon,
                economyReward.gems to game.assets.currencyGemIcon
            ).forEachIndexed { index, reward ->
                val chip = Rectangle(
                    startX + index * (chipWidth + gapWidth),
                    content.y + content.height * .52f,
                    chipWidth,
                    chipHeight
                )
                batch.color = Color(1f, 1f, 1f, walletReveal)
                batch.draw(atlas.requireRegion("ui_button_dark_blank"), chip.x, chip.y, chip.width, chip.height)
                val iconSide = chip.height * .66f
                batch.draw(
                    reward.second,
                    chip.x + 13f,
                    chip.y + (chip.height - iconSide) * .5f,
                    iconSide,
                    iconSide
                )
                batch.color = Color.WHITE
                drawAnimatedText(
                    game.assets.buttonFont,
                    "+${(reward.first * walletReveal).toInt()}",
                    chip.x + iconSide + 17f,
                    chip.y + chip.height * .70f,
                    chip.width - iconSide - 25f,
                    walletReveal,
                    .76f,
                    Color.WHITE
                )
            }
        }

        if (unlocks.isNotEmpty()) {
            val rewardTitleReveal = reveal(.52f, .30f)
            drawAnimatedText(
                game.assets.hudLabelFont,
                if (unlocks.size == 1) "NEW COLLECTION ITEM" else "NEW COLLECTION ITEMS",
                content.x + content.width * .08f,
                content.y + content.height * .47f - 12f * (1f - rewardTitleReveal),
                content.width * .84f,
                rewardTitleReveal,
                color = Color(1f, .78f, .43f, 1f)
            )
            drawRewardCards(content)
        } else if (orbLoot.isNotEmpty()) {
            drawAnimatedText(game.assets.hudLabelFont, "ORBS FOUND", content.x + content.width * .08f,
                content.y + content.height * .47f, content.width * .84f, reveal(.55f, .30f),
                color = Color(1f, .78f, .43f, 1f))
            drawOrbLootCards(content)
        } else {
            val clearReveal = reveal(.55f, .30f)
            val crown = game.assets.arcadeIcons.findRegion("assets_9cd24f93_021")
            if (crown != null) {
                val side = content.width * .16f
                batch.color = Color(1f, 1f, 1f, clearReveal)
                batch.draw(
                    crown,
                    content.x + (content.width - side) * .5f,
                    content.y + content.height * .29f,
                    side,
                    side
                )
                batch.color = Color.WHITE
            }
            drawAnimatedText(
                game.assets.buttonFont,
                "STAGE ${level.id} CLEARED",
                content.x + content.width * .08f,
                content.y + content.height * .25f,
                content.width * .84f,
                clearReveal,
                .78f,
                Color(1f, .81f, .53f, 1f)
            )
        }

        val buttonsReveal = reveal(if (unlocks.isEmpty()) .66f else .92f, .36f)
        var next = Rectangle()
        var retry = Rectangle()
        var menu = Rectangle()
        var bag = Rectangle()
        if (buttonsReveal > 0f) {
            val slide = 36f * (1f - buttonsReveal)
            val nextLabel = when {
                level.id >= CampaignCatalog.totalLevels(campaignMode) -> "CAMPAIGN COMPLETE"
                game.developmentAccess.canSelectLevel(level.id + 1, game.progress.unlockedLevel, campaignMode) -> "NEXT LEVEL"
                else -> "BACK TO WORLD"
            }
            next = victoryButton(
                nextLabel,
                Rectangle(
                    content.x + content.width * .14f,
                    content.y + 10f - slide,
                    content.width * .72f,
                    content.height * .105f
                ),
                "ui_button_green_blank",
                buttonsReveal
            )
            if (orbLoot.isNotEmpty()) bag = victoryButton(
                "OPEN ORB BAG",
                Rectangle(content.x + content.width * .22f,
                    content.y + content.height * .13f - slide,
                    content.width * .56f, content.height * .075f),
                "ui_button_dark_blank", buttonsReveal)
            val backArea = atlas.placement("scroll_board", "scroll_board_back_button_area", board)
            val useArea = atlas.placement("scroll_board", "scroll_board_use_button_area", board)
            retry = victoryButton(
                "REPLAY",
                Rectangle(
                    backArea.x,
                    backArea.y - slide,
                    backArea.width,
                    backArea.height
                ),
                "ui_button_red_blank",
                buttonsReveal
            )
            menu = victoryButton(
                "MAIN MENU",
                Rectangle(
                    useArea.x,
                    useArea.y - slide,
                    useArea.width,
                    useArea.height
                ),
                "ui_button_dark_blank",
                buttonsReveal
            )
        }
        rewardFx.draw(batch)
        end()

        // Wait until the last gameplay touch is released so the victory animation cannot be skipped accidentally.
        if (!inputArmed) {
            if (revealTime >= .65f && !Gdx.input.isTouched) inputArmed = true
            return
        }

        when {
            tapped(next) -> {
                when {
                    level.id >= CampaignCatalog.totalLevels(campaignMode) -> game.openMenu()
                    game.developmentAccess.canSelectLevel(level.id + 1, game.progress.unlockedLevel, campaignMode) -> game.play(level.id + 1)
                    else -> game.navigate(LevelSelectScreen(game, level.world, campaignMode))
                }
            }

            tapped(retry) -> game.play(level.id)

            orbLoot.isNotEmpty() && tapped(bag) -> game.navigate(VolleyOrbsScreen(game, ShopReturnDestination.MAIN_MENU,
                GameplayMode.VOLLEY, returnToShop = false, backToBag = false))

            tapped(menu) -> game.openMenu()
        }
    }

    private fun drawOrbLootCards(content: Rectangle) {
        val shown = orbLoot.take(3)
        val cardW = content.width * .28f
        val cardH = content.height * .24f
        val gap = content.width * .025f
        val left = content.x + (content.width - shown.size * cardW - (shown.size - 1) * gap) / 2f
        shown.forEachIndexed { index, orb ->
            val alpha = reveal(.62f + index * .10f, .28f)
            val rect = Rectangle(left + index * (cardW + gap), content.y + content.height * .22f,
                cardW, cardH)
            batch.color = Color(1f, 1f, 1f, alpha)
            batch.draw(game.assets.boards2.requireRegion("large_stone_item_panel"),
                rect.x, rect.y, rect.width, rect.height)
            batch.color = Color.WHITE
            val side = minOf(rect.width * .46f, rect.height * .51f)
            batch.draw(game.assets.orbIcon(orb.id), rect.x + (rect.width - side) / 2f,
                rect.y + rect.height * .42f, side, side)
            drawAnimatedText(game.assets.smallFont, "${orb.family} R${orb.rank}",
                rect.x + 5f, rect.y + rect.height * .27f, rect.width - 10f, alpha, .58f)
            drawAnimatedText(game.assets.smallFont, orb.rarity,
                rect.x + 5f, rect.y + rect.height * .15f, rect.width - 10f, alpha, .48f)
        }
        if (orbLoot.size > shown.size) drawAnimatedText(game.assets.smallFont,
            "+${orbLoot.size - shown.size} MORE IN BAG", content.x + content.width * .1f,
            content.y + content.height * .19f, content.width * .8f, reveal(.9f, .3f), .55f)
    }

    /** Source-atlas fantasy button rather than stretched generic gradient rectangles. */
    private fun victoryButton(text: String, hit: Rectangle, skin: String, alpha: Float): Rectangle {
        val region = game.assets.boards2.requireRegion(skin)
        val pressed = Gdx.input.isTouched && hit.contains(touchPoint().x, touchPoint().y)
        val shown = if (pressed) {
            Rectangle(
                hit.x + hit.width * .025f,
                hit.y + hit.height * .05f,
                hit.width * .95f,
                hit.height * .9f
            )
        } else {
            hit
        }
        batch.color = Color(
            if (pressed) .73f else 1f,
            if (pressed) .73f else 1f,
            if (pressed) .73f else 1f,
            alpha
        )
        batch.draw(region, shown.x, shown.y, shown.width, shown.height)
        batch.color = Color.WHITE
        // Atlas button artwork has a narrower opaque center than its placement rectangle.
        val textInset = if (shown.width < 180f) shown.width * .18f else shown.width * .11f
        drawAnimatedText(
            game.assets.buttonFont,
            text,
            shown.x + textInset,
            shown.y + shown.height * .71f,
            shown.width - textInset * 2f,
            alpha,
            .68f,
            maxHeight = shown.height * .52f
        )
        return hit
    }

    /** تعرض مكافآت الكرات الفعلية في شاشة الفوز. */
    private fun drawRewardCards(content: Rectangle) {
        val visibleRewards = unlocks.take(3)
        val cardWidth = minOf(content.width * .28f, 250f)
        val cardHeight = minOf(content.height * .29f, 240f)
        val gap = content.width * .025f
        val totalWidth = visibleRewards.size * cardWidth + (visibleRewards.size - 1) * gap
        val startX = content.x + (content.width - totalWidth) * .5f
        visibleRewards.forEachIndexed { index, unlock ->
            val alpha = reveal(.62f + index * .12f, .32f)
            if (alpha <= 0f) return@forEachIndexed
            val rect = Rectangle(
                startX + index * (cardWidth + gap),
                content.y + content.height * .16f - 18f * (1f - alpha),
                cardWidth,
                cardHeight
            )
            batch.color = Color(1f, 1f, 1f, alpha)
            batch.draw(
                game.assets.boards2.requireRegion("large_stone_item_panel"),
                rect.x,
                rect.y,
                rect.width,
                rect.height
            )
            batch.color = Color.WHITE
            when (unlock) {
                is CosmeticUnlock.Ball -> {
                    game.assets.cosmetics.ballRegion(unlock.ball)?.let { region ->
                        val size = minOf(cardWidth * .42f, cardHeight * .36f)
                        batch.draw(
                            region,
                            rect.x + (rect.width - size) * .5f,
                            rect.y + rect.height * .52f,
                            size,
                            size
                        )
                    }
                    val profile = BallAbilityCatalog.profileForBall(unlock.ball)
                    drawAnimatedText(
                        game.assets.buttonFont,
                        unlock.ball.name.uppercase(),
                        rect.x + 10f,
                        rect.y + rect.height * .42f,
                        rect.width - 20f,
                        alpha,
                        .63f
                    )
                    drawAnimatedText(
                        game.assets.smallFont,
                        "${profile.title}  TIER ${profile.tier}",
                        rect.x + 12f,
                        rect.y + rect.height * .27f,
                        rect.width - 24f,
                        alpha,
                        .54f
                    )
                    drawAnimatedText(
                        game.assets.smallFont,
                        profile.shortStat(),
                        rect.x + 12f,
                        rect.y + rect.height * .14f,
                        rect.width - 24f,
                        alpha,
                        .52f
                    )
                }
            }
        }
        if (unlocks.size > visibleRewards.size) {
            drawAnimatedText(
                game.assets.smallFont,
                "+${unlocks.size - visibleRewards.size} MORE UNLOCKED",
                content.x + content.width * .1f,
                content.y + content.height * .10f,
                content.width * .8f,
                reveal(.95f, .30f),
                .56f
            )
        }
    }

    private fun drawBallReward(ball: BallSpriteDefinition, rect: Rectangle, alpha: Float) {
        val profile = BallAbilityCatalog.profileForBall(ball)
        val previewSize = minOf(128f, rect.height - 34f)
        game.assets.cosmetics.ballRegion(ball)?.let { region ->
            val side = minOf(region.regionWidth, region.regionHeight)
            val xOffset = ((region.regionWidth - side) / 2).coerceAtLeast(0)
            val yOffset = ((region.regionHeight - side) / 2).coerceAtLeast(0)
            val square = TextureRegion(region, xOffset, yOffset, side, side)
            // Intentionally no generated glow/shadow behind the ball: it caused the faint square halo.
            batch.draw(square, rect.x + 28f, rect.y + (rect.height - previewSize) / 2f, previewSize, previewSize)
        }
        val textX = rect.x + 185f
        val textWidth = rect.width - 210f
        drawAnimatedText(game.assets.hudLabelFont, ball.name.uppercase(), textX, rect.y + rect.height - 34f, textWidth, alpha, .70f)
        drawAnimatedText(game.assets.smallFont, "${profile.title} • TIER ${profile.tier}", textX, rect.y + rect.height - 80f, textWidth, alpha, .60f, ForgeUiPalette.primaryLight)
        drawAnimatedText(game.assets.smallFont, profile.shortStat(), textX, rect.y + rect.height - 116f, textWidth, alpha, .56f, ForgeUiPalette.gold)
        if (rect.height >= 210f) {
            drawAnimatedText(game.assets.smallFont, profile.description.uppercase(), textX, rect.y + 50f, textWidth, alpha, .46f, ForgeUiPalette.textSecondary)
        }
    }
}

class SettingsScreen(
    game: BrickBreakerGame,
    private val returnToPausedGame: Boolean = false,
    private val returnToWorldMapWorld: Int? = null,
    private val returnToWorldMapMode: GameplayMode = game.progress.settings.gameplayMode
) : ForgeScreen(game) {
    private var activeVolumeChannel: VolumeChannel? = null
    private var volumeWasChanged = false
    private enum class VolumeChannel { MUSIC, SFX }

    private companion object {
        const val FIRST_SETTING_Y = 1160f
        const val SETTING_STEP = 136f
        const val SETTING_TOUCH_HEIGHT = 114f
        const val SETTING_ART_HEIGHT = 200f
        const val SETTING_ART_BOTTOM_OFFSET = 45f
        const val DEVELOPER_BLOCK_HEIGHT = 210f
        const val DEVELOPER_SPACING = 24f
        const val ACTION_BUTTON_HEIGHT = 88f
        const val ACTION_BUTTON_GAP = 28f
    }

    override fun show() {
        if (!returnToPausedGame) {
            game.assets.startMenuMusic(
                game.progress.settings.masterVolume * game.progress.settings.musicVolume
            )
        }
    }

    override fun hide() {
        game.assets.stopMenuMusic()
    }

    private fun openCloudSave() {
        game.progress.saveSettings()
        game.cloudProgress.retry()
        game.navigate(CloudSaveScreen(game) {
            game.navigate(SettingsScreen(game, returnToPausedGame, returnToWorldMapWorld, returnToWorldMapMode))
        })
    }

    /** Saves settings and returns to the exact screen that opened Settings. */
    private fun saveAndReturn() {
        game.progress.saveSettings()
        when {
            returnToPausedGame -> game.resumePausedGame(showLoading = false)

            returnToWorldMapWorld != null -> {
                if (game.progress.settings.gameplayMode != returnToWorldMapMode) {
                    game.progress.settings.gameplayMode = returnToWorldMapMode
                    game.progress.saveSettings()
                }
                game.navigate(WorldMapScreen(game, returnToWorldMapWorld), ScreenTransitionStyle.FADE_THROUGH)
            }

            else -> game.openMenu()
        }
    }

    /** ملاحظة صيانة: الدالة `settingButton` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun settingButton(assetName: String, x: Float, y: Float, width: Float): Rectangle {
        val artHeight = SETTING_ART_HEIGHT * (width / 900f)
        val touch = Rectangle(x + width * (115f / 900f), y, width * (700f / 900f), SETTING_TOUCH_HEIGHT)
        val point = touchPoint()
        val pressed = Gdx.input.isTouched && UiMetrics.containsExpanded(touch, point.x, point.y)
        if (pressed) {
            batch.color = Color(ForgeUiPalette.ivory).also { it.a = .12f }
            batch.draw(game.assets.particles.findRegion("glow"), touch.x - 14f, touch.y - 14f, touch.width + 28f, touch.height + 28f)
        }
        batch.color = Color.WHITE
        val pressScale = if (pressed && !game.progress.settings.reduceMotion) .985f else 1f
        val drawWidth = width * pressScale
        val drawHeight = artHeight * pressScale
        batch.draw(
            game.assets.settingsMenuTexture(assetName),
            x + (width - drawWidth) * .5f,
            y - SETTING_ART_BOTTOM_OFFSET + (artHeight - drawHeight) * .5f,
            drawWidth,
            drawHeight
        )
        return touch
    }

    private fun roundedButton(text: String, x: Float, y: Float, width: Float, height: Float): Rectangle {
        val rect = Rectangle(x, y, width, height)
        val point = touchPoint()
        val pressed = Gdx.input.isTouched && UiMetrics.containsExpanded(rect, point.x, point.y)
        val visual = Pressable.visualRect(rect, pressed, game.progress.settings.reduceMotion)
        if (pressed) {
            batch.color = Color(ForgeUiPalette.primary).also { it.a = .11f }
            batch.draw(game.assets.particles.findRegion("glow"), visual.x - 8f, visual.y - 8f, visual.width + 16f, visual.height + 16f)
            batch.color = Color.WHITE
        }
        game.assets.uiRenderer.drawCompactRoundedGradientButton(batch, visual, ForgeUiRenderer.GradientStyle.PRIMARY)
        game.assets.buttonFont.color = ForgeUiPalette.textPrimary
        fittedText(
            game.assets.buttonFont,
            text,
            visual.x + 10f,
            visual.y + visual.height * .66f,
            visual.width - 20f,
            .92f
        )
        game.assets.buttonFont.color = ForgeUiPalette.textPrimary
        return rect
    }

    private fun imageButton(assetName: String, x: Float, y: Float, width: Float, height: Float): Rectangle {
        val rect = Rectangle(x, y, width, height)

        // Use the exact same forged outer frame, height and corner radius as the
        // orange Privacy / Website buttons so all three controls belong to one system.
        game.assets.uiRenderer.drawCompactRoundedGradientButton(
            batch,
            rect,
            ForgeUiRenderer.GradientStyle.NEUTRAL
        )

        val texture = game.assets.settingsMenuTexture(assetName)

        // The GitHub PNG already contains its own large rounded frame. Crop that old
        // frame away and keep only the GitHub mark + MSR7799 artwork, otherwise it
        // looks like a small button sitting inside another button.
        val cropX = (texture.width * .055f).toInt().coerceAtLeast(1)
        val cropY = (texture.height * .18f).toInt().coerceAtLeast(1)
        val cropWidth = (texture.width - cropX * 2).coerceAtLeast(1)
        val cropHeight = (texture.height - cropY * 2).coerceAtLeast(1)
        val region = TextureRegion(texture, cropX, cropY, cropWidth, cropHeight)

        // Match the inner face used by the orange buttons.
        val inset = 6f
        val innerX = x + inset
        val innerY = y + inset
        val innerWidth = width - inset * 2f
        val innerHeight = height - inset * 2f

        // Preserve the artwork aspect ratio and make it occupy almost the whole button.
        val sourceRatio = cropWidth.toFloat() / cropHeight.toFloat()
        val targetRatio = innerWidth / innerHeight
        var drawWidth = innerWidth
        var drawHeight = innerHeight
        if (sourceRatio > targetRatio) {
            drawHeight = innerWidth / sourceRatio
        } else {
            drawWidth = innerHeight * sourceRatio
        }

        batch.color = Color.WHITE
        batch.draw(
            region,
            innerX + (innerWidth - drawWidth) / 2f,
            innerY + (innerHeight - drawHeight) / 2f,
            drawWidth,
            drawHeight
        )
        return rect
    }

    /** ملاحظة صيانة: الدالة `toggleIcon` تحوّل البيانات أو تبني المعرّف المتوافق مع بقية النظام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun toggleIcon(enabled: Boolean, target: Rectangle, sound: Boolean = false) {
        batch.color = Color.WHITE
        val texture = if (sound) {
            game.assets.settingsMenuTexture(if (enabled) "sound-btn" else "sound-btn-off")
        } else {
            game.assets.settingsMenuTexture(if (enabled) "check-on" else "check-off")
        }
        if (sound) {
            val maxSize = 72f
            val aspect = texture.width.toFloat() / texture.height.toFloat()
            val drawWidth = if (aspect >= 1f) maxSize else maxSize * aspect
            val drawHeight = if (aspect >= 1f) maxSize / aspect else maxSize
            val x = target.x + target.width - drawWidth - 59f
            val y = target.y + (SETTING_TOUCH_HEIGHT - drawHeight) / 2f
            batch.draw(texture, x, y, drawWidth, drawHeight)
        } else {
            val size = 48f
            val x = target.x + target.width - size - 71f
            batch.draw(texture, x, target.y + (SETTING_TOUCH_HEIGHT - size) / 2f, size, size)
        }
    }

    /** Two independent Arcade-style channels. Keep the existing global master level in settings. */
    private fun drawVolumeMixer(rect: Rectangle): Pair<Rectangle, Rectangle> {
        val settings = game.progress.settings
        game.assets.uiRenderer.drawGradientBorderPanel(
            batch,
            rect,
            ForgeUiRenderer.GradientStyle.NEUTRAL,
            3f
        )
        val compact = rect.height < 180f
        val x = rect.x + if (compact) 222f else 160f
        val w = (rect.width - (if (compact) 365f else 305f)).coerceAtLeast(100f)
        val h = if (compact) 15f else 20f
        val musicY = rect.y + rect.height * (if (compact) .65f else .59f)
        val sfxY = rect.y + rect.height * (if (compact) .23f else .27f)
        fun channel(label: String, v: Float, y: Float): Rectangle {
            fittedText(game.assets.smallFont, label, rect.x + 12f, y + 19f, x - rect.x - 23f, .65f)
            val track = Rectangle(x, y, w, h)
            game.assets.uiRenderer.drawGradientProgress(batch, track, ForgeUiRenderer.GradientStyle.NEUTRAL)
            if (v > 0f) {
                game.assets.uiRenderer.drawGradientProgress(
                    batch,
                    Rectangle(x, y, w * v.coerceIn(0f, 1f), h),
                    ForgeUiRenderer.GradientStyle.PRIMARY
                )
            }
            fittedText(
                game.assets.smallFont,
                "${(v * 100).toInt()}%",
                x + w + 7f,
                y + 20f,
                rect.x + rect.width - x - w - 15f,
                .58f
            )
            return track
        }
        val music = channel("MUSIC", settings.musicVolume, musicY)
        val sfx = channel("SFX", settings.sfxVolume, sfxY)
        return music to sfx
    }

    /** Captures one touch until release; mixing music never modifies SFX or vice versa. */
    private fun handleMixerInput(musicTrack: Rectangle, sfxTrack: Rectangle) {
        val point = touchPoint()
        if (activeVolumeChannel == null && Gdx.input.justTouched()) {
            val musicArea = Rectangle(musicTrack.x - 20f, musicTrack.y - 19f, musicTrack.width + 40f, musicTrack.height + 38f)
            val sfxArea = Rectangle(sfxTrack.x - 20f, sfxTrack.y - 19f, sfxTrack.width + 40f, sfxTrack.height + 38f)
            activeVolumeChannel = when {
                UiMetrics.containsExpanded(musicArea, point.x, point.y) -> VolumeChannel.MUSIC
                UiMetrics.containsExpanded(sfxArea, point.x, point.y) -> VolumeChannel.SFX
                else -> null
            }
        }
        val channel = activeVolumeChannel ?: return
        if (Gdx.input.isTouched) {
            val track = if (channel == VolumeChannel.MUSIC) musicTrack else sfxTrack
            val value = ((point.x - track.x) / track.width).coerceIn(0f, 1f)
            val settings = game.progress.settings
            val old = if (channel == VolumeChannel.MUSIC) settings.musicVolume else settings.sfxVolume
            if (kotlin.math.abs(value - old) >= .005f) {
                if (channel == VolumeChannel.MUSIC) {
                    settings.musicVolume = value
                    // Audition menu music on the options screen when opened from a menu.
                    if (!returnToPausedGame) {
                        if (value > 0f) {
                            game.assets.startMenuMusic(settings.masterVolume * value)
                        } else {
                            game.assets.stopMenuMusic()
                        }
                    }
                } else {
                    settings.sfxVolume = value
                }
                volumeWasChanged = true
                game.applyAudioSettings()
            }
        } else {
            activeVolumeChannel = null
            if (volumeWasChanged) {
                game.progress.saveSettings()
                if (channel == VolumeChannel.SFX) {
                    game.assets.play("ui_click", game.progress.settings.masterVolume * game.progress.settings.sfxVolume * .65f)
                }
                volumeWasChanged = false
            }
        }
    }

    private fun landscapeSettingRow(
        label: String,
        subtitle: String,
        rect: Rectangle,
        enabled: Boolean,
        volume: Float? = null
    ): Rectangle {
        game.assets.uiRenderer.drawGradientPanel(batch, rect, ForgeUiRenderer.GradientStyle.NEUTRAL)
        fittedText(game.assets.hudLabelFont, label, rect.x + 28f, rect.y + rect.height - 40f, rect.width * .62f, .78f)
        game.assets.smallFont.color = ForgeUiPalette.textSecondary
        fittedText(game.assets.smallFont, subtitle, rect.x + 28f, rect.y + rect.height - 78f, rect.width * .62f, .68f)
        game.assets.smallFont.color = ForgeUiPalette.textPrimary
        if (volume != null) {
            val track = Rectangle(rect.x + 28f, rect.y + 35f, rect.width - 170f, 24f)
            game.assets.uiRenderer.drawGradientProgress(batch, track, ForgeUiRenderer.GradientStyle.NEUTRAL)
            game.assets.uiRenderer.drawGradientProgress(
                batch,
                Rectangle(track.x, track.y, track.width * volume.coerceIn(0f, 1f), track.height),
                ForgeUiRenderer.GradientStyle.PRIMARY
            )
            fittedText(game.assets.smallFont, "${(volume * 100).toInt()}%", rect.x + rect.width - 120f, rect.y + 52f, 90f, .72f)
        } else {
            val toggle = Rectangle(rect.x + rect.width - 150f, rect.y + (rect.height - 70f) / 2f, 120f, 70f)
            game.assets.uiRenderer.drawRoundedGradientButton(
                batch,
                toggle,
                if (enabled) ForgeUiRenderer.GradientStyle.PRIMARY else ForgeUiRenderer.GradientStyle.DISABLED
            )
            fittedText(game.assets.smallFont, if (enabled) "ON" else "OFF", toggle.x, toggle.y + 45f, toggle.width, .72f)
        }
        return rect
    }

    private fun toggleControlMode() {
        val settings = game.progress.settings
        settings.relativeControl = !settings.relativeControl
        game.progress.saveSettings()
    }

    private fun cycleSensitivity() {
        val settings = game.progress.settings
        val presets = floatArrayOf(.75f, 1.00f, 1.25f, 1.50f, 1.75f)
        val current = presets.indices.minByOrNull { kotlin.math.abs(presets[it] - settings.sensitivity) } ?: 1
        settings.sensitivity = presets[(current + 1) % presets.size]
        game.progress.saveSettings()
    }

    private fun renderLandscapeSettings() {
        begin("world_2_crystal")
        drawLandscapeScrim()
        title("SETTINGS", 1530f)
        val settings = game.progress.settings
        val topControlGap = 18f
        val topControlWidth = 360f
        val topControlsTotal = topControlWidth * 2f + topControlGap
        val topControlsX = 450f - topControlsTotal / 2f
        val controlMode = roundedButton(
            if (settings.relativeControl) "CONTROL RELATIVE" else "CONTROL DIRECT",
            topControlsX,
            1362f,
            topControlWidth,
            84f
        )
        val sensitivity = roundedButton(
            "SENS ${(settings.sensitivity * 100f).toInt()}%",
            topControlsX + topControlWidth + topControlGap,
            1362f,
            topControlWidth,
            84f
        )
        val contentWidth = responsiveContentWidth()
        val contentLeft = responsiveContentLeft()
        val gap = 28f
        val columnWidth = (contentWidth - gap) / 2f
        val left = Rectangle(contentLeft, 585f, columnWidth, 710f)
        val right = Rectangle(contentLeft + columnWidth + gap, 585f, columnWidth, 710f)
        game.assets.uiRenderer.drawGradientBorderPanel(batch, left, ForgeUiRenderer.GradientStyle.NEUTRAL, 4f)
        game.assets.uiRenderer.drawGradientBorderPanel(batch, right, ForgeUiRenderer.GradientStyle.NEUTRAL, 4f)
        fittedText(game.assets.hudLabelFont, "AUDIO", left.x + 28f, 1250f, left.width - 56f, .92f)
        fittedText(game.assets.hudLabelFont, "ACCESSIBILITY", right.x + 28f, 1250f, right.width - 56f, .92f)

        val (musicTrack, sfxTrack) = drawVolumeMixer(Rectangle(left.x + 24f, 930f, left.width - 48f, 245f))
        val haptic = landscapeSettingRow("HAPTICS", "Enable tactile feedback", Rectangle(left.x + 24f, 645f, left.width - 48f, 245f), settings.haptics)
        val motion = landscapeSettingRow("REDUCE MOTION", "Minimize motion effects", Rectangle(right.x + 24f, 1010f, right.width - 48f, 160f), settings.reduceMotion)
        val contrast = landscapeSettingRow("HIGH CONTRAST BALL", "Increase ball visibility", Rectangle(right.x + 24f, 820f, right.width - 48f, 160f), settings.highContrastBall)
        val color = landscapeSettingRow("COLOR BLIND PALETTE", "Use color-blind friendly colors", Rectangle(right.x + 24f, 630f, right.width - 48f, 160f), settings.colorBlind)

        val linksPanel = Rectangle(contentLeft, 270f, contentWidth, 280f)
        game.assets.uiRenderer.drawGradientBorderPanel(batch, linksPanel, ForgeUiRenderer.GradientStyle.NEUTRAL, 4f)
        fittedText(game.assets.hudLabelFont, "LINKS", linksPanel.x + 28f, linksPanel.y + linksPanel.height - 35f, linksPanel.width - 56f, .86f)
        val adPrivacyRequired = game.monetization.rewardedAdGateway.privacyOptionsRequired
        val linkCount = if (adPrivacyRequired) 4 else 3
        val linkGap = 24f
        val linkWidth = (linksPanel.width - 48f - linkGap * (linkCount - 1)) / linkCount
        var linkX = linksPanel.x + 24f
        val privacy = roundedButton("PRIVACY POLICY", linkX, linksPanel.y + 45f, linkWidth, 120f)
        linkX += linkWidth + linkGap
        val adPrivacy = if (adPrivacyRequired) roundedButton("AD PRIVACY", linkX, linksPanel.y + 45f, linkWidth, 120f).also { linkX += linkWidth + linkGap } else null
        val website = roundedButton("WEBSITE", linkX, linksPanel.y + 45f, linkWidth, 120f)
        linkX += linkWidth + linkGap
        val github = roundedButton("GITHUB", linkX, linksPanel.y + 45f, linkWidth, 120f)

        val cloud = roundedButton("CLOUD SAVE", contentLeft, 190f, 300f, 64f)
        fittedText(game.assets.smallFont, game.cloudProgress.message, contentLeft + 320f, 230f, contentWidth - 320f, .52f)
        val back = button("SAVE & BACK", 450f - 360f, 70f, 720f, 105f)
        end()
        handleMixerInput(musicTrack, sfxTrack)

        when {
            tapped(cloud) -> openCloudSave()

            tapped(controlMode) -> toggleControlMode()

            tapped(sensitivity) -> cycleSensitivity()

            activeVolumeChannel != null -> Unit

            tapped(haptic) -> {
                settings.haptics = !settings.haptics
                game.progress.saveSettings()
                if (settings.haptics) Gdx.input.vibrate(24)
            }

            tapped(motion) -> {
                settings.reduceMotion = !settings.reduceMotion
                game.progress.saveSettings()
            }

            tapped(contrast) -> {
                settings.highContrastBall = !settings.highContrastBall
                game.progress.saveSettings()
            }

            tapped(color) -> {
                settings.colorBlind = !settings.colorBlind
                game.progress.saveSettings()
            }

            tapped(privacy) -> game.navigate(
                PrivacyPolicyScreen(game, returnToPausedGame, returnToWorldMapWorld, returnToWorldMapMode),
                ScreenTransitionStyle.FADE_THROUGH
            )

            adPrivacy != null && tapped(adPrivacy) -> game.monetization.rewardedAdGateway.showPrivacyOptions()

            tapped(website) -> Gdx.net.openURI("https://bricks-breaker-ball.vercel.app")

            tapped(github) -> Gdx.net.openURI("https://github.com/msr7799")

            tapped(back) || Gdx.input.isKeyJustPressed(Input.Keys.BACK) -> saveAndReturn()
        }
    }

    /** ملاحظة صيانة: الدالة `render` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun render(delta: Float) {
        if (isLandscape) {
            renderLandscapeSettings()
            return
        }
        begin("world_2_crystal")
        title("SETTINGS")
        val settings = game.progress.settings
        val topGap = 10f
        val topWidth = (840f - topGap) / 2f
        val controlMode = roundedButton(
            if (settings.relativeControl) "RELATIVE" else "DIRECT",
            30f,
            1330f,
            topWidth,
            78f
        )
        val sensitivity = roundedButton(
            "SENS ${(settings.sensitivity * 100f).toInt()}%",
            30f + topWidth + topGap,
            1330f,
            topWidth,
            78f
        )
        val contentWidth = if (isLandscape) minOf(visibleWidth - 180f, 2100f) else 900f
        val contentLeft = if (isLandscape) 450f - contentWidth / 2f else 0f
        val columnGap = if (isLandscape) 70f else 0f
        val settingWidth = if (isLandscape) (contentWidth - columnGap) / 2f else 900f
        val leftX = contentLeft
        val rightX = contentLeft + settingWidth + columnGap
        val soundY = FIRST_SETTING_Y
        val hapticY = if (isLandscape) soundY - 230f else soundY - SETTING_STEP
        val motionY = if (isLandscape) hapticY - 230f else hapticY - SETTING_STEP
        val contrastY = if (isLandscape) soundY else motionY - SETTING_STEP
        val colorY = if (isLandscape) soundY - 230f else contrastY - SETTING_STEP
        val (musicTrack, sfxTrack) = drawVolumeMixer(Rectangle(leftX + 55f, soundY, settingWidth - 110f, 120f))
        val haptic = settingButton("haptics", leftX, hapticY, settingWidth)
        toggleIcon(settings.haptics, haptic)
        val motion = settingButton("motion", leftX, motionY, settingWidth)
        toggleIcon(settings.reduceMotion, motion)
        val contrast = settingButton("contrest-ball", if (isLandscape) rightX else leftX, contrastY, settingWidth)
        toggleIcon(settings.highContrastBall, contrast)
        val color = settingButton("color-blind-palette", if (isLandscape) rightX else leftX, colorY, settingWidth)
        toggleIcon(settings.colorBlind, color)
        val developerY = if (isLandscape) colorY - 300f else color.y - DEVELOPER_BLOCK_HEIGHT - DEVELOPER_SPACING
        batch.color = Color.WHITE
        // Larger developer artwork, still clear of the settings row and action buttons.
        batch.draw(
            game.assets.settingsMenuTexture("game-developer"),
            if (isLandscape) rightX + 40f else 60f,
            developerY + 7f,
            if (isLandscape) settingWidth - 80f else 780f,
            DEVELOPER_BLOCK_HEIGHT - 14f
        )

        val actionY = developerY - 102f
        val adPrivacyRequired = game.monetization.rewardedAdGateway.privacyOptionsRequired
        val privacy: Rectangle
        val adPrivacy: Rectangle?
        val website: Rectangle
        val github: Rectangle
        if (isLandscape) {
            val gap = 22f
            val count = if (adPrivacyRequired) 4 else 3
            val width = (contentWidth - gap * (count - 1)) / count
            privacy = roundedButton("PRIVACY POLICY", contentLeft, actionY, width, ACTION_BUTTON_HEIGHT)
            var nextX = contentLeft + width + gap
            adPrivacy = if (adPrivacyRequired) {
                roundedButton("AD PRIVACY", nextX, actionY, width, ACTION_BUTTON_HEIGHT).also { nextX += width + gap }
            } else {
                null
            }
            website = roundedButton("WEBSITE", nextX, actionY, width, ACTION_BUTTON_HEIGHT)
            github = imageButton("github", nextX + width + gap, actionY, width, ACTION_BUTTON_HEIGHT)
        } else if (adPrivacyRequired) {
            // Four equal production actions: privacy, AdMob consent options, game website, GitHub.
            val startX = 24f
            val gap = 18f
            val width = (852f - gap * 3f) / 4f
            privacy = roundedButton("PRIVACY POLICY", startX, actionY, width, ACTION_BUTTON_HEIGHT)
            adPrivacy = roundedButton(
                "AD PRIVACY",
                startX + width + gap,
                actionY,
                width,
                ACTION_BUTTON_HEIGHT
            )
            website = roundedButton(
                "WEBSITE",
                startX + (width + gap) * 2f,
                actionY,
                width,
                ACTION_BUTTON_HEIGHT
            )
            github = imageButton(
                "github",
                startX + (width + gap) * 3f,
                actionY,
                width,
                ACTION_BUTTON_HEIGHT
            )
        } else {
            // Three equal buttons, centered as one balanced row.
            val startX = 45f
            val gap = 22f
            val width = (810f - gap * 2f) / 3f
            privacy = roundedButton("PRIVACY POLICY", startX, actionY, width, ACTION_BUTTON_HEIGHT)
            adPrivacy = null
            website = roundedButton(
                "WEBSITE",
                startX + width + gap,
                actionY,
                width,
                ACTION_BUTTON_HEIGHT
            )
            github = imageButton(
                "github",
                startX + (width + gap) * 2f,
                actionY,
                width,
                ACTION_BUTTON_HEIGHT
            )
        }
        val cloud = roundedButton("CLOUD SAVE", 60f, 228f, 245f, 46f)
        fittedText(game.assets.smallFont, game.cloudProgress.message, 322f, 259f, 520f, .42f)
        batch.draw(game.assets.settingsMenuTexture("save&back"), 0f, 55f, 900f, 210f)
        val back = Rectangle(115f, 100f, 700f, 120f)
        end()
        handleMixerInput(musicTrack, sfxTrack)
        when {
            tapped(cloud) -> openCloudSave()

            tapped(controlMode) -> toggleControlMode()

            tapped(sensitivity) -> cycleSensitivity()

            activeVolumeChannel != null -> Unit

            tapped(haptic) -> {
                settings.haptics = !settings.haptics
                game.progress.saveSettings()
                if (settings.haptics) Gdx.input.vibrate(24)
            }

            tapped(motion) -> {
                settings.reduceMotion = !settings.reduceMotion
                game.progress.saveSettings()
            }

            tapped(contrast) -> {
                settings.highContrastBall = !settings.highContrastBall
                game.progress.saveSettings()
            }

            tapped(color) -> {
                settings.colorBlind = !settings.colorBlind
                game.progress.saveSettings()
            }

            tapped(privacy) -> game.navigate(
                PrivacyPolicyScreen(game, returnToPausedGame, returnToWorldMapWorld, returnToWorldMapMode),
                ScreenTransitionStyle.FADE_THROUGH
            )

            adPrivacy != null && tapped(adPrivacy) -> game.monetization.rewardedAdGateway.showPrivacyOptions()

            tapped(website) -> Gdx.net.openURI("https://bricks-breaker-ball.vercel.app")

            tapped(github) -> Gdx.net.openURI("https://github.com/msr7799")

            tapped(back) || Gdx.input.isKeyJustPressed(Input.Keys.BACK) -> saveAndReturn()
        }
    }
}
