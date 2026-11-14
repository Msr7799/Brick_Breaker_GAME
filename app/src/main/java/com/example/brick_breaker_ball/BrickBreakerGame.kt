/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/BrickBreakerGame.kt
 * الوظيفة: تهيئة LibGDX وربط الشاشات والمخازن والخدمات.
 * الحسابات: تزامن دورة الإطار مع الانتقالات؛ لا معادلة لعب مستقلة.
 */

package com.example.brick_breaker_ball

import com.badlogic.gdx.Game
import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.Screen
import com.example.brick_breaker_ball.app.DevelopmentAccess
import com.example.brick_breaker_ball.app.Navigator
import com.example.brick_breaker_ball.app.PlatformActions
import com.example.brick_breaker_ball.app.QaDiagnosticsOverlay
import com.example.brick_breaker_ball.assets.GameAssets
import com.example.brick_breaker_ball.campaign.LevelDefinition
import com.example.brick_breaker_ball.campaign.ProgressStore
import com.example.brick_breaker_ball.campaign.VolleyPausedSessionStore
import com.example.brick_breaker_ball.campaign.VolleyStageRepository
import com.example.brick_breaker_ball.cosmetics.CosmeticOwnershipStore
import com.example.brick_breaker_ball.cosmetics.CosmeticProgressionService
import com.example.brick_breaker_ball.economy.BoosterGrantFactory
import com.example.brick_breaker_ball.economy.BoosterInventoryStore
import com.example.brick_breaker_ball.economy.DailyRewardStore
import com.example.brick_breaker_ball.economy.DailyWalletReward
import com.example.brick_breaker_ball.economy.EconomyStore
import com.example.brick_breaker_ball.economy.MonetizationServices
import com.example.brick_breaker_ball.economy.ShopCatalog
import com.example.brick_breaker_ball.economy.TransactionLedger
import com.example.brick_breaker_ball.gameplay.GameplayMode
import com.example.brick_breaker_ball.gameplay.PowerUpType
import com.example.brick_breaker_ball.gameplay.VolleyEditorDraft
import com.example.brick_breaker_ball.ui.MainMenuScreen
import com.example.brick_breaker_ball.ui.HeroGuideProgress
import com.example.brick_breaker_ball.ui.HeroStoryScreen
import com.example.brick_breaker_ball.ui.DailyRewardsScreen
import com.example.brick_breaker_ball.ui.NavigationVeil
import com.example.brick_breaker_ball.ui.PendingRewardRevealStore
import com.example.brick_breaker_ball.ui.ScreenTransitionStyle
import com.example.brick_breaker_ball.ui.SplashScreen
import com.example.brick_breaker_ball.ui.CloudSaveScreen
import com.example.brick_breaker_ball.ui.AppUpdateScreen
import com.example.brick_breaker_ball.campaign.CloudSaveCodec
import com.example.brick_breaker_ball.campaign.CloudProgressSync
import com.example.brick_breaker_ball.ui.UiFeedback
import com.example.brick_breaker_ball.ui.UiMotionState
import com.example.brick_breaker_ball.ui.VolleyGameScreen
import com.example.brick_breaker_ball.ui.VolleyLevelEditorScreen
import com.example.brick_breaker_ball.ui.VolleyLoadingScreen

