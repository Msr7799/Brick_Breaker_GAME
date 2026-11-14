/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/assets/GameAssets.kt
 * الوظيفة: تحميل أصول اللعبة وخطوطها وأصواتها وتحريرها.
 * الحسابات: قياس الخطوط ومناطق الصور وحساب أحجام التحميل.
 */

package com.example.brick_breaker_ball.assets

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.audio.Music
import com.badlogic.gdx.audio.Sound
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.Pixmap
import com.badlogic.gdx.graphics.Texture
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.TextureAtlas
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.utils.JsonReader
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator
import com.example.brick_breaker_ball.BuildConfig
import com.example.brick_breaker_ball.campaign.VolleyStageDefinition
import com.example.brick_breaker_ball.campaign.VolleyStageRepository
import com.example.brick_breaker_ball.cosmetics.CosmeticSpriteRepository
import com.example.brick_breaker_ball.gameplay.GameplayMode
import com.example.brick_breaker_ball.gameplay.VolleyOrbCatalog
import com.example.brick_breaker_ball.ui.ForgeUiPalette
import com.example.brick_breaker_ball.ui.ForgeUiRenderer
import com.example.brick_breaker_ball.ui.ShopScreen
import com.example.brick_breaker_ball.ui.VOLLEY_LOADING_BACKGROUND_FILES
import com.example.brick_breaker_ball.ui.VolleyBossDialogueMoment
import java.util.LinkedHashMap
import java.util.concurrent.Executors

/** Deterministic routing for the nine Volley worlds. */
internal fun gameplayMusicPathForWorld(world: Int): String = when (world) {
    1 -> "audio/world_01_forest_theme.ogg"

    2 -> "audio/world_02_venom_theme.ogg"

    3, 4 -> "audio/world_03_04_aqua_desert_theme.ogg"

    5 -> "audio/world_05_blizzard_ambience.ogg"

    // Author request: windstorm INSTEAD of music
    6 -> "audio/world_06_souls_theme.ogg"

    7 -> "audio/world_07_moon_theme.ogg"

    8 -> "audio/world_08_infernal_theme.ogg"

    9 -> "audio/world_09_final_eclipse_theme.ogg"

    else -> MAIN_MUSIC_ASSET_PATH
}

/** No incidental gameplay effects over the eclipse theme in the last world boss stage.
 * The cannon's authored launch sound is played separately on every shot.
 * This does NOT apply to the boss dialogue: its war drums still play normally.
 */
internal fun volleyGameplayEffectsEnabled(world: Int, isBossStage: Boolean): Boolean = !(world == VolleyStageRepository.WORLDS_COUNT && isBossStage)

internal const val MAIN_MUSIC_ASSET_PATH = "audio/menu_intro_theme.ogg"
internal const val LEVEL_UP_ASSET_PATH = "audio/level_up.ogg"
internal const val BOSS_INTRO_SOUND_PATH = "audio/boss_intro_evil_laugh.ogg"
internal const val BOSS_FINAL_DRUMS_PATH = "audio/boss_final_war_drums.ogg"

internal fun worldAtmospherePathForWorld(world: Int): String? = when (world) {
    2 -> "audio/world_02_water_drips.ogg"
    8 -> "audio/world_08_fire_crackling.ogg"
    else -> null
}

/** These play only during active gameplay, randomly and without overlapping. */
internal fun randomWorldSoundPaths(world: Int): List<String> = when (world) {
    6 -> listOf("audio/world_06_wolf_howl.ogg", "audio/world_06_raven_call.ogg", "audio/world_06_ghost_whisper.ogg")
    8 -> listOf("audio/world_08_dragon_breath.ogg", "audio/world_08_dragon_exhale.ogg")
    else -> emptyList()
}

internal fun availableGameplayMusicPath(
    world: Int,
    assetExists: (String) -> Boolean
): String {
    val worldMusicPath = gameplayMusicPathForWorld(world)
    return if (assetExists(worldMusicPath)) worldMusicPath else MAIN_MUSIC_ASSET_PATH
}

// Previous 0.18 gain, combined with two 50% sliders, attenuated the soundtrack to 4.5%.
// Keep a safe overall gain and compensate world-specific differences in authored loudness.
internal const val BACKGROUND_MUSIC_OUTPUT_GAIN = .65f
internal const val SOUND_EFFECT_OUTPUT_GAIN = .55f

/** Audio mix coefficients calibrated from the supplied OGG masters, not per-screen defaults. */
internal fun gameplayMusicMixGain(world: Int): Float = when (world) {
    1 -> 1.7f

    // Forest was exceptionally quiet; its OGG has also been gently boosted.
    2 -> 1.6f

    3, 4 -> 1.1f

    5 -> .9f

    // Snowstorm is atmospheric; no added score.
    6 -> .75f

    7 -> .40f

    8 -> 1.9f

    9 -> .41f

    in 10..13 -> .32f

    else -> 1f
}

/** Keeps background music gentle even when the player's music slider is set to maximum. */
internal fun backgroundMusicVolume(requestedVolume: Float): Float = requestedVolume.coerceIn(0f, 1f) * BACKGROUND_MUSIC_OUTPUT_GAIN

