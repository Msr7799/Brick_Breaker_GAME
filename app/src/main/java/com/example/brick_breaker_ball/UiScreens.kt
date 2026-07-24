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
import com.badlogic.gdx.utils.viewport.FitViewport
import kotlin.math.abs
import kotlin.math.sin

abstract class ForgeScreen(protected val game: BrickBreakerGame) : ScreenAdapter() {
    protected val camera=OrthographicCamera();protected val viewport=FitViewport(900f,1600f,camera);protected val batch=SpriteBatch();private val p=Vector3()
    protected fun begin(world:Int=1, video:Boolean=false){if(video)WorldVideoBackgrounds.show(world) else WorldVideoBackgrounds.hide();val videoVisible=WorldVideoBackgrounds.isVideoVisible();Gdx.gl.glClearColor(.003f,.008f,.025f,if(videoVisible)0f else 1f);Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);viewport.apply();batch.projectionMatrix=camera.combined;batch.begin();if(!videoVisible){batch.color=Color(.52f,.62f,.76f,.76f);batch.draw(game.assets.worldFallbackBg(world),0f,0f,900f,1600f)};batch.color=Color(.01f,.025f,.07f,.28f);batch.draw(game.assets.ui.findRegion("panel"),0f,0f,900f,1600f);batch.color=Color.WHITE}
    protected fun begin(background:String)=begin(when(background){"world_2_crystal"->2;"world_3_magma"->3;"world_4_zerog"->4;else->1})
    protected fun title(s:String,y:Float=1480f){game.assets.titleFont.draw(batch,s,35f,y,830f,Align.center,false)}
    protected fun button(text:String,x:Float,y:Float,w:Float=650f,h:Float=100f):Rectangle{batch.color=Color(.35f,.85f,1f,.9f);batch.draw(game.assets.ui.findRegion("button_primary"),x,y,w,h);batch.color=Color.WHITE;game.assets.bodyFont.draw(batch,text,x,y+h*.65f,w,Align.center,false);return Rectangle(x,y,w,h)}
    protected fun fittedText(font: BitmapFont, text: String, x: Float, y: Float, width: Float, maxScale: Float = 1f) {
        val oldX = font.data.scaleX
        val oldY = font.data.scaleY
        font.data.setScale(maxScale)
        val measured = GlyphLayout(font, text).width
        if (measured > width) font.data.setScale(maxScale * width / measured)
        font.draw(batch, text, x, y, width, Align.center, false)
        font.data.setScale(oldX, oldY)
    }
    protected fun starIcon(x:Float,y:Float,size:Float){batch.color=Color.WHITE;batch.draw(game.assets.starIcon,x,y,size,size)}
    protected fun worldDoneIcon(x:Float,y:Float,size:Float){batch.color=Color.WHITE;batch.draw(game.assets.worldDoneIcon,x,y,size,size)}
    protected fun tapped(r:Rectangle):Boolean{if(!Gdx.input.justTouched())return false;p.set(Gdx.input.x.toFloat(),Gdx.input.y.toFloat(),0f);viewport.unproject(p);return r.contains(p.x,p.y)}
    protected fun touchPoint():Vector3 { p.set(Gdx.input.x.toFloat(),Gdx.input.y.toFloat(),0f); viewport.unproject(p); return p }
    protected fun end()=batch.end();override fun resize(w:Int,h:Int)=viewport.update(w,h,true);override fun dispose()=batch.dispose()
}

class SplashScreen(game:BrickBreakerGame):ForgeScreen(game){private var time=0f
    override fun show(){WorldVideoBackgrounds.showSplash()}
    override fun render(delta:Float){
        time+=delta
        WorldVideoBackgrounds.showSplash()
        val videoVisible=WorldVideoBackgrounds.isVideoVisible()
        Gdx.gl.glClearColor(0f,0f,0f,if(videoVisible)0f else 1f);Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        viewport.apply();batch.projectionMatrix=camera.combined;batch.begin()
        if(!videoVisible){batch.color=Color.WHITE;batch.draw(game.assets.splash,0f,0f,900f,1600f);batch.color=Color(0f,0f,.05f,.72f);batch.draw(game.assets.ui.findRegion("panel"),55f,90f,790f,270f);batch.color=Color.WHITE;title("BRICK BREAKER BALL",290f);game.assets.smallFont.draw(batch,"NEON INDUSTRIAL FORGE",0f,205f,900f,Align.center,false)}
        batch.end()
        if(time>=5f||Gdx.input.justTouched()){WorldVideoBackgrounds.hide();game.openMenu()}
    }
    override fun hide(){WorldVideoBackgrounds.hide()}
}

