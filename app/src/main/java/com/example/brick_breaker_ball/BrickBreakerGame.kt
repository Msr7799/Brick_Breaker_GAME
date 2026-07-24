package com.example.brick_breaker_ball

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen

class BrickBreakerGame(val monetization:MonetizationServices=MonetizationServices.fake()) : Game() {
    lateinit var assets: GameAssets
    lateinit var progress: ProgressStore
    lateinit var pausedSession: PausedSessionStore
    lateinit var boosterInventory:BoosterInventoryStore
    lateinit var ledger:TransactionLedger
    lateinit var dailyRewards:DailyRewardStore
    lateinit var pendingRewards:PendingRewardRevealStore
    lateinit var customLevels:CustomLevelRepository
    override fun create() {
        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        assets = GameAssets(); progress = ProgressStore(); pausedSession = PausedSessionStore()
        boosterInventory=BoosterInventoryStore();ledger=TransactionLedger();dailyRewards=DailyRewardStore();pendingRewards=PendingRewardRevealStore()
        customLevels=CustomLevelRepository();setScreen(SplashScreen(this))
    }
    override fun setScreen(next: Screen?) {
        val previous = screen
        if (next is GameScreen) assets.startGameplayMusic(progress.settings.masterVolume * progress.settings.musicVolume)
        else if (::assets.isInitialized) assets.stopGameplayMusic()
        super.setScreen(next)
        // A button may switch screens while the old screen is still inside render().
        // Dispose on the next application tick so its SpriteBatch is never destroyed mid-frame.
        if (previous != null && previous !== next) Gdx.app.postRunnable { previous.dispose() }
    }
    fun openMenu() = setScreen(MainMenuScreen(this))
    fun applyAudioSettings() {
        if (screen is GameScreen && progress.settings.masterVolume > 0f && progress.settings.musicVolume > 0f)
            assets.startGameplayMusic(progress.settings.masterVolume * progress.settings.musicVolume)
        else assets.stopGameplayMusic()
    }
    fun play(level: Int) { progress.markGameStarted(); setScreen(GameScreen(this, LevelRepository.level(level))) }
    fun playCustom(editor: LevelEditorState) = setScreen(GameScreen(this, editor.toLevelDefinition(), customTestEditor = editor))
    fun resumePausedGame() {
        pausedSession.restore()?.let { (level, session) ->
            session.applyCustomization(progress.settings)
            setScreen(GameScreen(this, level, session, startPaused = true))
        } ?: openMenu()
    }
    fun startNewGame() { pausedSession.clear(); play(1) }
    override fun dispose() { super.dispose(); assets.dispose() }
}