/** Leaves effects readable without allowing authored files to dominate the mix. */
internal fun soundEffectVolume(requestedVolume: Float): Float = requestedVolume.coerceIn(0f, 1f) * SOUND_EFFECT_OUTPUT_GAIN

/** Stable one-to-one boss artwork contract for the supplied Volley worlds. */
internal fun volleyBossAssetPath(world: Int): String {
    require(world in 1..VolleyStageRepository.WORLDS_COUNT) { "Volley boss world must be in 1..${VolleyStageRepository.WORLDS_COUNT}" }
    return listOf(
        "volley-world/01_Mystery_Forest/Boss_Enemy1.webp",
        "volley-world/02_Venom_Wilds/Boss_Enemy2.webp",
        "volley-world/03_Aqua_Vortex/Boss_Enemy3.webp",
        "volley-world/04_Desert_Spirits/Boss_Enemy4.webp",
        "volley-world/05_Ice_Grave/Boss_enemy5.webp",
        "volley-world/06_Tormented_Souls/Boss_Enemy-6.webp",
        "volley-world/07_Moon_light/Boss_Enemy7.webp",
        "volley-world/08_Infernal_Gates/Boss_Enemy-7.webp",
        "volley-world/09_Final_Eclipse/Boss_Enemy9.webp"
    )[world - 1]
}

/** One canonical registry keeps bundled effects and their actual file formats in sync. */
internal val SOUND_ASSET_PATHS = linkedMapOf(
    "wall_hit" to "audio/wall_hit.ogg",
    "brick_hit" to "audio/brick_hit.ogg",
    "glass_hit" to "audio/glass_hit.ogg",
    "metal_hit" to "audio/metal_hit.ogg",
    "explosion" to "audio/explosion.ogg",
    "powerup" to "audio/powerup.ogg",
    "source_orb_found" to "audio/source_orb_found.wav",
    "source_orb_legendary" to "audio/source_orb_legendary.wav",
    "laser_fire" to "audio/laser_fire.ogg",
    "ui_click" to "audio/ui_click.ogg",
    // Identical original button.mp3 and pop-up.mp3 are now ONE decoded OGG sample.
    "button" to "audio/ui_pop.ogg",
    "volley_ball" to "audio/pop_shoot.wav",
    "source_ball_hit" to "audio/thud3.mp3"
)

class GameAssets {
    private val visualUiArtDelegate = lazy { VisualUiArt() }
    val visualUiArt: VisualUiArt get() = visualUiArtDelegate.value
    private val boardsDelegate = lazy { BoardAtlasRepository("ui/boards.json", "ui/boards1.png") }
    private val boards2Delegate = lazy { BoardAtlasRepository("ui/boards2.json", "ui/boards2.png") }
    private val ballCollectionButtonsDelegate = lazy {
        BoardAtlasRepository("ui/ball_collection_buttons.json", "ui/ball_collection_buttons.png")
    }
    private val volleyEditorAtlasDelegate = lazy { BoardAtlasRepository("ui/volley_editor_atlas.json", "ui/volley_editor_atlas.png") }
    private val volleyEditorControlsDelegate = lazy { BoardAtlasRepository("ui/volley_editor_controls.json", "ui/volley_editor_controls.png") }
    val boards: BoardAtlasRepository get() = boardsDelegate.value
    val boards2: BoardAtlasRepository get() = boards2Delegate.value
    val ballCollectionButtons: BoardAtlasRepository get() = ballCollectionButtonsDelegate.value
    val volleyEditorAtlas: BoardAtlasRepository get() = volleyEditorAtlasDelegate.value
    val volleyEditorControls: BoardAtlasRepository get() = volleyEditorControlsDelegate.value

    // تنظيف Volley: الكور تُحمّل من أطلسها؛ لا ننشئ شيت Classic أثناء فتح التطبيق.
    val cosmetics = CosmeticSpriteRepository()
    val uiRenderer = ForgeUiRenderer()

    // A plain tintable pixel for veils and tiny gameplay diagrams. The legacy ui.png
    // panel is blue/gray artwork and must not be stretched over the wooden boards.
    private val solidUiTexture = Pixmap(1, 1, Pixmap.Format.RGBA8888).let { pixmap ->
        try {
            pixmap.setColor(Color.WHITE)
            pixmap.fill()
            Texture(pixmap)
        } finally {
            pixmap.dispose()
        }
    }
    val solidUiRegion = TextureRegion(solidUiTexture)
    // The legacy glow atlas frame has opaque black corners. Build a genuinely transparent
    // radial light so UI/impact effects cannot draw a rectangular shadow over the game.
    private val softGlowTexture = run {
        val side = 64
        val pixmap = Pixmap(side, side, Pixmap.Format.RGBA8888)
        try {
            pixmap.setBlending(Pixmap.Blending.None)
            for (y in 0 until side) for (x in 0 until side) {
                val dx = (x - (side - 1) / 2f) / ((side - 1) / 2f)
                val dy = (y - (side - 1) / 2f) / ((side - 1) / 2f)
                val falloff = (1f - dx * dx - dy * dy).coerceIn(0f, 1f)
                pixmap.setColor(1f, 1f, 1f, falloff * falloff * falloff)
                pixmap.drawPixel(x, y)
            }
            Texture(pixmap).apply {
                setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
            }
        } finally {
            pixmap.dispose()
        }
    }
    val softGlowRegion = TextureRegion(softGlowTexture)
    private val heroGuideTextureDelegate = lazy { linearTexture("ui/hero-guide/forge_dragon_guide.png") }
    val heroGuideRegion: TextureRegion by lazy { TextureRegion(heroGuideTextureDelegate.value) }
    private val orbSageTextureDelegate = lazy { linearTexture("ui/hero-guide/orb_sage_guide.png") }
    val orbSageRegion: TextureRegion by lazy { TextureRegion(orbSageTextureDelegate.value) }
    private val guideHelpTextDelegate = lazy { GuideHelpText(smallFont) }
    val guideHelpText: GuideHelpText get() = guideHelpTextDelegate.value
    val particles = TextureAtlas(Gdx.files.internal("atlases/particles.atlas"))
    val volley = TextureAtlas(Gdx.files.internal("atlases/volley.atlas"))