class MainMenuScreen(game: BrickBreakerGame) : ForgeScreen(game) {
    private var confirmingNewGame = false
    private var showingInfo = false
    private var animationTime = 0f

    override fun show() {
        WorldVideoBackgrounds.hide()
        startMenuMusic()
    }

    override fun hide() = game.assets.stopMenuMusic()
    override fun pause() = game.assets.stopMenuMusic()
    override fun resume() = startMenuMusic()

    private fun startMenuMusic() = game.assets.startMenuMusic(
        game.progress.settings.masterVolume * game.progress.settings.musicVolume,
    )

    private fun artButton(texture: Texture, x: Float, y: Float, width: Float, height: Float): Rectangle {
        batch.color = Color.WHITE
        batch.draw(texture, x, y, width, height)
        return Rectangle(x, y, width, height)
    }

    private fun floatingY(baseY: Float, index: Int): Float {
        if (game.progress.settings.reduceMotion) return baseY
        return baseY + sin(animationTime * .72f + index * .82f) * 11f
    }

    override fun render(delta: Float) {
        animationTime += delta.coerceAtMost(.05f)
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f); Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        viewport.apply(); batch.projectionMatrix = camera.combined; batch.begin()
        batch.color = Color.WHITE; batch.draw(game.assets.startScreenBackground, 0f, 0f, 900f, 1600f)

        val settings = artButton(game.assets.startMenuTexture("setting"), 70f, 1500f, 88f, 88f)
        val soundName = if (game.progress.settings.masterVolume > 0f) "sound-on" else "sound-off"
        val sound = artButton(game.assets.startMenuTexture(soundName), 166f, 1500f, 88f, 88f)
        val newGame = artButton(game.assets.startMenuTexture("new-game"), 398f, 1455f, 500f, 116f)
        val info = artButton(game.assets.startMenuTexture("info"), 35f, 1280f, 72f, 72f)
        val exit = artButton(game.assets.startMenuTexture("exit"), 805f, 1280f, 72f, 72f)

        val returningPlayer = game.progress.hasStartedGame || game.progress.unlockedLevel > 1 ||
            game.progress.stars(1) > 0 || game.pausedSession.hasPausedGame()
        val primaryTexture = game.assets.startMenuTexture(if (returningPlayer) "continue" else "start")
        val primary = artButton(primaryTexture, 85f, floatingY(1070f, 0), 730f, 170f)
        val worldMap = artButton(game.assets.startMenuTexture("world-map"), 85f, floatingY(835f, 1), 730f, 170f)
        val shop = artButton(game.assets.startMenuTexture("shop"), 85f, floatingY(600f, 2), 730f, 170f)
        val editor = artButton(game.assets.startMenuTexture("level-editor"), 85f, floatingY(365f, 3), 730f, 170f)
        val customize = artButton(game.assets.startMenuTexture("paddle&balls"), 85f, floatingY(130f, 4), 730f, 170f)

