/*
 * ملاحظات صيانة الملف:
 * المسار: app/src/main/java/com/example/brick_breaker_ball/BrickBreakerGame.kt
 * المؤلف: mohamed alromaihi
 * الدوال الموجودة: `create`، `setScreen`، `openMenu`، `applyAudioSettings`، `play`، `playCustom`، `resumePausedGame`، `startNewGame`، `dispose`
 */

package com.example.brick_breaker_ball

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen

class BrickBreakerGame(val monetization: MonetizationServices = MonetizationServices.unavailable()) : Game() {
    lateinit var assets: GameAssets
    lateinit var progress: ProgressStore
    lateinit var pausedSession: PausedSessionStore
    lateinit var boosterInventory: BoosterInventoryStore
    lateinit var ledger: TransactionLedger
    lateinit var dailyRewards: DailyRewardStore
    lateinit var pendingRewards: PendingRewardRevealStore
    lateinit var cosmeticOwnership: CosmeticOwnershipStore
    lateinit var cosmeticProgression: CosmeticProgressionService
    lateinit var developmentAccess: DevelopmentAccess
    lateinit var customLevels: CustomLevelRepository

    /** ملاحظة صيانة: الدالة `create` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun create() {
        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        assets = GameAssets()
        progress = ProgressStore()
        pausedSession = PausedSessionStore()
        boosterInventory = BoosterInventoryStore()
        ledger = TransactionLedger()
        dailyRewards = DailyRewardStore()
        pendingRewards =
            PendingRewardRevealStore()
        cosmeticOwnership = CosmeticOwnershipStore()
        developmentAccess = DevelopmentAccess()
        cosmeticProgression = CosmeticProgressionService(
            assets.cosmetics.balls,
            PaddleStyleCatalog.build(assets.cosmetics.paddles),
            cosmeticOwnership
        )
        cosmeticProgression.reconcile(progress, developmentAccess.enabled)
        customLevels = CustomLevelRepository()
        monetization.rewardedReviveAdGateway.preload()
        monetization.rewardedTalismanAdGateway.preload()
        reconcilePurchases()
        setScreen(SplashScreen(this))
    }

    /** ملاحظة صيانة: الدالة `setScreen` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun setScreen(next: Screen?) {
        val previous = screen
        if (next is GameScreen) {
            assets.startGameplayMusic(next.musicWorld, progress.settings.masterVolume * progress.settings.musicVolume)
        } else if (::assets.isInitialized) {
            assets.stopGameplayMusic()
        }
        super.setScreen(next)
        // A button may switch screens while the old screen is still inside render().
        // Dispose on the next application tick so its SpriteBatch is never destroyed mid-frame.
        if (previous != null && previous !== next) Gdx.app.postRunnable { previous.dispose() }
    }

    /** ملاحظة صيانة: الدالة `openMenu` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun openMenu() = setScreen(MainMenuScreen(this))

    /** ملاحظة صيانة: الدالة `applyAudioSettings` تعالج الحدث أو الطلب وتحدّث الحالة المرتبطة به؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun applyAudioSettings() {
        if (screen is GameScreen && progress.settings.masterVolume > 0f && progress.settings.musicVolume > 0f) {
            val gameplayScreen = screen as GameScreen
            assets.startGameplayMusic(gameplayScreen.musicWorld, progress.settings.masterVolume * progress.settings.musicVolume)
        } else {
            assets.stopGameplayMusic()
        }
    }

    /** ملاحظة صيانة: الدالة `play` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun play(level: Int) {
        progress.markGameStarted()
        setScreen(GameScreen(this, LevelRepository.level(level)))
    }

    /** ملاحظة صيانة: الدالة `playCustom` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun playCustom(editor: LevelEditorState) = setScreen(GameScreen(this, editor.toLevelDefinition(), customTestEditor = editor))

    /** ملاحظة صيانة: الدالة `resumePausedGame` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun resumePausedGame() {
        pausedSession.restore()?.let { (level, session) ->
            session.applyCustomization(progress.settings)
            setScreen(GameScreen(this, level, session, startPaused = true))
        } ?: openMenu()
    }

    /** ملاحظة صيانة: الدالة `startNewGame` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun startNewGame() {
        pausedSession.clear()
        play(1)
    }

    /** Restores completed Play purchases (including purchases that were previously pending) exactly once. */
    private fun reconcilePurchases() {
        monetization.purchaseGateway.reconcile { recovered ->
            val product = ShopCatalog.findByStoreId(recovered.storeId) ?: return@reconcile
            if (ledger.hasProcessed(recovered.transactionId)) return@reconcile
            val grant = BoosterGrantFactory.createGrant(product, recovered.transactionId)
            boosterInventory.addAll(grant)
            ledger.markProcessed(recovered.transactionId)
        }
    }

    override fun resume() {
        super.resume()
        if (::ledger.isInitialized) reconcilePurchases()
        monetization.rewardedReviveAdGateway.preload()
        monetization.rewardedTalismanAdGateway.preload()
    }

    /** ملاحظة صيانة: الدالة `dispose` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun dispose() {
        monetization.dispose()
        super.dispose()
        if (::assets.isInitialized) assets.dispose()
    }
}