    /** Shared compact item/economy artwork; one 2048x512 page, no per-icon textures. */
    val arcadeIcons = TextureAtlas(Gdx.files.internal("atlases/assets_sheet_atlas.atlas"))
    val currencyCoinIcon: TextureRegion = requireNotNull(arcadeIcons.findRegion("assets_9cd24f93_023")) // crowned gold coin from the reference
    val currencyGemIcon: TextureRegion = requireNotNull(arcadeIcons.findRegion("approved_blue_gem"))

    /** Use semantic replacements only. Existing gameplay controls retain original icons otherwise. */
    fun atlasUiIcon(name: String): TextureRegion? = when (name) {
        "bag" -> arcadeIcons.findRegion("assets_9cd24f93_013")

        // treasure chest
        "items" -> arcadeIcons.findRegion("assets_9cd24f93_022")
        "orb" -> arcadeIcons.findRegion("assets_9cd24f93_042")

        // inventory chest
        "featured" -> arcadeIcons.findRegion("assets_9cd24f93_021")

        // crown
        "free" -> currencyCoinIcon

        "recall" -> arcadeIcons.findRegion("assets_9cd24f93_061")

        // returning vortex
        "ball" -> arcadeIcons.findRegion("assets_9cd24f93_042")

        // enchanted orb
        "speed" -> arcadeIcons.findRegion("assets_9cd24f93_016")

        // acceleration arrow
        else -> null
    }

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

    // The visible authored electric strip used by Volley.
    // The PNG has its fully transparent top/bottom padding trimmed to conserve GPU memory.
    val arenaFloorWireTexture = linearTexture("hazard/electric_floor_wire.png")
    val arenaFloorWireRegion = TextureRegion(arenaFloorWireTexture)

    // Shared star and lock artwork remains active in stage selection.
    val landscapeWorldMapStarOn = linearTexture("backgrounds/worlds-map-images/star-on.png")
    val landscapeWorldMapStarOff = linearTexture("backgrounds/worlds-map-images/star-off.png")
    val landscapeWorldMapLockIcon = linearTexture("backgrounds/worlds-map-images/worlds-maps-lock-icon.png")

