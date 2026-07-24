package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator

class GameAssets {
    val detailedSprites = DetailedSpriteSheet()
    val gameplayAtlas = GameplayAtlas(detailedSprites)
    val cosmetics = CosmeticSpriteRepository()
    val ui = TextureAtlas(Gdx.files.internal("atlases/ui.atlas"))
    val particles = TextureAtlas(Gdx.files.internal("atlases/particles.atlas"))
    val backgrounds = TextureAtlas(Gdx.files.internal("atlases/backgrounds.atlas"))
    // Static fallback backgrounds are served from backgrounds.atlas (world_1_foundry … world_4_zerog).
    val splash = Texture(Gdx.files.internal("ui/splash_keyart.png")).apply {
        setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
    }
    val starIcon = Texture(Gdx.files.internal("ui/star_icon.png")).apply {
        setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
    }
    val worldDoneIcon = Texture(Gdx.files.internal("ui/world_done_icon.png")).apply {
        setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
    }
    private val worldMapTextures: Map<Int, Texture> = (1..13).mapNotNull { world ->
        runCatching {
            Texture(Gdx.files.internal("backgrounds/worlds-map-images/$world.png")).apply {
                setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            }
        }.onFailure { Gdx.app.error("GameAssets", "Unable to load World Map image $world", it) }
            .getOrNull()?.let { world to it }
    }.toMap()
    private val worldMapRegions = worldMapTextures.mapValues { TextureRegion(it.value) }
    val startScreenBackground = linearTexture("backgrounds/start-screen/start-screen-bg.png")
    private val startMenuTextures: Map<String, Texture> = listOf(
        "continue", "start", "world-map", "shop", "level-editor", "paddle&balls", "new-game",
        "setting", "sound-on", "sound-off", "info", "exit",
    ).associateWith { name -> linearTexture("backgrounds/start-screen/$name.png") }
    private val pauseMenuTextures: Map<String, Texture> = listOf(
        "resume", "setting", "customise", "restart-level", "main-menu",
    ).associateWith { name -> linearTexture("backgrounds/start-screen/pused/$name.png") }
    private val settingsMenuTextures: Map<String, Texture> = listOf(
        "sound", "haptics", "motion", "contrest-ball", "color-blind-palette",
        "game-developer", "github", "save&back", "check-on", "check-off",
    ).associateWith { name -> linearTexture("backgrounds/start-screen/setting/$name.png") }
    val titleFont: BitmapFont
    val bodyFont: BitmapFont
    val smallFont: BitmapFont
    val hudLabelFont: BitmapFont
    val hudValueFont: BitmapFont
    val buttonFont: BitmapFont
    val pauseTitleFont: BitmapFont
    private val sounds = mutableMapOf<String, Sound>()
    private val gameplayMusic = Gdx.audio.newMusic(Gdx.files.internal("audio/special-state-bg-loop.mp3")).apply {
        isLooping = true
    }
    private val menuMusic = Gdx.audio.newMusic(Gdx.files.internal("audio/special-state-bg-loop2.mp3")).apply {
        isLooping = true
    }