        if (confirmingNewGame) {
            batch.color = Color(.02f, .06f, .13f, .97f); batch.draw(game.assets.ui.findRegion("panel"), 90f, 530f, 720f, 560f); batch.color = Color.WHITE
            game.assets.titleFont.draw(batch, "NEW GAME?", 0f, 1010f, 900f, Align.center, false)
            game.assets.bodyFont.draw(batch, "Start again from Level 1?\nSaved stars and best scores stay safe.", 130f, 885f, 640f, Align.center, true)
            val confirm = button("YES, START NEW GAME", 160f, 650f, 580f, 100f)
            val cancel = button("CANCEL", 160f, 520f, 580f, 100f)
            end()
            when { tapped(confirm) -> game.startNewGame(); tapped(cancel) -> confirmingNewGame = false }
            return
        }
        if (showingInfo) {
            batch.color = Color(.02f, .06f, .13f, .97f); batch.draw(game.assets.ui.findRegion("panel"), 90f, 500f, 720f, 590f); batch.color = Color.WHITE
            game.assets.titleFont.draw(batch, "BRICK BREAKER BALL", 110f, 1010f, 680f, Align.center, false)
            game.assets.bodyFont.draw(batch, "${LevelRepository.TOTAL_LEVELS} LEVELS • ${LevelRepository.worlds.size} WORLDS\n\nBreak every target. Master every world.\nCustomize your paddle and ball.", 135f, 880f, 630f, Align.center, true)
            val close = button("CLOSE", 210f, 550f, 480f, 95f)
            end()
            if (tapped(close)) showingInfo = false
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
            tapped(info) -> showingInfo = true
            tapped(exit) -> Gdx.app.exit()
            tapped(primary) -> if (returningPlayer && game.pausedSession.hasPausedGame()) game.resumePausedGame()
                else game.play(if (returningPlayer) game.progress.unlockedLevel else 1)
            tapped(worldMap) -> game.setScreen(WorldMapScreen(game))
            tapped(shop) -> game.setScreen(ShopScreen(game,ShopReturnDestination.MAIN_MENU))
            tapped(editor) -> game.setScreen(LevelEditorScreen(game))
            tapped(customize) -> game.setScreen(CustomizationScreen(game))
        }
    }
}

class WorldMapScreen(game: BrickBreakerGame) : ForgeScreen(game) {
    private var scrollOffset = 0f
    private var touchActive = false
    private var touchMoved = false
    private var touchStartX = 0f
    private var touchStartY = 0f
    private var touchLastX = 0f
    private var touchLastY = 0f
    private val cards = mutableListOf<Pair<Rectangle, Int>>()
    private val maxScroll = 1060f

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

    private fun mapCard(world: WorldDefinition, unlocked: Boolean, x: Float, y: Float): Rectangle {
        val width = 380f
        val height = 280f
        val imageHeight = 212f
        val preview = game.assets.worldMapImage(world.id)
        batch.color = Color(.35f, .85f, 1f, .95f)
        batch.draw(game.assets.ui.findRegion("button_primary"), x, y, width, height)
        batch.color = Color.WHITE
        drawCover(preview, x + 6f, y + 62f, width - 12f, imageHeight)
        if (!unlocked) {
            batch.color = Color(0f, 0f, 0f, .56f)
            batch.draw(game.assets.ui.findRegion("panel"), x + 6f, y + 62f, width - 12f, imageHeight)
        }
        batch.color = Color(.02f, .08f, .15f, .94f)
        batch.draw(game.assets.ui.findRegion("panel"), x + 6f, y + 6f, width - 12f, 58f)
        batch.color = Color.WHITE
        fittedText(game.assets.bodyFont, "${world.id}. ${world.name}", x + 12f, y + 53f, width - 24f, .67f)
        fittedText(game.assets.smallFont, if (unlocked) world.subtitle else "LOCKED • TAP TO PREVIEW", x + 12f, y + 23f, width - 24f, .62f)
        return Rectangle(x, y, width, height)
    }

    private fun drawRoute() {
        val points = LevelRepository.worlds.indices.map { index ->
            val row = index / 2
            val leftToRight = row % 2 == 0
            val visualColumn = if (leftToRight) index % 2 else 1 - index % 2
            (245f + visualColumn * 405f) to (1200f - row * 320f + scrollOffset)
        }
        val glow = game.assets.particles.findRegion("glow")
        points.zipWithNext().forEach { (start, end) ->
            repeat(15) { step ->
                val t = step / 14f
                val x = start.first + (end.first - start.first) * t
                val y = start.second + (end.second - start.second) * t
                batch.color = Color(.08f, .92f, 1f, .72f)
                batch.draw(glow, x - 10f, y - 10f, 20f, 20f)
            }
        }
        batch.color = Color.WHITE
    }