    // Keep both five-card pages alive during world-card transitions, with two spare
    // entries for prefetch/selection changes. This prevents LRU eviction and texture re-uploading
    // while the outgoing and incoming pages are rendered in the same animation frame.
    private val worldMapTextures = object : LinkedHashMap<String, Texture>(12, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Texture>?): Boolean {
            val shouldRemove = size > 12
            if (shouldRemove) eldest?.value?.dispose()
            return shouldRemove
        }
    }
    private val loadingWorldPreviews = mutableSetOf<String>()
    private val loadingWorldBackgrounds = mutableSetOf<String>()
    @Volatile private var previewGeneration = 0
    @Volatile private var assetsDisposed = false
    // Two decoders prevent the nine world thumbnails from competing for all CPU cores.
    private val previewDecodePool = Executors.newFixedThreadPool(2) { task ->
        Thread(task, "BBB-preview-decode").apply { isDaemon = true }
    }
    private val volleyWorldTextures = object : LinkedHashMap<String, Texture>(1, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Texture>?): Boolean {
            val shouldRemove = size > 1
            if (shouldRemove) eldest?.value?.dispose()
            return shouldRemove
        }
    }
    private var orbIconSheet: Texture? = null
    private var orbIconSheetLoading = false
    private val volleyBossTextures = object : LinkedHashMap<Int, Texture>(1, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Texture>?): Boolean {
            val shouldRemove = size > 1
            if (shouldRemove) eldest?.value?.dispose()
            return shouldRemove
        }
    }
    private val volleyLoadingBackgroundTextures = boundedTextureCache(2)
    private val volleyLoaderFrameTextures = boundedTextureCache(3)

    private fun boundedTextureCache(capacity: Int): LinkedHashMap<String, Texture> = object : LinkedHashMap<String, Texture>(capacity, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Texture>?): Boolean {
            val shouldRemove = size > capacity
            if (shouldRemove) eldest?.value?.dispose()
            return shouldRemove
        }
    }
    private val volleyWorldArt = object : LinkedHashMap<Int, VolleyWorldArt>(1, .75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, VolleyWorldArt>?): Boolean {
            val shouldRemove = size > 1
            if (shouldRemove) eldest?.value?.dispose()
            return shouldRemove
        }
    }

    // Heavy screen art is lazy.  It is decoded only when the owning screen is actually opened.
    private var mainMenuPortraitAuthoredTexture: Texture? = null
    val mainMenuPortraitAuthored: Texture
        get() = mainMenuPortraitAuthoredTexture ?: linearTexture("backgrounds/start-screen/main-screen/main-screen-volley.png")
            .also { mainMenuPortraitAuthoredTexture = it }
    private val mainMenuComponentNames: Set<String> = setOf(
        "sound-off-btn-final",
        "new-game-btn-final"
    )
    private val mainMenuComponentTextures = mutableMapOf<String, Texture>()

    private val settingsMenuTextureNames = setOf(
        "sound", "sound-btn", "sound-btn-off", "haptics", "motion", "contrest-ball", "color-blind-palette",
        "game-developer", "github", "save&back", "check-on", "check-off"
    )
    private val settingsMenuTextures = mutableMapOf<String, Texture>()

    // Small game-themed icon pack used by secondary screens and landscape gameplay.
    // Kept lazy so opening the main menu does not decode artwork that belongs elsewhere.
    private val gameIconTextures = mutableMapOf<String, Texture>()
    val titleFont: BitmapFont
    val bodyFont: BitmapFont
    val smallFont: BitmapFont
    val hudLabelFont: BitmapFont
    val hudValueFont: BitmapFont
    val buttonFont: BitmapFont
    val pauseTitleFont: BitmapFont
    val shopFont: BitmapFont
    val accentHeadingFont: BitmapFont
    private val sounds = mutableMapOf<String, Sound>()
    private var activeGameplayMusic: Music? = null
    private var activeGameplayMusicPath: String? = null
    private var levelUpSound: Sound? = null
    private var bossIntroSound: Sound? = null
    private var bossFinalDrums: Music? = null
    private var activeAtmosphereWorld: Int? = null
    private var atmosphereMusic: Music? = null
    private var activeAtmospherePath: String? = null
    private val randomWorldSounds = mutableMapOf<String, Sound>()
    private var secondsToRandomWorldSound = 0f

    // World-specific soundtrack is streamed as Music; short effects are decoded as Sound.
    private val menuMusic = Gdx.audio.newMusic(Gdx.files.internal(MAIN_MUSIC_ASSET_PATH)).apply {
        isLooping = true
    }

    init {
        val gothamBoldGenerator = FreeTypeFontGenerator(Gdx.files.internal("fonts/GOTHAM-BOLD.TTF"))
        val headlineGenerator = FreeTypeFontGenerator(Gdx.files.internal("fonts/TANHEADLINE-Regular.ttf"))
        val insigniaGenerator = FreeTypeFontGenerator(Gdx.files.internal("fonts/Insignia.ttf"))

        /** ملاحظة صيانة: الدالة `font` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
        fun font(
            generator: FreeTypeFontGenerator,
            size: Int,
            color: Color,
            border: Float,
            extraCharacters: String = "•‹›…—–"
        ) = generator.generateFont(
            FreeTypeFontGenerator.FreeTypeFontParameter().apply {
                this.size = size
                this.color = color
                characters = FreeTypeFontGenerator.DEFAULT_CHARS + extraCharacters
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
        ).also { installEnglishDigitGlyphs(it.data) }
        // TAN remains for display titles; Insignia remains for accent headings.
        // Gotham Bold keeps descriptive labels, HUD captions, shop text and controls legible.
        titleFont = font(headlineGenerator, 58, ForgeUiPalette.ivory, 2f, "‹›…—–")
        bodyFont = font(gothamBoldGenerator, 45, Color.WHITE, 1.8f)
        smallFont = font(gothamBoldGenerator, 39, Color.WHITE, 1.4f)
        hudLabelFont = font(gothamBoldGenerator, 37, Color.WHITE, 1.4f)
        hudValueFont = font(gothamBoldGenerator, 49, Color.WHITE, 1.7f)
        buttonFont = font(gothamBoldGenerator, 44, Color.WHITE, 1.8f)
        pauseTitleFont = font(headlineGenerator, 74, Color.WHITE, 2f, "‹›…—–")
        shopFont = font(gothamBoldGenerator, 40, Color.WHITE, .75f)
        accentHeadingFont = font(insigniaGenerator, 40, Color.WHITE, 1.4f)
        gothamBoldGenerator.dispose()
        headlineGenerator.dispose()
        insigniaGenerator.dispose()
        val uniqueDecodedSounds = mutableMapOf<String, Sound>()
        SOUND_ASSET_PATHS.forEach { (name, path) ->
            sounds[name] = uniqueDecodedSounds.getOrPut(path) {
                Gdx.audio.newSound(Gdx.files.internal(path))
            }
        }
    }

    /** ملاحظة صيانة: الدالة `play` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun play(name: String, volume: Float = .55f) {
        sounds[name]?.play(soundEffectVolume(volume))
    }

    /** Plays a short effect with bounded runtime pitch for satisfying combo feedback. */
    fun play(name: String, volume: Float, pitch: Float, pan: Float = 0f) {
        sounds[name]?.play(
            soundEffectVolume(volume),
            pitch.coerceIn(.5f, 2f),
            pan.coerceIn(-1f, 1f)
        )
    }

    /** Level-up is a short UI sample: Sound avoids creating a streaming decoder for 1.4 seconds. */
    fun playLevelUp(volume: Float) {
        val safeVolume = soundEffectVolume(volume)
        if (safeVolume <= 0f) return
        val sound = levelUpSound ?: Gdx.audio.newSound(Gdx.files.internal(LEVEL_UP_ASSET_PATH))
            .also { levelUpSound = it }
        sound.stop()
        sound.play(safeVolume)
    }

    /** Decode/prepare boss audio during the six-second Volley loading screen. */
    fun prepareBossDialogueAudio(moment: VolleyBossDialogueMoment) {
        when (moment) {
            VolleyBossDialogueMoment.WORLD_INTRO -> {
                if (bossIntroSound == null) {
                    bossIntroSound = Gdx.audio.newSound(Gdx.files.internal(BOSS_INTRO_SOUND_PATH))
                }
            }

            VolleyBossDialogueMoment.FINAL_WARNING -> {
                if (bossFinalDrums == null) {
                    bossFinalDrums = Gdx.audio.newMusic(Gdx.files.internal(BOSS_FINAL_DRUMS_PATH)).apply {
                        isLooping = true
                    }
                }
            }
        }
    }

    /** Start only after the dialogue's first rendered frame, not while its textures are loading. */
    fun playBossDialogueAudio(moment: VolleyBossDialogueMoment, volume: Float) {
        stopBossDialogueAudio()
        val safeUserVolume = volume.coerceIn(0f, 1f)
        if (safeUserVolume <= 0f) return
        prepareBossDialogueAudio(moment)
        when (moment) {
            VolleyBossDialogueMoment.WORLD_INTRO -> {
                // Keep the short vocal cue intelligible without raising every collision sound.
                bossIntroSound?.play(safeUserVolume)
            }

            VolleyBossDialogueMoment.FINAL_WARNING -> {
                bossFinalDrums?.let { drums ->
                    drums.volume = soundEffectVolume(safeUserVolume) * .55f
                    drums.play()
                }
            }
        }
        if (BuildConfig.QA_DIAGNOSTICS) Gdx.app.log("GameAudio", "Boss dialogue audio: $moment")
    }

    fun stopBossDialogueAudio() {
        bossIntroSound?.stop()
        bossFinalDrums?.stop()
    }

    /**
     * Returns the shared splash_keyart.png background as the static fallback for every world.
     * Used when a dedicated world-map image is unavailable or when gameplay video is not playing.
     */

    /** ملاحظة صيانة: الدالة `worldFallbackBg` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun worldFallbackBg(@Suppress("UNUSED_PARAMETER") world: Int): TextureRegion = splashRegion

    /** ملاحظة صيانة: الدالة `worldMapImage` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun worldMapImage(world: Int, @Suppress("UNUSED_PARAMETER") mode: GameplayMode = GameplayMode.VOLLEY): TextureRegion {
        if (world !in 1..VolleyStageRepository.worlds.size) return worldFallbackBg(world)
        val artId = world
        val key = "volley:$artId"
        val texture = worldMapTextures[key] ?: run {
            val base = "volley-world/thumbnails/world_${artId.toString().padStart(2, '0')}"
            val webp = "$base.webp"
            val path = if (Gdx.files.internal(webp).exists()) webp else "$base.jpg"
            if (loadingWorldPreviews.add(key)) {
                val generation = previewGeneration
                previewDecodePool.execute {
                    if (assetsDisposed || generation != previewGeneration) return@execute
                    val result = runCatching { previewPixmapFromAsset(path) }
                    Gdx.app.postRunnable {
                        if (generation == previewGeneration) loadingWorldPreviews.remove(key)
                        result.onSuccess { pixmap ->
                            try {
                                if (!assetsDisposed && generation == previewGeneration && key !in worldMapTextures) {
                                    runCatching {
                                        Texture(pixmap).apply {
                                            setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                                        }
                                    }.onSuccess { worldMapTextures[key] = it }
                                        .onFailure { Gdx.app.error("GameAssets", "Unable to upload World Map image $world", it) }
                                }
                            } finally { pixmap.dispose() }
                        }.onFailure { Gdx.app.error("GameAssets", "Unable to load World Map image $world", it) }
                    }
                }
            }
            return worldFallbackBg(world)
        }
        return TextureRegion(texture)
    }

    /** Uses the first authored stage as a world's portrait preview outside gameplay. */
    fun volleyWorldBackground(world: Int): TextureRegion {
        val artId = VolleyAssetCatalog.worldArtId(world)
        val path = VolleyStageRepository.backgroundAsset(artId, 1)
        val texture = volleyWorldTextures[path] ?: run {
            if (loadingWorldBackgrounds.add(path)) {
                val generation = previewGeneration
                previewDecodePool.execute {
                    if (assetsDisposed || generation != previewGeneration) return@execute
                    val result = runCatching { pixmapFromAsset(path) }
                    Gdx.app.postRunnable {
                        if (generation == previewGeneration) loadingWorldBackgrounds.remove(path)
                        result.onSuccess { pixmap ->
                            try {
                                if (!assetsDisposed && generation == previewGeneration && path !in volleyWorldTextures) {
                                    runCatching {
                                        Texture(pixmap).apply {
                                            setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                                        }
                                    }.onSuccess { volleyWorldTextures[path] = it }
                                        .onFailure { Gdx.app.error("GameAssets", "Unable to upload world background $world", it) }
                                }
                            } finally { pixmap.dispose() }
                        }.onFailure { Gdx.app.error("GameAssets", "Unable to load world background $world", it) }
                    }
                }
            }
            return worldFallbackBg(world)
        }
        return TextureRegion(texture)
    }

    /** Dedicated authored portrait artwork for this exact Volley stage. */
    fun volleyStageBackground(stage: VolleyStageDefinition): TextureRegion {
        val path = stage.backgroundAsset
        val texture = volleyWorldTextures[path] ?: run {
            val beginNanos = System.nanoTime()
            linearTexture(path).also {
                volleyWorldTextures[path] = it
                logColdTextureLoad("stage-gameplay:$path", beginNanos)
            }
        }
        return TextureRegion(texture)
    }

    private val orbRegions: Map<String, IntArray> by lazy {
        val root = JsonReader().parse(Gdx.files.internal("volley-orbs/orb-atlas.json"))
        buildMap {
            var node = root.get("regions").child
            while (node != null) {
                put(node.name, intArrayOf(node.getInt("x"), node.getInt("y"), node.getInt("w"), node.getInt("h")))
                node = node.next
            }
        }
    }

    /** RPG scene artwork, packed once into a single texture with rarity-specific frames. */
    fun orbIcon(id: String): TextureRegion {
        val sheet = orbIconSheet ?: run {
            if (!orbIconSheetLoading) {
                orbIconSheetLoading = true
                val generation = previewGeneration
                previewDecodePool.execute {
                    if (assetsDisposed || generation != previewGeneration) return@execute
                    val result = runCatching { pixmapFromAsset("volley-orbs/orb-atlas.png") }
                    Gdx.app.postRunnable {
                        if (generation == previewGeneration) orbIconSheetLoading = false
                        result.onSuccess { pixmap ->
                            try {
                                if (!assetsDisposed && generation == previewGeneration && orbIconSheet == null) {
                                    runCatching {
                                        Texture(pixmap).apply {
                                            setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
                                        }
                                    }.onSuccess { orbIconSheet = it }
                                        .onFailure { Gdx.app.error("GameAssets", "Unable to upload Orb icons", it) }
                                }
                            } finally { pixmap.dispose() }
                        }.onFailure { Gdx.app.error("GameAssets", "Unable to load Orb icons", it) }
                    }
                }
            }
            return splashRegion
        }
        val region = orbRegions[id] ?: return splashRegion
        return TextureRegion(sheet, region[0], region[1], region[2], region[3])
    }

    /** Compatibility for other shop previews; calls now use the source Atlas. */
    fun orbIcon(familyIndex: Int): TextureRegion {
        val family = VolleyOrbCatalog.families().getOrNull(familyIndex) ?: return splashRegion
        val item = VolleyOrbCatalog.atRank(family, 0) ?: VolleyOrbCatalog.all.firstOrNull { it.family == family }
        return orbIcon(item?.id ?: return splashRegion)
    }

    fun orbIconsLoaded(): Boolean = orbIconSheet != null

    /** Lazily keeps only the current world's large transparent boss portrait in GPU memory. */
    fun volleyBossPortrait(world: Int): TextureRegion {
        val texture = volleyBossTextures[world]
            ?: linearTexture(volleyBossAssetPath(world)).also { volleyBossTextures[world] = it }
        return TextureRegion(texture)
    }

    /** Keeps only the active world's sliced brick sheet and dedicated cannon in GPU memory. */
    fun volleyWorldArt(world: Int): VolleyWorldArt = volleyWorldArt.getOrPut(world) { VolleyWorldArt(world) }

    fun volleyLoadingBackground(fileName: String): TextureRegion {
        require(fileName in VOLLEY_LOADING_BACKGROUND_FILES) { "Unknown Volley loading artwork: $fileName" }
        val path = "volley-world/Loading_Screen/$fileName"
        return TextureRegion(volleyLoadingBackgroundTextures.getOrPut(path) { linearTexture(path) })
    }

    fun volleyLoaderFrame(index: Int): TextureRegion {
        val frame = Math.floorMod(index, 12).toString().padStart(2, '0')
        val path = "volley-loading/loader/frame_$frame.png"
        return TextureRegion(volleyLoaderFrameTextures.getOrPut(path) { linearTexture(path) })
    }

    fun volleyRegion(name: String): TextureRegion = requireNotNull(volley.findRegion(name)) { "Missing Volley atlas region: $name" }

    fun mainMenuComponentTexture(name: String): Texture {
        require(name in mainMenuComponentNames) { "Missing Main Menu component texture: $name" }
        return mainMenuComponentTextures.getOrPut(name) { linearTexture("backgrounds/start-screen/main-screen/$name.png") }
    }

    /** Preloads the portrait menu while the splash is still visible. */
    fun warmMainMenuAssets() {
        mainMenuPortraitAuthored
        mainMenuComponentNames.forEach(::mainMenuComponentTexture)
    }

    /** ملاحظة صيانة: الدالة `settingsMenuTexture` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun settingsMenuTexture(name: String): Texture {
        require(name in settingsMenuTextureNames) { "Missing Settings Menu texture: $name" }
        return settingsMenuTextures.getOrPut(name) { linearTexture("backgrounds/start-screen/setting/$name.png") }
    }

    /** Lazily loads one of the compact game-themed icons used outside the authored main menu. */
    fun gameIcon(name: String): Texture {
        val file = Gdx.files.internal("ui/game-icons/$name.png")
        require(file.exists()) { "Missing game icon texture: $name" }
        return gameIconTextures.getOrPut(name) { linearTexture(file.path()) }
    }

    /**
     * Releases screen-specific art before entering gameplay. Core atlases, cosmetics,
     * pause controls and the current world background remain resident. Reopening a screen simply
     * lazy-loads its art again.
     */
    fun trimForGameplay() {
        previewGeneration++
        loadingWorldPreviews.clear()
        loadingWorldBackgrounds.clear()
        orbIconSheetLoading = false
        orbIconSheet?.dispose()
        orbIconSheet = null
        worldMapTextures.values.forEach(Texture::dispose)
        worldMapTextures.clear()
        volleyLoadingBackgroundTextures.values.forEach(Texture::dispose)
        volleyLoadingBackgroundTextures.clear()
        volleyLoaderFrameTextures.values.forEach(Texture::dispose)
        volleyLoaderFrameTextures.clear()
        mainMenuPortraitAuthoredTexture?.dispose()
        mainMenuPortraitAuthoredTexture = null
        mainMenuComponentTextures.values.forEach(Texture::dispose)
        mainMenuComponentTextures.clear()
        settingsMenuTextures.values.forEach(Texture::dispose)
        settingsMenuTextures.clear()
    }

    /** سجل QA يحدد أي صورة سببت توقفًا فعليًا في الإطار (بالمللي ثانية). */
    private fun logColdTextureLoad(label: String, startedAt: Long) {
        val ms = (System.nanoTime() - startedAt) / 1_000_000L
        if (ms >= 24L) Gdx.app.log("BBB-PERF", "$label decode+upload=${ms}ms")
    }

    fun residentWorldCardCount(): Int = worldMapTextures.size

    /** ملاحظة صيانة: الدالة `startMenuMusic` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun startMenuMusic(volume: Float) {
        menuMusic.volume = backgroundMusicVolume(volume)
        if (!menuMusic.isPlaying && menuMusic.volume > 0f) menuMusic.play()
    }

    /** ملاحظة صيانة: الدالة `stopMenuMusic` تنظّف الحالة أو الموارد المرتبطة بهذه المسؤولية بأمان؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun stopMenuMusic() = menuMusic.stop()

    /** Streams only the track required for the active stage; level 5 deliberately plays wind only. */
    fun startGameplayMusic(world: Int, volume: Float) {
        val targetVolume = backgroundMusicVolume(volume)
        if (targetVolume <= 0f) {
            stopGameplayMusic()
            return
        }
        val path = availableGameplayMusicPath(world) { Gdx.files.internal(it).exists() }
        if (activeGameplayMusicPath != path) {
            activeGameplayMusic?.stop()
            activeGameplayMusic?.dispose()
            activeGameplayMusic = Gdx.audio.newMusic(Gdx.files.internal(path)).apply { isLooping = true }
            activeGameplayMusicPath = path
        }
        activeGameplayMusic?.let { music ->
            music.volume = (targetVolume * gameplayMusicMixGain(world)).coerceIn(0f, 1f)
            if (!music.isPlaying) music.play()
        }
        if (BuildConfig.QA_DIAGNOSTICS) {
            Gdx.app.log("GameAudio", "Gameplay loop world=$world asset=$path volume=${activeGameplayMusic?.volume} playing=${activeGameplayMusic?.isPlaying}")
        }
    }

    /** Release the Android MediaPlayer when leaving a stage; never cache all world players. */
    fun stopGameplayMusic() {
        activeGameplayMusic?.stop()
        activeGameplayMusic?.dispose()
        activeGameplayMusic = null
        activeGameplayMusicPath = null
    }

    /** The environment follows SFX volume independently from the soundtrack slider. */
    fun startWorldAtmosphere(world: Int, sfxVolume: Float) {
        if (activeAtmosphereWorld != world) {
            stopWorldAtmosphere()
            activeAtmosphereWorld = world
            secondsToRandomWorldSound = com.badlogic.gdx.math.MathUtils.random(10f, 19f)
        }
        val path = worldAtmospherePathForWorld(world)
        val outputVolume = soundEffectVolume(sfxVolume) * .27f
        if (path == null || outputVolume <= 0f) {
            atmosphereMusic?.stop()
            return
        }
        if (activeAtmospherePath != path) {
            atmosphereMusic?.dispose()
            atmosphereMusic = Gdx.audio.newMusic(Gdx.files.internal(path)).apply { isLooping = true }
            activeAtmospherePath = path
        }
        atmosphereMusic?.let {
            it.volume = outputVolume
            if (!it.isPlaying) it.play()
        }
    }

    /** A bounded, one-shot event; random sounds never accumulate/overlap every frame. */
    fun updateWorldAtmosphere(delta: Float, sfxVolume: Float) {
        val world = activeAtmosphereWorld ?: return
        val paths = randomWorldSoundPaths(world)
        if (paths.isEmpty() || soundEffectVolume(sfxVolume) <= 0f) return
        secondsToRandomWorldSound -= delta.coerceIn(0f, .1f)
        if (secondsToRandomWorldSound > 0f) return
        val chosen = paths[com.badlogic.gdx.math.MathUtils.random(paths.lastIndex)]
        val effect = randomWorldSounds.getOrPut(chosen) { Gdx.audio.newSound(Gdx.files.internal(chosen)) }
        effect.play(soundEffectVolume(sfxVolume) * .55f)
        secondsToRandomWorldSound = if (world == 6) {
            com.badlogic.gdx.math.MathUtils.random(13f, 24f)
        } else {
            com.badlogic.gdx.math.MathUtils.random(16f, 29f)
        }
    }

    fun stopWorldAtmosphere() {
        atmosphereMusic?.stop()
        atmosphereMusic?.dispose()
        atmosphereMusic = null
        activeAtmospherePath = null
        activeAtmosphereWorld = null
        secondsToRandomWorldSound = 0f
        randomWorldSounds.values.forEach {
            it.stop()
            it.dispose()
        }
        randomWorldSounds.clear()
    }

    /** ملاحظة صيانة: الدالة `dispose` تنظّف الحالة أو الموارد المرتبطة بهذه المسؤولية بأمان؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun dispose() {
        assetsDisposed = true
        previewGeneration++
        previewDecodePool.shutdownNow()
        if (boardsDelegate.isInitialized()) boards.dispose()
        if (visualUiArtDelegate.isInitialized()) visualUiArt.dispose()
        if (boards2Delegate.isInitialized()) boards2.dispose()
        if (ballCollectionButtonsDelegate.isInitialized()) ballCollectionButtons.dispose()
        if (volleyEditorAtlasDelegate.isInitialized()) volleyEditorAtlas.dispose()
        if (volleyEditorControlsDelegate.isInitialized()) volleyEditorControls.dispose()
        cosmetics.dispose()
        uiRenderer.dispose()
        particles.dispose()
        softGlowTexture.dispose()
        volley.dispose()
        arcadeIcons.dispose()
        splash.dispose()
        starIcon.dispose()
        worldDoneIcon.dispose()
        lockIcon.dispose()
        arenaFloorWireTexture.dispose()
        landscapeWorldMapStarOn.dispose()
        landscapeWorldMapStarOff.dispose()
        landscapeWorldMapLockIcon.dispose()
        worldMapTextures.values.forEach(Texture::dispose)
        volleyWorldTextures.values.forEach(Texture::dispose)
        orbIconSheet?.dispose()
        volleyBossTextures.values.forEach(Texture::dispose)
        volleyWorldArt.values.forEach(VolleyWorldArt::dispose)
        volleyLoadingBackgroundTextures.values.forEach(Texture::dispose)
        volleyLoaderFrameTextures.values.forEach(Texture::dispose)
        mainMenuPortraitAuthoredTexture?.dispose()
        mainMenuComponentTextures.values.forEach(Texture::dispose)
        settingsMenuTextures.values.forEach(Texture::dispose)
        gameIconTextures.values.forEach(Texture::dispose)
        titleFont.dispose()
        bodyFont.dispose()
        smallFont.dispose()
        hudLabelFont.dispose()
        hudValueFont.dispose()
        buttonFont.dispose()
        pauseTitleFont.dispose()
        shopFont.dispose()
        accentHeadingFont.dispose()
        solidUiTexture.dispose()
        if (heroGuideTextureDelegate.isInitialized()) heroGuideTextureDelegate.value.dispose()
        if (orbSageTextureDelegate.isInitialized()) orbSageTextureDelegate.value.dispose()
        stopWorldAtmosphere()
        stopBossDialogueAudio()
        sounds.values.distinct().forEach(Sound::dispose)
        stopGameplayMusic()
        levelUpSound?.dispose()
        bossIntroSound?.dispose()
        bossFinalDrums?.dispose()
        menuMusic.dispose()
    }

    /** ملاحظة صيانة: الدالة `linearTexture` تنفّذ مسؤولية محلية يعتمد عليها هذا الجزء من اللعبة؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    private fun linearTexture(path: String) = textureFromAsset(path).apply {
        setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear)
        setWrap(Texture.TextureWrap.ClampToEdge, Texture.TextureWrap.ClampToEdge)
    }
}
