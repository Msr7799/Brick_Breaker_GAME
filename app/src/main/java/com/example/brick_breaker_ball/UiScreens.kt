/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/main/java/com/example/brick_breaker_ball/UiScreens.kt
 * المؤلف: mohamed alromaihi
 * دالة التموضع المتجاوب للقائمة: `topAnchoredY`
 * دوال حاوية World Map: `beginWorldContentClip`، `endWorldContentClip`، `visibleWorldCard`
 * الدوال الموجودة: `begin`، `title`، `button`، `fittedText`، `starIcon`، `worldDoneIcon`، `tapped`، `touchPoint`، `end`، `resize`، `dispose`، `show`، `render`، `hide`، `pause`، `resume`، `startMenuMusic`، `artButton`، `floatingY`، `drawCover`، `mapCard`، `drawRoute`، `handleTouch`، `settingButton`، `toggleIcon`
 */

package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.ScreenAdapter
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.utils.viewport.ExtendViewport
import kotlin.math.abs
import kotlin.math.sin

abstract class ForgeScreen(protected val game: BrickBreakerGame) : ScreenAdapter() {
    companion object {
        const val DESIGN_WIDTH = 900f
        const val DESIGN_HEIGHT = 1600f
    }

    protected val camera = OrthographicCamera()
    protected val viewport = ExtendViewport(DESIGN_WIDTH, DESIGN_HEIGHT, camera)
    protected val batch = SpriteBatch()
    private val p = Vector3()

    protected val visibleWidth: Float get() = viewport.worldWidth
    protected val visibleHeight: Float get() = viewport.worldHeight
    protected val visibleLeft: Float get() = camera.position.x - visibleWidth / 2f
    protected val visibleBottom: Float get() = camera.position.y - visibleHeight / 2f