    override fun render(delta: Float) {
        begin("world_4_zerog")
        cards.clear()
        drawRoute()
        LevelRepository.worlds.forEachIndexed { index, world ->
            val row = index / 2
            val col = if (row % 2 == 0) index % 2 else 1 - index % 2
            val x = 55f + col * 405f
            val y = 1060f - row * 320f + scrollOffset
            val firstLevel = LevelRepository.firstLevel(world.id)
            val lastLevel = LevelRepository.lastLevel(world.id)
            val completed = (firstLevel..lastLevel).count { game.progress.stars(it) > 0 }
            val unlocked = game.progress.unlockedLevel >= firstLevel
            val rect = mapCard(world, unlocked, x, y)
            cards += rect to world.id
            if (unlocked && completed > 0) {
                val iconSize = 30f
                val gap = 5f
                val starsWidth = completed * iconSize + (completed - 1) * gap
                val starsX = x + (380f - starsWidth) / 2f
                repeat(completed) { starIcon(starsX + it * (iconSize + gap), y + 65f, iconSize) }
            }
            if (completed == LevelRepository.worldLevelCount(world.id)) {
                worldDoneIcon(x + 326f, y + 222f, 48f)
            }
        }
        batch.color = Color(.005f, .015f, .045f, .96f)
        batch.draw(game.assets.ui.findRegion("panel"), 0f, 1400f, 900f, 200f)
        batch.color = Color.WHITE
        title("WORLD MAP", 1520f)
        fittedText(game.assets.smallFont, "SWIPE UP OR DOWN • TAP ANY WORLD TO PREVIEW", 45f, 1430f, 810f, .72f)
        batch.color = Color(.005f, .015f, .045f, .94f)
        batch.draw(game.assets.ui.findRegion("panel"), 0f, 0f, 900f, 150f)
        batch.color = Color.WHITE
        val mainMenu = button("MAIN MENU", 250f, 35f, 400f, 90f)
        end()
        handleTouch(mainMenu)
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK)) game.openMenu()
    }

    private fun handleTouch(mainMenu: Rectangle) {
        val isTouched = Gdx.input.isTouched
        if (Gdx.input.justTouched()) {
            val point = touchPoint()
            touchActive = point.y < 1400f
            touchMoved = false
            touchStartX = point.x; touchStartY = point.y
            touchLastX = point.x; touchLastY = point.y
        }
        if (touchActive && isTouched) {
            val point = touchPoint()
            val delta = point.y - touchLastY
            scrollOffset = (scrollOffset + delta).coerceIn(0f, maxScroll)
            touchLastX = point.x; touchLastY = point.y
            if (abs(point.y - touchStartY) > 24f || abs(point.x - touchStartX) > 24f) touchMoved = true
        } else if (touchActive) {
            if (!touchMoved) {
                if (mainMenu.contains(touchLastX, touchLastY)) game.openMenu()
                else cards.firstOrNull { it.first.contains(touchLastX, touchLastY) }?.let {
                    game.setScreen(LevelSelectScreen(game, it.second))
                }
            }
            touchActive = false
        }
    }
}

class LevelSelectScreen(game: BrickBreakerGame, private val world: Int) : ForgeScreen(game) {
    override fun render(delta: Float) {
        begin(world, video = true)
        title(LevelRepository.worlds[world - 1].name)
        val worldUnlocked = game.progress.unlockedLevel >= LevelRepository.firstLevel(world)
        fittedText(game.assets.smallFont, if (worldUnlocked) LevelRepository.worlds[world - 1].subtitle else "WORLD PREVIEW • COMPLETE EARLIER WORLDS TO UNLOCK", 45f, 1375f, 810f, .9f)
        val cells = mutableListOf<Pair<Rectangle, Int>>()
        for (stage in 1..LevelRepository.worldLevelCount(world)) {
            val id = LevelRepository.firstLevel(world) + stage - 1
            val col = (stage - 1) % 3
            val row = (stage - 1) / 3
            val x = 75f + col * 260f
            val y = 1190f - row * 190f
            val open = id <= game.progress.unlockedLevel
            val completed = game.progress.stars(id) > 0
            val rect = button(if (open) stage.toString() else "LOCK", x, y, 230f, 125f)
            cells += rect to id
            if (completed) starIcon(x + 158f, y + 39f, 42f)
        }
        val back = button("BACK", 250f, 120f, 400f, 90f)
        end()
        cells.firstOrNull { tapped(it.first) }?.let {
            if (it.second <= game.progress.unlockedLevel) game.play(it.second)
        }
        if (tapped(back)) game.setScreen(WorldMapScreen(game))
    }
}