class BrickBreakerGame(
    val monetization: MonetizationServices = MonetizationServices.unavailable(),
    val platformActions: PlatformActions = PlatformActions.UNAVAILABLE
) : Game() {
    lateinit var assets: GameAssets
    lateinit var progress: ProgressStore
    lateinit var heroGuideProgress: HeroGuideProgress
    lateinit var volleyPausedSession: VolleyPausedSessionStore
    lateinit var volleyItemInventory: BoosterInventoryStore
    lateinit var ledger: TransactionLedger
    lateinit var volleyDailyRewards: DailyRewardStore
    lateinit var pendingRewards: PendingRewardRevealStore
    lateinit var cosmeticOwnership: CosmeticOwnershipStore
    lateinit var cosmeticProgression: CosmeticProgressionService
    lateinit var developmentAccess: DevelopmentAccess
    lateinit var economy: EconomyStore
    internal lateinit var cloudProgress: CloudProgressSync
    private var cloudGatePassed = false
    private var cloudReloaded = false
    private var cloudForceCheck = false
    private var cloudConflictRoute: CloudSaveScreen? = null
    val cloudAccountReady: Boolean get() = !platformActions.cloudProgressEnabled ||
        (::cloudProgress.isInitialized && cloudProgress.accountReady)
    var pendingDailyWalletReward: DailyWalletReward? = null
    private var pendingCharmLesson: Pair<GameplayMode, PowerUpType>? = null

    fun showCharmLessonAfterBag(mode: GameplayMode, type: PowerUpType) {
        pendingCharmLesson = mode to type
    }

    fun takeCharmLesson(mode: GameplayMode): PowerUpType? {
        val pending = pendingCharmLesson ?: return null
        if (pending.first != mode) return null
        pendingCharmLesson = null
        return pending.second
    }
    private var dailyStatusPending = false
    private var refreshDailyStatusAfterPending = false
    private var nextDailyStatusCheckNano = 0L
    private var pendingDailyRewardDay: Long? = null
    private var pendingDailyRewardAccountId: String? = null
    val uiMotion = UiMotionState()
    private lateinit var navigationVeil: NavigationVeil
    lateinit var uiFeedback: UiFeedback
    lateinit var navigator: Navigator
    lateinit var qaDiagnostics: QaDiagnosticsOverlay

    /** ملاحظة صيانة: الدالة `create` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun create() {
        Gdx.input.setCatchKey(Input.Keys.BACK, true)
        assets = GameAssets()
        CloudSaveCodec.recover()
        progress = ProgressStore()
        heroGuideProgress = HeroGuideProgress(Gdx.app.getPreferences("brickbreakerball-hero-guide-v1"))
        navigationVeil = NavigationVeil()
        uiFeedback = UiFeedback(this)
        navigator = Navigator(this)
        qaDiagnostics = QaDiagnosticsOverlay(this)
        economy = EconomyStore()
        volleyPausedSession = VolleyPausedSessionStore()
        volleyItemInventory = BoosterInventoryStore(
            Gdx.app.getPreferences("brickbreakerball-volley-items-v1")
        )
        ledger = TransactionLedger()
        volleyDailyRewards = DailyRewardStore(
            Gdx.app.getPreferences("brickbreakerball-volley-daily-reward-v1")
        )
        pendingRewards =
            PendingRewardRevealStore()
        cosmeticOwnership = CosmeticOwnershipStore()
        developmentAccess = DevelopmentAccess()
        cosmeticProgression = CosmeticProgressionService(
            assets.cosmetics.balls,
            cosmeticOwnership
        )
        cosmeticProgression.reconcile(progress, developmentAccess.enabled)
        cloudProgress = CloudProgressSync(platformActions, reloaded = {
            reloadPlayerState()
            cloudReloaded = true
        }, ready = {
            reconcilePurchases()
            nextDailyStatusCheckNano = 0L
            syncDailyRewards()
        })
        monetization.rewardedReviveAdGateway.preload()
        monetization.rewardedTalismanAdGateway.preload()
        if (!platformActions.cloudProgressEnabled) reconcilePurchases()
        setScreen(SplashScreen(this))
    }

    /** ملاحظة صيانة: الدالة `setScreen` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun setScreen(next: Screen?) {
        cloudForceCheck = true
        if (next === cloudConflictRoute) cloudConflictRoute = null
        if (::progress.isInitialized) applyGameplayOrientation(progress.settings.gameplayMode)
        val previous = screen
        val gameplayWorld = when (next) {
            is VolleyGameScreen -> next.musicWorld
            else -> null
        }
        if (gameplayWorld != null) {
            assets.trimForGameplay()
        } else if (::assets.isInitialized) {
            assets.stopGameplayMusic()
            assets.stopWorldAtmosphere()
        }
        // Calls the old screen's hide() first (stopping menu music) and the new screen's show().
        // Only then start this world's loop so the menu player cannot overlap or steal audio focus.
        super.setScreen(next)
        if (gameplayWorld != null) {
            assets.stopMenuMusic()
            assets.startGameplayMusic(gameplayWorld, progress.settings.masterVolume * progress.settings.musicVolume)
            assets.startWorldAtmosphere(gameplayWorld, progress.settings.masterVolume * progress.settings.sfxVolume)
        }
        // A button may switch screens while the old screen is still inside render().
        // Dispose on the next application tick so its SpriteBatch is never destroyed mid-frame.
        if (previous != null && previous !== next) Gdx.app.postRunnable { previous.dispose() }
    }

    /** ملاحظة صيانة: الدالة `openMenu` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun navigate(next: Screen, style: ScreenTransitionStyle = ScreenTransitionStyle.FADE_THROUGH) = navigator.navigate(next, style)

    fun applyGameplayOrientation(mode: GameplayMode = progress.settings.gameplayMode) {
        platformActions.setGameplayOrientation(mode)
    }

    fun openMenu() = navigate(MainMenuScreen(this), ScreenTransitionStyle.FADE_THROUGH)

    fun openFirstEntryOrMenu() {
        if (!cloudGatePassed && platformActions.cloudProgressEnabled && !cloudProgress.initialComplete) {
            cloudGatePassed = true
            navigate(CloudSaveScreen(this))
            return
        }
        if (cloudProgress.conflict != null) {
            navigate(CloudSaveScreen(this))
            return
        }
        navigate(
            if (heroGuideProgress.shouldShowStory(progress.hasStartedGame || progress.unlockedLevel > 1, hasPausedGame())) HeroStoryScreen(this)
            else MainMenuScreen(this), ScreenTransitionStyle.FADE_THROUGH
        )
    }

    /** Recreate cached wallet/orb objects only at a safe menu or startup boundary. */
    private fun reloadPlayerState() {
        progress = ProgressStore()
        economy = EconomyStore()
        volleyItemInventory = BoosterInventoryStore(Gdx.app.getPreferences("brickbreakerball-volley-items-v1"))
        ledger = TransactionLedger()
        cosmeticOwnership = CosmeticOwnershipStore()
        heroGuideProgress = HeroGuideProgress(Gdx.app.getPreferences("brickbreakerball-hero-guide-v1"))
        pendingRewards = PendingRewardRevealStore()
        pendingDailyWalletReward = null
        pendingDailyRewardDay = null
        pendingDailyRewardAccountId = null
        cosmeticProgression = CosmeticProgressionService(assets.cosmetics.balls, cosmeticOwnership)
        cosmeticProgression.reconcile(progress, developmentAccess.enabled)
        volleyPausedSession.clear()
    }

    /** ملاحظة صيانة: الدالة `applyAudioSettings` تعالج الحدث أو الطلب وتحدّث الحالة المرتبطة به؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun applyAudioSettings() {
        val gameplayWorld = when (val active = screen) {
            is VolleyGameScreen -> active.musicWorld
            else -> null
        }
        if (gameplayWorld != null) {
            assets.startGameplayMusic(gameplayWorld, progress.settings.masterVolume * progress.settings.musicVolume)
            assets.startWorldAtmosphere(gameplayWorld, progress.settings.masterVolume * progress.settings.sfxVolume)
        } else {
            assets.stopGameplayMusic()
            assets.stopWorldAtmosphere()
        }
    }

    /** ملاحظة صيانة: الدالة `play` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun play(level: Int) {
        progress.markGameStarted()
        progress.markLevelPlayed(level)
        val definition = VolleyStageRepository.levelDefinition(level)
        volleyPausedSession.clear()
        navigate(VolleyLoadingScreen(this, definition), ScreenTransitionStyle.FADE_THROUGH)
    }

    /** Runs an isolated Volley stage and restores the editor draft, including unsaved changes. */
    fun playVolleyEditorTest(draft: VolleyEditorDraft) {
        val snapshot = draft.duplicate()
        val stage = snapshot.toStage()
        val level = LevelDefinition(stage.levelId, stage.world, stage.name)
        navigate(
            VolleyGameScreen(this, level, editorTestDraft = snapshot) {
                navigate(VolleyLevelEditorScreen(this, snapshot.duplicate()), ScreenTransitionStyle.FADE_THROUGH)
            },
            ScreenTransitionStyle.FADE_THROUGH
        )
    }

    /** ملاحظة صيانة: الدالة `playCustom` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */

    /** ملاحظة صيانة: الدالة `resumePausedGame` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun resumePausedGame(showLoading: Boolean = true) {
        volleyPausedSession.restore()?.let { (level, snapshot) ->
            val next = if (showLoading) {
                VolleyLoadingScreen(this, level, snapshot, volleyPausedSession.rewardedRevivesUsed())
            } else {
                VolleyGameScreen(this, level, snapshot, volleyPausedSession.rewardedRevivesUsed())
            }
            navigate(next, ScreenTransitionStyle.FADE_THROUGH)
        } ?: run {
            volleyPausedSession.clear()
            play(progress.nextRecommendedLevel())
        }
    }

    /** ملاحظة صيانة: الدالة `startNewGame` تنفّذ انتقالًا أو تعرض التدفق المطلوب للمستخدم؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    fun startNewGame() {
        volleyPausedSession.clear()
        play(1)
    }

    fun hasPausedGame(): Boolean = volleyPausedSession.hasPausedGame()

    val activeBoosterInventory: BoosterInventoryStore
        get() = itemInventoryFor(progress.settings.gameplayMode)

    fun itemInventoryFor(@Suppress("UNUSED_PARAMETER") mode: GameplayMode): BoosterInventoryStore = volleyItemInventory

    /** Reconcile earned rewards and check whether today's calendar needs to appear. */
    fun onPlayGamesAccountConnected() {
        if (::cloudProgress.isInitialized) cloudProgress.retry()
        nextDailyStatusCheckNano = 0L
        val calendar = screen as? DailyRewardsScreen
        if (calendar != null) {
            calendar.refreshAfterAccountConnected()
        } else if (dailyStatusPending) {
            refreshDailyStatusAfterPending = true
        } else {
            syncDailyRewards()
        }
    }

    fun syncDailyRewards() {
        if (!platformActions.serverDailyRewardsEnabled || dailyStatusPending || !cloudAccountReady) return
        val elapsed = System.nanoTime()
        if (elapsed < nextDailyStatusCheckNano) return
        // A short retry interval also handles delayed Play Games/Firebase authentication.
        nextDailyStatusCheckNano = elapsed + 60_000_000_000L
        dailyStatusPending = true
        val requestedAccountId = platformActions.dailyRewardAccountId
        platformActions.fetchDailyRewardStatus { status, _ ->
            dailyStatusPending = false
            if (status != null && requestedAccountId != null &&
                requestedAccountId == platformActions.dailyRewardAccountId && cloudAccountReady) {
                status.lastReward?.let { economy.creditServerDailyReward(it) }
                pendingDailyRewardDay = if (status.claimedToday) null else status.serverDay
                pendingDailyRewardAccountId = requestedAccountId
            }
            if (refreshDailyStatusAfterPending) {
                refreshDailyStatusAfterPending = false
                nextDailyStatusCheckNano = 0L
                syncDailyRewards()
            }
        }
    }

    /** Use the server's Bahrain day and the Firebase account, never the device clock. */
    fun markDailyRewardPromptSeen(serverDay: Long) {
        val accountId = platformActions.dailyRewardAccountId ?: return
        Gdx.app.getPreferences("brickbreakerball-daily-prompt-v1")
            .putLong("shown_$accountId", serverDay).flush()
        if (pendingDailyRewardAccountId == accountId && pendingDailyRewardDay == serverDay) {
            pendingDailyRewardDay = null
        }
    }

    fun openDailyRewardIfDue() {
        val accountId = platformActions.dailyRewardAccountId ?: return
        val serverDay = pendingDailyRewardDay ?: return
        if (pendingDailyRewardAccountId != accountId || screen !is MainMenuScreen) return
        val preferences = Gdx.app.getPreferences("brickbreakerball-daily-prompt-v1")
        if (preferences.getLong("shown_$accountId", Long.MIN_VALUE) == serverDay) {
            pendingDailyRewardDay = null
            return
        }
        markDailyRewardPromptSeen(serverDay)
        navigate(DailyRewardsScreen(this), ScreenTransitionStyle.FADE_THROUGH)
    }

    fun dailyRewardStoreFor(@Suppress("UNUSED_PARAMETER") mode: GameplayMode): DailyRewardStore = volleyDailyRewards

    /** Restores completed Play purchases (including purchases that were previously pending) exactly once. */
    private fun reconcilePurchases() {
        val requestedAccount = platformActions.dailyRewardAccountId
        monetization.purchaseGateway.reconcile { recovered ->
            if (platformActions.cloudProgressEnabled &&
                (!cloudAccountReady || requestedAccount != platformActions.dailyRewardAccountId)) return@reconcile
            val product = ShopCatalog.findByStoreId(recovered.storeId) ?: return@reconcile
            if (product.ballSpriteName != null) {
                // Rehydrate a permanent Play entitlement even if local preferences were cleared.
                assets.cosmetics.balls.firstOrNull { it.spriteName == product.ballSpriteName }
                    ?.let { cosmeticOwnership.unlockBall(it.id) }
                ledger.markProcessed(recovered.transactionId)
                return@reconcile
            }
            if (ledger.hasProcessed(recovered.transactionId)) return@reconcile
            if (product.gemAmount > 0) {
                // Receipt markers are committed together with Gems in EconomyStore.
                economy.creditPurchasedGems(recovered.transactionId, product.gemAmount)
            } else {
                // Preserve the ORIGINAL reward and mode of a previously sold Item bundle.
                val grant = BoosterGrantFactory.createGrant(product, recovered.transactionId)
                if (grant.isEmpty()) return@reconcile
                itemInventoryFor(ledger.purchaseMode(recovered.storeId) ?: progress.settings.gameplayMode).addAll(grant)
            }
            ledger.markProcessed(recovered.transactionId)
            ledger.clearPurchaseMode(recovered.storeId)
        }
    }

    override fun resume() {
        super.resume()
        if (::cloudProgress.isInitialized) cloudProgress.retry()
        if (::ledger.isInitialized && cloudAccountReady) reconcilePurchases()
        monetization.rewardedReviveAdGateway.preload()
        monetization.rewardedTalismanAdGateway.preload()
    }

    override fun render() {
        if (::assets.isInitialized && ::progress.isInitialized &&
            screen is VolleyGameScreen
        ) {
            assets.updateWorldAtmosphere(Gdx.graphics.deltaTime, progress.settings.masterVolume * progress.settings.sfxVolume)
        }
        super.render()
        if (::navigationVeil.isInitialized && ::progress.isInitialized) {
            if (navigationVeil.render(uiMotion, Gdx.graphics.deltaTime, progress.settings.reduceMotion)) {
                navigator.commitPending()
            }
        }
        if (::qaDiagnostics.isInitialized) qaDiagnostics.render(screen)
        if (::cloudProgress.isInitialized) {
            val safe = !navigator.hasPendingRoute &&
                (screen is SplashScreen || screen is MainMenuScreen || screen is CloudSaveScreen || screen is AppUpdateScreen)
            cloudProgress.tick(safe, cloudForceCheck)
            cloudForceCheck = false
            if (screen is MainMenuScreen && cloudProgress.conflict != null && !navigator.hasPendingRoute) {
                val destination = cloudConflictRoute ?: CloudSaveScreen(this).also { cloudConflictRoute = it }
                navigate(destination)
            }
            else if (screen is MainMenuScreen && cloudReloaded) navigate(MainMenuScreen(this))
            if (safe) cloudReloaded = false
        }
    }

    override fun pause() {
        super.pause()
        if (::cloudProgress.isInitialized) cloudProgress.tick(false, force = true)
    }

    /** ملاحظة صيانة: الدالة `dispose` تنفّذ العقد الموروث وتربط دورة حياة المكوّن بسلوك هذا الملف؛ راجع استدعاءاتها واختباراتها قبل تعديلها. */
    override fun dispose() {
        cloudConflictRoute?.dispose()
        cloudConflictRoute = null
        monetization.dispose()
        if (::qaDiagnostics.isInitialized) qaDiagnostics.dispose()
        uiMotion.dispose()
        if (::navigationVeil.isInitialized) navigationVeil.dispose()
        super.dispose()
        if (::assets.isInitialized) assets.dispose()
    }
}
