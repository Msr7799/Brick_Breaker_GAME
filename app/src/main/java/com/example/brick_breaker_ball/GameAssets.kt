/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/main/java/com/example/brick_breaker_ball/GameAssets.kt
 * المؤلف: mohamed alromaihi
 * الدوال الموجودة: `gameplayMusicPathForWorld`، `font`، `play`، `worldFallbackBg`، `worldMapImage`، `startMenuTexture`، `pauseMenuTexture`، `settingsMenuTexture`، `startMenuMusic`، `stopMenuMusic`، `startGameplayMusic`، `stopGameplayMusic`، `dispose`، `linearTexture`
 */

package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator

/** يعيد مسار موسيقى الحملة المخصصة للعالم من دون تحميل مورد صوتي. */
internal fun gameplayMusicPathForWorld(world: Int): String = when (world) {
    in 1..3 -> "audio/special-state-bg-loop2.mp3"
    in 4..6 -> "audio/special-state-bg-loop4.mp3"
    in 7..9 -> "audio/special-state-bg-loop5.mp3"
    in 10..11 -> "audio/special-state-bg-loop6.mp3"
    in 12..13 -> "audio/special-state-bg-loop7-final.mp3"
    else -> "audio/special-state-bg-loop2.mp3"
}

class GameAssets {
    val detailedSprites = DetailedSpriteSheet()
    val gameplayAtlas = GameplayAtlas(detailedSprites)
    val cosmetics = CosmeticSpriteRepository()
    val ui = TextureAtlas(Gdx.files.internal("atlases/ui.atlas"))
    val uiRenderer = ForgeUiRenderer()
    val particles = TextureAtlas(Gdx.files.internal("atlases/particles.atlas"))

    // Single static fallback background used instead of backgrounds.atlas.
    val splash = Texture(Gdx.files.internal("ui/splash_keyart.png")).apply {
        setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
    }
    private val splashRegion = TextureRegion(splash)
    val starIcon = Texture(Gdx.files.internal("ui/star_icon.png")).apply {
        setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
    }
    val worldDoneIcon = Texture(Gdx.files.internal("ui/world_done_icon.png")).apply {
        setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
    }
    val lockIcon = linearTexture("backgrounds/start-screen/lock-icon.png")
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
    val forgePlusGamesLogo = linearTexture("backgrounds/start-screen/forge-plus-games.png")
    // Dedicated background for ShopScreen only.
    val shopScreenBackground = linearTexture("ui/splash_keyart1.png")
    private val startMenuTextures: Map<String, Texture> = (
        listOf(
            "continue", "start", "world-map", "shop", "charms-bag", "level-editor", "paddle&balls", "new-game",
            "setting", "sound-on", "sound-off", "info", "exit"
        ) + if (DevelopmentAccess.DEVELOPER_ACCESS && BuildConfig.DEVELOPER_ACCESS_ALLOWED) {
            listOf("code-on", "code-off")
        } else {
            emptyList()
        }
    ).associateWith { name -> linearTexture("backgrounds/start-screen/$name.png") }
    private val pauseMenuTextures: Map<String, Texture> = listOf(
        "resume",
        "setting",
        "customise",
        "charms-bag",
        "shop",
        "restart-level",
        "main-menu"
    ).associateWith { name -> linearTexture("backgrounds/start-screen/pused/$name.png") }
    private val settingsMenuTextures: Map<String, Texture> = listOf(
        "sound", "haptics", "motion", "contrest-ball", "color-blind-palette",
        "game-developer", "github", "save&back", "check-on", "check-off"
    ).associateWith { name -> linearTexture("backgrounds/start-screen/setting/$name.png") }
    val titleFont: BitmapFont
    val bodyFont: BitmapFont
    val smallFont: BitmapFont
    val hudLabelFont: BitmapFont
    val hudValueFont: BitmapFont
    val buttonFont: BitmapFont
    val pauseTitleFont: BitmapFont
    private val sounds = mutableMapOf<String, Sound>()
    private val gameplayMusic = mutableMapOf<String, Music>()
    private var activeGameplayMusic: Music? = null
    private var activeGameplayMusicPath: String? = null
    // loop.mp3 is reserved for menus; campaign worlds use the five tracks mapped above.
    private val menuMusic = Gdx.audio.newMusic(Gdx.files.internal("audio/special-state-bg-loop.mp3")).apply {
        isLooping = true
    }