class ResultsScreen(
    game: BrickBreakerGame,
    private val level: LevelDefinition,
    private val score: Int,
    private val stars: Int,
) : ForgeScreen(game) {
    override fun render(delta: Float) {
        begin(level.world, video = true)
        title("LEVEL COMPLETE")
        val iconSize = 92f
        val gap = 18f
        val starsWidth = stars * iconSize + (stars - 1) * gap
        val starsX = (900f - starsWidth) / 2f
        repeat(stars) { starIcon(starsX + it * (iconSize + gap), 1080f, iconSize) }
        game.assets.bodyFont.draw(batch, "SCORE  $score", 0f, 1020f, 900f, Align.center, false)
        val next = button(if (level.id < LevelRepository.TOTAL_LEVELS) "NEXT LEVEL" else "CAMPAIGN COMPLETE", 125f, 760f)
        val retry = button("REPLAY", 125f, 630f)
        val menu = button("MAIN MENU", 125f, 500f)
        end()
        when {
            tapped(next) -> if (level.id < LevelRepository.TOTAL_LEVELS) game.play(level.id + 1) else game.openMenu()
            tapped(retry) -> game.play(level.id)
            tapped(menu) -> game.openMenu()
        }
    }
}

class CustomTestResultScreen(
    game: BrickBreakerGame,
    private val editor: LevelEditorState,
    private val score: Int,
    private val stars: Int,
) : ForgeScreen(game) {
    override fun render(delta: Float) {
        begin(editor.properties.world); title("CUSTOM TEST COMPLETE")
        game.assets.bodyFont.draw(batch, "SCORE  $score   •   STARS  $stars", 0f, 1120f, 900f, Align.center, false)
        game.assets.smallFont.draw(batch, "Campaign progress and paid Items were not changed.", 70f, 1010f, 760f, Align.center, true)
        val replay = button("REPLAY TEST", 125f, 760f)
        val exit = button("EXIT TEST TO EDITOR", 125f, 620f)
        end()
        when { tapped(replay) -> game.playCustom(editor); tapped(exit) -> game.setScreen(LevelEditorScreen(game, editor)) }
    }
}

class SettingsScreen(game: BrickBreakerGame, private val returnToPausedGame: Boolean = false) : ForgeScreen(game) {
    private fun settingButton(assetName: String, y: Float): Rectangle {
        batch.color = Color.WHITE
        batch.draw(game.assets.settingsMenuTexture(assetName), 0f, y - 50f, 900f, 210f)
        return Rectangle(115f, y, 700f, 120f)
    }

    private fun toggleIcon(enabled: Boolean, y: Float, sound: Boolean = false) {
        batch.color = Color.WHITE
        val texture = if (sound) {
            game.assets.startMenuTexture(if (enabled) "sound-on" else "sound-off")
        } else {
            game.assets.settingsMenuTexture(if (enabled) "check-on" else "check-off")
        }
        val size = if (sound) 92f else 66f
        batch.draw(texture, 680f, y + (120f - size) / 2f, size, size)
    }

    override fun render(delta: Float) {
        begin("world_2_crystal")
        title("SETTINGS")
        val settings = game.progress.settings
        val sound = settingButton("sound", 1120f)
        toggleIcon(settings.masterVolume > 0f, 1120f, sound = true)
        val haptic = settingButton("haptics", 990f)
        toggleIcon(settings.haptics, 990f)
        val motion = settingButton("motion", 860f)
        toggleIcon(settings.reduceMotion, 860f)
        val contrast = settingButton("contrest-ball", 730f)
        toggleIcon(settings.highContrastBall, 730f)
        val color = settingButton("color-blind-palette", 600f)
        toggleIcon(settings.colorBlind, 600f)
        batch.color = Color.WHITE
        batch.draw(game.assets.settingsMenuTexture("game-developer"), 0f, 365f, 900f, 210f)
        batch.draw(game.assets.settingsMenuTexture("github"), 0f, 225f, 900f, 210f)
        val github = Rectangle(245f, 270f, 410f, 125f)
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
            tapped(github) -> Gdx.net.openURI("https://github.com/msr7799")
            tapped(back) -> { game.progress.saveSettings(); if (returnToPausedGame) game.resumePausedGame() else game.openMenu() }
        }
    }
}