    init {
        camera.position.set(DESIGN_WIDTH / 2f, DESIGN_HEIGHT / 2f, 0f)
        camera.update()
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
                sourceHeight,
            )
        } else if (sourceRatio > targetRatio) {
            val sourceWidth = (region.regionHeight * targetRatio).toInt().coerceIn(1, region.regionWidth)
            cropped.setRegion(
                region.regionX + (region.regionWidth - sourceWidth) / 2,
                region.regionY,
                sourceWidth,
                region.regionHeight,
            )
        }
        batch.draw(cropped, visibleLeft, visibleBottom, visibleWidth, visibleHeight)
    }

    protected fun drawFullscreen(texture: Texture) = drawFullscreen(TextureRegion(texture))

    protected fun drawFullscreenPanel(region: TextureRegion) {
        batch.draw(region, visibleLeft, visibleBottom, visibleWidth, visibleHeight)
    }

    /** ملاحظة صيانة: الدالة `begin` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    protected fun begin(world: Int = 1, video: Boolean = false) {
        if (video) WorldVideoBackgrounds.show(world) else WorldVideoBackgrounds.hide()
        val videoVisible = WorldVideoBackgrounds.isVideoVisible()
        Gdx.gl.glClearColor(.003f, .008f, .025f, if (videoVisible) 0f else 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        viewport.apply()
        batch.projectionMatrix = camera.combined
        batch.begin()
        if (!videoVisible) {
            batch.color = Color(1f, 1f, 1f, .86f)
            drawFullscreen(game.assets.worldFallbackBg(world))
        }
        batch.color = Color.valueOf("080D1766")
        drawFullscreenPanel(game.assets.ui.findRegion("panel"))
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

    /** ملاحظة صيانة: الدالة `title` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    protected fun title(s: String, y: Float = 1480f) {
        game.assets.titleFont.draw(batch, s, 35f, y, 830f, Align.center, false)
    }

    /** ملاحظة صيانة: الدالة `button` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    protected fun button(text: String, x: Float, y: Float, w: Float = 650f, h: Float = 100f): Rectangle {
        val rect = Rectangle(x, y, w, h)
        game.assets.uiRenderer.drawGradientButton(batch, rect, ForgeUiRenderer.GradientStyle.PRIMARY)
        game.assets.buttonFont.color = ForgeUiPalette.textPrimary
        game.assets.buttonFont.draw(batch, text, x, y + h * .66f, w, Align.center, false)
        game.assets.buttonFont.color = Color.WHITE
        return rect
    }

    /** ملاحظة صيانة: الدالة `fittedText` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    protected fun fittedText(font: BitmapFont, text: String, x: Float, y: Float, width: Float, maxScale: Float = 1f) {
        val oldX = font.data.scaleX
        val oldY = font.data.scaleY
        font.data.setScale(maxScale)
        val measured = GlyphLayout(font, text).width
        if (measured > width) font.data.setScale(maxScale * width / measured)
        font.draw(batch, text, x, y, width, Align.center, false)
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
        if (!Gdx.input.justTouched()) return false
        p.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
        viewport.unproject(p)
        return r.contains(p.x, p.y)
    }

    /** ملاحظة صيانة: الدالة `touchPoint` تحوّل البيانات أو تبني المعرّف المتوافق مع بقية النظام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    protected fun touchPoint(): Vector3 {
        p.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
        viewport.unproject(p)
        return p
    }

    /** ملاحظة صيانة: الدالة `end` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    protected fun end() = batch.end()

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

    /** ملاحظة صيانة: الدالة `show` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun show() {
        WorldVideoBackgrounds.showSplash()
    }

    /** ملاحظة صيانة: الدالة `render` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun render(delta: Float) {
        time += delta
        WorldVideoBackgrounds.showSplash()
        val videoVisible = WorldVideoBackgrounds.isVideoVisible()
        Gdx.gl.glClearColor(0f, 0f, 0f, if (videoVisible) 0f else 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        viewport.apply()
        batch.projectionMatrix = camera.combined
        batch.begin()
        if (!videoVisible) {
            batch.color = Color.WHITE
            drawFullscreen(game.assets.splash)
            batch.color = Color(0f, 0f, .05f, .72f)
            batch.draw(game.assets.ui.findRegion("panel"), 55f, 90f, 790f, 270f)
            batch.color = Color.WHITE
            title("BRICK BREAKER BALL", 290f)
            game.assets.smallFont.draw(batch, "NEON INDUSTRIAL FORGE", 0f, 205f, 900f, Align.center, false)
        }
        batch.end()
        if (time >= 5f || Gdx.input.justTouched()) {
            WorldVideoBackgrounds.hide()
            game.openMenu()
        }
    }

    /** ملاحظة صيانة: الدالة `hide` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun hide() {
        WorldVideoBackgrounds.hide()
    }
}

class MainMenuScreen(game: BrickBreakerGame) : ForgeScreen(game) {
    private var confirmingNewGame = false
    private var animationTime = 0f

    private companion object {
        const val BUTTON_FLOAT_AMPLITUDE = 12f
        const val BUTTON_FLOAT_SPEED = .9f
        const val BUTTON_FLOAT_PHASE_STEP = .72f
        const val HEADER_ACTION_Y = 1506f
        const val HEADER_ACTION_SIZE = 88f
        const val NEW_GAME_X = 448f
        const val HEADER_ACTION_TOP_INSET = 110f
        const val NEW_GAME_TOP_INSET = 135f
        const val SIDE_ACTION_TOP_INSET = 350f
        const val DEVELOPMENT_BOTTOM_INSET = 24f
    }

    /** ملاحظة صيانة: الدالة `show` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun show() {
        WorldVideoBackgrounds.hide()
        startMenuMusic()
    }

    /** ملاحظة صيانة: الدالة `hide` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun hide() = game.assets.stopMenuMusic()

    /** ملاحظة صيانة: الدالة `pause` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun pause() = game.assets.stopMenuMusic()

    /** ملاحظة صيانة: الدالة `resume` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun resume() = startMenuMusic()

    /** ملاحظة صيانة: الدالة `startMenuMusic` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun startMenuMusic() = game.assets.startMenuMusic(
        game.progress.settings.masterVolume * game.progress.settings.musicVolume
    )

    /** ملاحظة صيانة: الدالة `artButton` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun artButton(texture: Texture, x: Float, y: Float, width: Float, height: Float): Rectangle {
        batch.color = Color.WHITE
        batch.draw(texture, x, y, width, height)
        return Rectangle(x, y, width, height)
    }

    /** ملاحظة صيانة: الدالة `floatingY` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun floatingY(baseY: Float, index: Int): Float {
        if (game.progress.settings.reduceMotion) return baseY
        val phase = animationTime * BUTTON_FLOAT_SPEED + index * BUTTON_FLOAT_PHASE_STEP
        return baseY + sin(phase) * BUTTON_FLOAT_AMPLITUDE
    }

    /** يثبت أدوات القائمة عند حافة الشاشة العليا مع الحفاظ على مكانها في الشاشات الأقصر. */
    private fun topAnchoredY(defaultY: Float, topInset: Float): Float =
        maxOf(defaultY, visibleBottom + visibleHeight - topInset)

    /** ملاحظة صيانة: الدالة `render` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun render(delta: Float) {
        animationTime += delta.coerceAtMost(.05f)
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        viewport.apply()
        batch.projectionMatrix = camera.combined
        batch.begin()
        batch.color = Color.WHITE
        drawFullscreen(game.assets.startScreenBackground)

        val headerActionY = topAnchoredY(HEADER_ACTION_Y, HEADER_ACTION_TOP_INSET)
        val settings = artButton(
            game.assets.startMenuTexture("setting"),
            70f,
            headerActionY,
            HEADER_ACTION_SIZE,
            HEADER_ACTION_SIZE
        )
        val soundName = if (game.progress.settings.masterVolume > 0f) "sound-on" else "sound-off"
        val sound = artButton(
            game.assets.startMenuTexture(soundName),
            166f,
            headerActionY,
            HEADER_ACTION_SIZE,
            HEADER_ACTION_SIZE
        )
        val newGame = artButton(
            game.assets.startMenuTexture("new-game"),
            NEW_GAME_X,
            topAnchoredY(1455f, NEW_GAME_TOP_INSET),
            500f,
            116f
        )
        val sideActionY = topAnchoredY(1280f, SIDE_ACTION_TOP_INSET)
        val info = artButton(game.assets.startMenuTexture("info"), 35f, sideActionY, 72f, 72f)
        val exit = artButton(game.assets.startMenuTexture("exit"), 805f, sideActionY, 72f, 72f)
        val development = artButton(
            game.assets.startMenuTexture(if (game.developmentAccess.enabled) "code-on" else "code-off"),
            24f, visibleBottom + DEVELOPMENT_BOTTOM_INSET, 112f, 112f
        )

        val returningPlayer = game.progress.hasStartedGame || game.progress.unlockedLevel > 1 ||
            game.progress.stars(1) > 0 || game.pausedSession.hasPausedGame()
        val primaryTexture = game.assets.startMenuTexture(if (returningPlayer) "continue" else "start")
        // Level Editor is an intentional public feature in this build, not a debug-only tool.
        // Six matching art cards fit below the header without shrinking their touch targets.
        val primary = artButton(primaryTexture, 85f, floatingY(1070f, 0), 730f, 145f)
        val worldMap = artButton(game.assets.startMenuTexture("world-map"), 85f, floatingY(905f, 1), 730f, 145f)
        val shop = artButton(game.assets.startMenuTexture("shop"), 85f, floatingY(740f, 2), 730f, 145f)
        val charmsBag = artButton(game.assets.startMenuTexture("charms-bag"), 85f, floatingY(575f, 3), 730f, 145f)
        val editor = artButton(game.assets.startMenuTexture("level-editor"), 85f, floatingY(410f, 4), 730f, 145f)
        val customize = artButton(game.assets.startMenuTexture("paddle&balls"), 85f, floatingY(245f, 5), 730f, 145f)

        if (confirmingNewGame) {
            batch.color = Color(.02f, .06f, .13f, .97f)
            batch.draw(game.assets.ui.findRegion("panel"), 90f, 530f, 720f, 560f)
            batch.color = Color.WHITE
            game.assets.titleFont.draw(batch, "NEW GAME?", 0f, 1010f, 900f, Align.center, false)
            game.assets.bodyFont.draw(batch, "Start again from Level 1?\nSaved stars and best scores stay safe.", 130f, 885f, 640f, Align.center, true)
            val confirm = button("YES, START NEW GAME", 160f, 650f, 580f, 100f)
            val cancel = button("CANCEL", 160f, 520f, 580f, 100f)
            end()
            when {
                tapped(confirm) -> game.startNewGame()
                tapped(cancel) -> confirmingNewGame = false
            }
            return
        }
        end()
        when {
            tapped(settings) -> game.setScreen(SettingsScreen(game))

            tapped(sound) -> {
                game.progress.settings.masterVolume = if (game.progress.settings.masterVolume > 0f) 0f else .8f
                game.progress.saveSettings()
                if (game.progress.settings.masterVolume > 0f) startMenuMusic() else game.assets.stopMenuMusic()
            }

            tapped(newGame) -> confirmingNewGame = true

            tapped(info) -> game.setScreen(RulesScreen(game))

            tapped(exit) -> Gdx.app.exit()

            tapped(development) -> {
                val enabled = game.developmentAccess.toggle()
                if (!enabled) game.cosmeticProgression.reconcile(game.progress)
                game.assets.play("ui_click", game.progress.settings.masterVolume * game.progress.settings.sfxVolume)
            }

            tapped(primary) -> if (returningPlayer && game.pausedSession.hasPausedGame()) {
                game.resumePausedGame()
            } else {
                game.play(if (returningPlayer) game.progress.unlockedLevel else 1)
            }

            tapped(worldMap) -> game.setScreen(WorldMapScreen(game))

            tapped(shop) -> game.setScreen(ShopScreen(game, ShopReturnDestination.MAIN_MENU))

            tapped(charmsBag) -> game.setScreen(CharmsBagScreen(game, ShopReturnDestination.MAIN_MENU))

            tapped(editor) -> game.setScreen(LevelEditorScreen(game))

            tapped(customize) -> game.setScreen(CustomizationScreen(game))
        }
    }
}

class WorldMapScreen(game: BrickBreakerGame) : ForgeScreen(game) {
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
    private val cards = mutableListOf<Pair<Rectangle, Int>>()
    private val maxScroll: Float
        get() {
            val rows = (LevelRepository.worlds.size + 1) / 2
            val lastRowY = CARD_START_Y - (rows - 1) * CARD_ROW_SPACING
            return (FOOTER_TOP + CONTENT_BOTTOM_PADDING - lastRowY).coerceAtLeast(0f)
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
    private fun mapCard(world: WorldDefinition, unlocked: Boolean, x: Float, y: Float): Rectangle {
        val width = CARD_WIDTH
        val height = CARD_HEIGHT
        val imageHeight = 212f
        val preview = game.assets.worldMapImage(world.id)
        game.assets.uiRenderer.drawGradientBorderPanel(
            batch,
            Rectangle(x, y, width, height),
            if (unlocked) ForgeUiRenderer.GradientStyle.PRIMARY else ForgeUiRenderer.GradientStyle.NEUTRAL,
            5f,
        )
        drawCover(preview, x + 6f, y + 74f, width - 12f, imageHeight)
        if (!unlocked) {
            batch.color = Color(0f, 0f, 0f, .56f)
            batch.draw(game.assets.ui.findRegion("panel"), x + 6f, y + 74f, width - 12f, imageHeight)
        }
        batch.color = Color(.02f, .08f, .15f, .94f)
        batch.draw(game.assets.ui.findRegion("panel"), x + 6f, y + 6f, width - 12f, 70f)
        batch.color = Color.WHITE
        fittedText(game.assets.bodyFont, "${world.id}. ${world.name}", x + 12f, y + 61f, width - 24f, .79f)
        fittedText(game.assets.smallFont, if (unlocked) world.subtitle else "LOCKED • TAP TO PREVIEW", x + 12f, y + 27f, width - 24f, .70f)
        return Rectangle(x, y, width, height)
    }

    /** يبدأ قص شبكة العوالم بين الهيدر والفوتر الثابتين. */
    private fun beginWorldContentClip() {
        batch.flush()
        val bottom = Vector3(0f, FOOTER_TOP, 0f)
        val top = Vector3(DESIGN_WIDTH, HEADER_BOTTOM, 0f)
        viewport.project(bottom)
        viewport.project(top)
        Gdx.gl.glEnable(GL20.GL_SCISSOR_TEST)
        Gdx.gl.glScissor(
            bottom.x.toInt(),
            bottom.y.toInt(),
            (top.x - bottom.x).toInt().coerceAtLeast(1),
            (top.y - bottom.y).toInt().coerceAtLeast(1),
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
        val points = LevelRepository.worlds.indices.map { index ->
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

    /** ملاحظة صيانة: الدالة `render` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun render(delta: Float) {
        begin("world_4_zerog")
        cards.clear()
        beginWorldContentClip()
        drawRoute()
        LevelRepository.worlds.forEachIndexed { index, world ->
            val row = index / 2
            val col = if (row % 2 == 0) index % 2 else 1 - index % 2
            val x = 55f + col * 405f
            val y = CARD_START_Y - row * CARD_ROW_SPACING + scrollOffset
            val firstLevel = LevelRepository.firstLevel(world.id)
            val lastLevel = LevelRepository.lastLevel(world.id)
            val completed = (firstLevel..lastLevel).count { game.progress.stars(it) > 0 }
            val unlocked = game.developmentAccess.canSelectWorld(world.id, game.progress.unlockedLevel)
            val rect = mapCard(world, unlocked, x, y)
            visibleWorldCard(rect)?.let { cards += it to world.id }
            if (unlocked && completed > 0) {
                val iconSize = 33f
                val gap = 5f
                val starsWidth = completed * iconSize + (completed - 1) * gap
                val starsX = x + (CARD_WIDTH - starsWidth) / 2f
                repeat(completed) { starIcon(starsX + it * (iconSize + gap), y + 79f, iconSize) }
            }
            if (completed == LevelRepository.worldLevelCount(world.id)) {
                worldDoneIcon(x + 326f, y + 234f, 48f)
            }
        }
        endWorldContentClip()
        game.assets.uiRenderer.drawGradientPanel(batch, Rectangle(0f, 1400f, 900f, 200f), ForgeUiRenderer.GradientStyle.NEUTRAL)
        title("WORLD MAP", 1520f)
        fittedText(game.assets.smallFont, "SWIPE UP OR DOWN • TAP ANY WORLD TO PREVIEW", 45f, 1430f, 810f, .72f)
        game.assets.uiRenderer.drawGradientPanel(batch, Rectangle(0f, 0f, 900f, 150f), ForgeUiRenderer.GradientStyle.NEUTRAL)
        val mainMenu = button("MAIN MENU", 250f, 35f, 400f, 90f)
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
            touchStartX = point.x
            touchStartY = point.y
            touchLastX = point.x
            touchLastY = point.y
        }
        if (touchActive && isTouched) {
            val point = touchPoint()
            val delta = point.y - touchLastY
            scrollOffset = (scrollOffset + delta).coerceIn(0f, maxScroll)
            touchLastX = point.x
            touchLastY = point.y
            if (abs(point.y - touchStartY) > 24f || abs(point.x - touchStartX) > 24f) touchMoved = true
        } else if (touchActive) {
            if (!touchMoved) {
                cards.firstOrNull { it.first.contains(touchLastX, touchLastY) }?.let {
                    game.setScreen(LevelSelectScreen(game, it.second))
                }
            }
            touchActive = false
        }
    }
}

class LevelSelectScreen(game: BrickBreakerGame, private val world: Int) : ForgeScreen(game) {
    /** ملاحظة صيانة: الدالة `render` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun render(delta: Float) {
        begin(world, video = true)
        title(LevelRepository.worlds[world - 1].name)
        val worldUnlocked = game.developmentAccess.canSelectWorld(world, game.progress.unlockedLevel)
        val worldSubtitle = when {
            !worldUnlocked -> "WORLD PREVIEW • COMPLETE EARLIER WORLDS TO UNLOCK"
            game.developmentAccess.enabled -> "DEVELOPMENT ACCESS • ${LevelRepository.worlds[world - 1].subtitle}"
            else -> LevelRepository.worlds[world - 1].subtitle
        }
        fittedText(game.assets.smallFont, worldSubtitle, 45f, 1375f, 810f, .9f)
        val cells = mutableListOf<Pair<Rectangle, Int>>()
        for (stage in 1..LevelRepository.worldLevelCount(world)) {
            val id = LevelRepository.firstLevel(world) + stage - 1
            val col = (stage - 1) % 3
            val row = (stage - 1) / 3
            val x = 75f + col * 260f
            val y = 1190f - row * 190f
            val open = game.developmentAccess.canSelectLevel(id, game.progress.unlockedLevel)
            val completed = game.progress.stars(id) > 0
            val rect = button(if (open) stage.toString() else "LOCK", x, y, 230f, 125f)
            cells += rect to id
            if (completed) starIcon(x + 158f, y + 39f, 42f)
        }
        val back = button("BACK", 250f, 120f, 400f, 90f)
        end()
        cells.firstOrNull { tapped(it.first) }?.let {
            if (game.developmentAccess.canSelectLevel(it.second, game.progress.unlockedLevel)) game.play(it.second)
        }
        if (tapped(back)) game.setScreen(WorldMapScreen(game))
    }
}

class ResultsScreen(
    game: BrickBreakerGame,
    private val level: LevelDefinition,
    private val score: Int,
    private val stars: Int,
    private val unlocks: List<CosmeticUnlock> = emptyList()
) : ForgeScreen(game) {
    private var revealTime = 0f
    private var inputArmed = false

    override fun show() {
        revealTime = 0f
        inputArmed = false
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
    ) {
        if (alpha <= 0f) return
        val oldColor = Color(font.color)
        val oldScaleX = font.data.scaleX
        val oldScaleY = font.data.scaleY
        val baseColor = color ?: oldColor
        font.color = Color(baseColor.r, baseColor.g, baseColor.b, alpha)
        font.data.setScale(oldScaleX * scale, oldScaleY * scale)
        font.draw(batch, text, x, y, width, Align.center, false)
        font.data.setScale(oldScaleX, oldScaleY)
        font.color = oldColor
    }

    /** Completion screen with a staggered, smooth reveal and delayed input. */
    override fun render(delta: Float) {
        revealTime = (revealTime + delta.coerceAtMost(.1f)).coerceAtMost(1.4f)
        begin(level.world, video = true)

        val titleReveal = reveal(0f, .30f)
        drawAnimatedText(
            game.assets.titleFont,
            "LEVEL COMPLETE",
            35f,
            1480f - 22f * (1f - titleReveal),
            830f,
            titleReveal,
            .94f + .06f * titleReveal,
        )

        val iconSize = 92f
        val gap = 18f
        val starsWidth = stars * iconSize + (stars - 1) * gap
        val starsX = (900f - starsWidth) / 2f
        repeat(stars) { index ->
            val starReveal = reveal(.12f + index * .11f, .30f)
            if (starReveal > 0f) {
                batch.color = Color(1f, 1f, 1f, starReveal)
                batch.draw(
                    game.assets.starIcon,
                    starsX + index * (iconSize + gap),
                    1080f - 26f * (1f - starReveal),
                    iconSize,
                    iconSize,
                )
                batch.color = Color.WHITE
            }
        }

        val scoreReveal = reveal(.38f, .30f)
        drawAnimatedText(
            game.assets.bodyFont,
            "SCORE  $score",
            0f,
            1020f - 18f * (1f - scoreReveal),
            900f,
            scoreReveal,
        )

        if (unlocks.isNotEmpty()) {
            val rewardReveal = reveal(.48f, .30f)
            drawAnimatedText(
                game.assets.hudLabelFont,
                "NEW COLLECTION REWARD READY",
                75f,
                900f - 16f * (1f - rewardReveal),
                750f,
                rewardReveal,
                color = ForgeUiPalette.gold,
            )
        }

        val buttonReveal = reveal(.46f, .42f)
        var next = Rectangle()
        var retry = Rectangle()
        var menu = Rectangle()
        if (buttonReveal > 0f) {
            val slide = 42f * (1f - buttonReveal)
            next = button(
                if (unlocks.isNotEmpty()) {
                    "VIEW NEW REWARDS"
                } else if (level.id < LevelRepository.TOTAL_LEVELS) {
                    "NEXT LEVEL"
                } else {
                    "CAMPAIGN COMPLETE"
                },
                125f,
                760f - slide,
            )
            if (unlocks.isEmpty()) {
                retry = button("REPLAY", 125f, 630f - slide)
                menu = button("MAIN MENU", 125f, 500f - slide)
            }
        }
        end()

        // Ignore the touch that may still be held from the last gameplay frame. Only arm
        // controls after the reveal has started and the player has released the screen.
        if (!inputArmed) {
            if (revealTime >= .50f && !Gdx.input.isTouched) inputArmed = true
            return
        }

        when {
            tapped(next) -> {
                val destination = { if (level.id < LevelRepository.TOTAL_LEVELS) game.play(level.id + 1) else game.openMenu() }
                if (unlocks.isNotEmpty()) game.setScreen(CosmeticUnlockRevealScreen(game, unlocks, destination)) else destination()
            }

            unlocks.isEmpty() && tapped(retry) -> game.play(level.id)

            unlocks.isEmpty() && tapped(menu) -> game.openMenu()
        }
    }
}

class CustomTestResultScreen(
    game: BrickBreakerGame,
    private val editor: LevelEditorState,
    private val score: Int,
    private val stars: Int
) : ForgeScreen(game) {
    /** ملاحظة صيانة: الدالة `render` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun render(delta: Float) {
        begin(editor.properties.world)
        title("CUSTOM TEST COMPLETE")
        game.assets.bodyFont.draw(batch, "SCORE  $score   •   STARS  $stars", 0f, 1120f, 900f, Align.center, false)
        game.assets.smallFont.draw(batch, "Campaign progress and paid Items were not changed.", 70f, 1010f, 760f, Align.center, true)
        val replay = button("REPLAY TEST", 125f, 760f)
        val exit = button("EXIT TEST TO EDITOR", 125f, 620f)
        end()
        when {
            tapped(replay) -> game.playCustom(editor)
            tapped(exit) -> game.setScreen(LevelEditorScreen(game, editor))
        }
    }
}

class SettingsScreen(game: BrickBreakerGame, private val returnToPausedGame: Boolean = false) : ForgeScreen(game) {
    private companion object {
        const val FIRST_SETTING_Y = 1160f
        const val SETTING_STEP = 136f
        const val SETTING_TOUCH_HEIGHT = 114f
        const val SETTING_ART_HEIGHT = 200f
        const val SETTING_ART_BOTTOM_OFFSET = 45f
        const val DEVELOPER_BLOCK_HEIGHT = 210f
        const val DEVELOPER_SPACING = 24f
    }

    /** ملاحظة صيانة: الدالة `settingButton` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun settingButton(assetName: String, y: Float): Rectangle {
        batch.color = Color.WHITE
        batch.draw(game.assets.settingsMenuTexture(assetName), 0f, y - SETTING_ART_BOTTOM_OFFSET, 900f, SETTING_ART_HEIGHT)
        return Rectangle(115f, y, 700f, SETTING_TOUCH_HEIGHT)
    }

    /** ملاحظة صيانة: الدالة `toggleIcon` تحوّل البيانات أو تبني المعرّف المتوافق مع بقية النظام؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun toggleIcon(enabled: Boolean, y: Float, sound: Boolean = false) {
        batch.color = Color.WHITE
        val texture = if (sound) {
            game.assets.startMenuTexture(if (enabled) "sound-on" else "sound-off")
        } else {
            game.assets.settingsMenuTexture(if (enabled) "check-on" else "check-off")
        }
        val size = if (sound) 72f else 48f
        val x = if (sound) 684f else 696f
        batch.draw(texture, x, y + (SETTING_TOUCH_HEIGHT - size) / 2f, size, size)
    }

    /** ملاحظة صيانة: الدالة `render` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun render(delta: Float) {
        begin("world_2_crystal")
        title("SETTINGS")
        val settings = game.progress.settings
        val soundY = FIRST_SETTING_Y
        val hapticY = soundY - SETTING_STEP
        val motionY = hapticY - SETTING_STEP
        val contrastY = motionY - SETTING_STEP
        val colorY = contrastY - SETTING_STEP
        val sound = settingButton("sound", soundY)
        toggleIcon(settings.masterVolume > 0f, soundY, sound = true)
        val haptic = settingButton("haptics", hapticY)
        toggleIcon(settings.haptics, hapticY)
        val motion = settingButton("motion", motionY)
        toggleIcon(settings.reduceMotion, motionY)
        val contrast = settingButton("contrest-ball", contrastY)
        toggleIcon(settings.highContrastBall, contrastY)
        val color = settingButton("color-blind-palette", colorY)
        toggleIcon(settings.colorBlind, colorY)
        val developerY = color.y - DEVELOPER_BLOCK_HEIGHT - DEVELOPER_SPACING
        batch.color = Color.WHITE
        batch.draw(game.assets.settingsMenuTexture("game-developer"), 0f, developerY, 900f, DEVELOPER_BLOCK_HEIGHT)
        val actionY = developerY - 100f
        val privacy = button("PRIVACY POLICY", 115f, actionY, 325f, 86f)
        val github = button("GITHUB", 460f, actionY, 325f, 86f)
        batch.draw(game.assets.settingsMenuTexture("save&back"), 0f, 55f, 900f, 210f)
        val back = Rectangle(115f, 100f, 700f, 120f)
        end()
        when {
            tapped(sound) -> {
                settings.masterVolume = if (settings.masterVolume > 0f) 0f else .8f
                game.progress.saveSettings()
                game.applyAudioSettings()
            }

            tapped(haptic) -> settings.haptics = !settings.haptics

            tapped(motion) -> settings.reduceMotion = !settings.reduceMotion

            tapped(contrast) -> settings.highContrastBall = !settings.highContrastBall

            tapped(color) -> settings.colorBlind = !settings.colorBlind

            tapped(privacy) -> game.setScreen(PrivacyPolicyScreen(game, returnToPausedGame))

            tapped(github) -> Gdx.net.openURI("https://github.com/msr7799")

            tapped(back) -> {
                game.progress.saveSettings()
                if (returnToPausedGame) game.resumePausedGame() else game.openMenu()
            }
        }
    }
}