    init {
        val semiBoldGenerator = FreeTypeFontGenerator(Gdx.files.internal("fonts/Oxanium-SemiBold.ttf"))
        val boldGenerator = FreeTypeFontGenerator(Gdx.files.internal("fonts/Oxanium-Bold.ttf"))

        /** ملاحظة صيانة: الدالة `font` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
        fun font(generator: FreeTypeFontGenerator, size: Int, color: Color, border: Float) = generator.generateFont(
            FreeTypeFontGenerator.FreeTypeFontParameter().apply {
                this.size = size
                this.color = color
                borderWidth = border
                borderColor = Color(0f, .006f, .018f, .94f)
                shadowOffsetX = 1
                shadowOffsetY = 1
                shadowColor = Color(0f, 0f, 0f, .72f)
                genMipMaps = true
                minFilter = Texture.TextureFilter.MipMapLinearLinear
                magFilter = Texture.TextureFilter.Linear
                hinting = FreeTypeFontGenerator.Hinting.AutoFull
                kerning = true
            }
        )
titleFont = font(
    boldGenerator,
    58,
    Color.valueOf("D9DEE5FF"),
    2f
)

bodyFont = font(semiBoldGenerator, 38, Color.WHITE, 1.8f)
smallFont = font(semiBoldGenerator, 30, Color.WHITE, 1.4f)
hudLabelFont = font(semiBoldGenerator, 29, Color.WHITE, 1.4f)
hudValueFont = font(boldGenerator, 39, Color.WHITE, 1.7f)
buttonFont = font(boldGenerator, 35, Color.WHITE, 1.8f)
pauseTitleFont = font(boldGenerator, 68, Color.WHITE, 2f)
        semiBoldGenerator.dispose()
        boldGenerator.dispose()
        listOf("wall_hit", "brick_hit", "glass_hit", "explosion", "powerup", "ui_click").forEach {
            sounds[it] = Gdx.audio.newSound(Gdx.files.internal("audio/$it.ogg"))
        }
    }

    /** ملاحظة صيانة: الدالة `play` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun play(name: String, volume: Float = .55f) {
        sounds[name]?.play(volume.coerceIn(0f, 1f))
    }
    /**
     * Returns the shared splash_keyart.png background as the static fallback for every world.
     * Used when a dedicated world-map image is unavailable or when gameplay video is not playing.
     */

    /** ملاحظة صيانة: الدالة `worldFallbackBg` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun worldFallbackBg(@Suppress("UNUSED_PARAMETER") world: Int): TextureRegion = splashRegion

    /** ملاحظة صيانة: الدالة `worldMapImage` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun worldMapImage(world: Int): TextureRegion = worldMapRegions[world] ?: worldFallbackBg(world)

    /** ملاحظة صيانة: الدالة `startMenuTexture` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun startMenuTexture(name: String): Texture = requireNotNull(startMenuTextures[name]) { "Missing Start Menu texture: $name" }

    /** ملاحظة صيانة: الدالة `pauseMenuTexture` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun pauseMenuTexture(name: String): Texture = requireNotNull(pauseMenuTextures[name]) { "Missing Pause Menu texture: $name" }

    /** ملاحظة صيانة: الدالة `settingsMenuTexture` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun settingsMenuTexture(name: String): Texture = requireNotNull(settingsMenuTextures[name]) { "Missing Settings Menu texture: $name" }

    /** ملاحظة صيانة: الدالة `startMenuMusic` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun startMenuMusic(volume: Float) {
        menuMusic.volume = volume.coerceIn(0f, 1f)
        if (!menuMusic.isPlaying && menuMusic.volume > 0f) menuMusic.play()
    }

    /** ملاحظة صيانة: الدالة `stopMenuMusic` تنظّف الحالة أو الموارد المرتبطة بهذه المسؤولية بأمان؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun stopMenuMusic() = menuMusic.stop()

    /** ملاحظة صيانة: الدالة `startGameplayMusic` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun startGameplayMusic(world: Int, volume: Float) {
        val path = gameplayMusicPathForWorld(world)
        val music = gameplayMusic.getOrPut(path) {
            Gdx.audio.newMusic(Gdx.files.internal(path)).apply { isLooping = true }
        }
        if (activeGameplayMusicPath != path) {
            activeGameplayMusic?.stop()
            activeGameplayMusic = music
            activeGameplayMusicPath = path
        }
        music.volume = volume.coerceIn(0f, 1f)
        if (!music.isPlaying && music.volume > 0f) music.play()
    }

    /** ملاحظة صيانة: الدالة `stopGameplayMusic` تنظّف الحالة أو الموارد المرتبطة بهذه المسؤولية بأمان؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun stopGameplayMusic() = activeGameplayMusic?.stop() ?: Unit

    /** ملاحظة صيانة: الدالة `dispose` تنظّف الحالة أو الموارد المرتبطة بهذه المسؤولية بأمان؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun dispose() {
        cosmetics.dispose()
        detailedSprites.dispose()
        ui.dispose()
        uiRenderer.dispose()
        particles.dispose()
        splash.dispose()
        starIcon.dispose()
        worldDoneIcon.dispose()
        lockIcon.dispose()
        worldMapTextures.values.forEach(Texture::dispose)
        startScreenBackground.dispose()
        forgePlusGamesLogo.dispose()
        shopScreenBackground.dispose()
        startMenuTextures.values.forEach(Texture::dispose)
        pauseMenuTextures.values.forEach(Texture::dispose)
        settingsMenuTextures.values.forEach(Texture::dispose)
        titleFont.dispose()
        bodyFont.dispose()
        smallFont.dispose()
        hudLabelFont.dispose()
        hudValueFont.dispose()
        buttonFont.dispose()
        pauseTitleFont.dispose()
        sounds.values.forEach(Sound::dispose)
        gameplayMusic.values.forEach(Music::dispose)
        menuMusic.dispose()
    }

    /** ملاحظة صيانة: الدالة `linearTexture` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun linearTexture(path: String) = Texture(Gdx.files.internal(path)).apply {
        setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
    }
}