    init {
        val semiBoldGenerator = FreeTypeFontGenerator(Gdx.files.internal("fonts/Oxanium-SemiBold.ttf"))
        val boldGenerator = FreeTypeFontGenerator(Gdx.files.internal("fonts/Oxanium-Bold.ttf"))
        fun font(generator: FreeTypeFontGenerator, size: Int, color: Color, border: Float) =
            generator.generateFont(FreeTypeFontGenerator.FreeTypeFontParameter().apply {
            this.size = size; this.color = color; borderWidth = border
            borderColor = Color(0f, .006f, .018f, .94f)
            shadowOffsetX = 1; shadowOffsetY = 1; shadowColor = Color(0f, 0f, 0f, .72f)
            genMipMaps = true
            minFilter = Texture.TextureFilter.MipMapLinearLinear; magFilter = Texture.TextureFilter.Linear
            hinting = FreeTypeFontGenerator.Hinting.AutoFull
            kerning = true
        })
        titleFont = font(boldGenerator, 58, Color(.2f, .96f, 1f, 1f), 2f)
        bodyFont = font(semiBoldGenerator, 38, Color.WHITE, 1.8f)
        smallFont = font(semiBoldGenerator, 30, Color.WHITE, 1.4f)
        hudLabelFont = font(semiBoldGenerator, 29, Color.WHITE, 1.4f)
        hudValueFont = font(boldGenerator, 39, Color.WHITE, 1.7f)
        buttonFont = font(boldGenerator, 35, Color.WHITE, 1.8f)
        pauseTitleFont = font(boldGenerator, 68, Color.WHITE, 2f)
        semiBoldGenerator.dispose(); boldGenerator.dispose()
        listOf("wall_hit", "brick_hit", "glass_hit", "explosion", "powerup", "ui_click").forEach {
            sounds[it] = Gdx.audio.newSound(Gdx.files.internal("audio/$it.ogg"))
        }
    }

    fun play(name: String, volume: Float = .55f) { sounds[name]?.play(volume.coerceIn(0f, 1f)) }
    /**
     * Returns a static TextureRegion from the packed backgrounds.atlas that matches the
     * world's visual style (world_1_foundry / world_2_crystal / world_3_magma / world_4_zerog).
     * Used as fallback when ExoPlayer video is not playing (menus, world map cards, world 11).
     * Never loads a missing file — atlas is always present.
     */
    fun worldFallbackBg(world: Int): TextureRegion {
        val regionName = LevelRepository.worlds.getOrNull(world - 1)?.backgroundRegion
            ?: "world_1_foundry"
        return backgrounds.findRegion(regionName)
            ?: backgrounds.findRegion("world_1_foundry")
    }
    fun worldMapImage(world: Int): TextureRegion = worldMapRegions[world] ?: worldFallbackBg(world)
    fun startMenuTexture(name: String): Texture = requireNotNull(startMenuTextures[name]) { "Missing Start Menu texture: $name" }
    fun pauseMenuTexture(name: String): Texture = requireNotNull(pauseMenuTextures[name]) { "Missing Pause Menu texture: $name" }
    fun settingsMenuTexture(name: String): Texture = requireNotNull(settingsMenuTextures[name]) { "Missing Settings Menu texture: $name" }
    fun startMenuMusic(volume: Float) {
        menuMusic.volume = volume.coerceIn(0f, 1f)
        if (!menuMusic.isPlaying && menuMusic.volume > 0f) menuMusic.play()
    }
    fun stopMenuMusic() = menuMusic.stop()
    fun startGameplayMusic(volume: Float) {
        gameplayMusic.volume = volume.coerceIn(0f, 1f)
        if (!gameplayMusic.isPlaying) gameplayMusic.play()
    }
    fun stopGameplayMusic() = gameplayMusic.stop()
    fun dispose() {
        cosmetics.dispose(); detailedSprites.dispose(); ui.dispose(); particles.dispose(); backgrounds.dispose(); splash.dispose()
        starIcon.dispose(); worldDoneIcon.dispose(); worldMapTextures.values.forEach(Texture::dispose)
        startScreenBackground.dispose(); startMenuTextures.values.forEach(Texture::dispose)
        pauseMenuTextures.values.forEach(Texture::dispose); settingsMenuTextures.values.forEach(Texture::dispose)
        titleFont.dispose(); bodyFont.dispose(); smallFont.dispose(); hudLabelFont.dispose(); hudValueFont.dispose()
        buttonFont.dispose(); pauseTitleFont.dispose(); sounds.values.forEach(Sound::dispose); gameplayMusic.dispose(); menuMusic.dispose()
    }

    private fun linearTexture(path: String) = Texture(Gdx.files.internal(path)).apply {
        setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
    }
}
