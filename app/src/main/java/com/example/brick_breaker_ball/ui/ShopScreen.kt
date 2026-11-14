/*
 * المسار: app/src/main/java/com/example/brick_breaker_ball/ui/ShopScreen.kt
 * الوظيفة: عرض متجر اللعبة والتفاعل مع العروض.
 * الحسابات: تخطيط البطاقات والصفحات والأسعار وحدود اللمس.
 */

package com.example.brick_breaker_ball.ui

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.MathUtils
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack
import com.badlogic.gdx.utils.Align
import com.example.brick_breaker_ball.BrickBreakerGame
import com.example.brick_breaker_ball.BuildConfig
import com.example.brick_breaker_ball.app.DevelopmentAccess
import com.example.brick_breaker_ball.assets.VolleyAssetCatalog
import com.example.brick_breaker_ball.assets.WorldVideoBackgrounds
import com.example.brick_breaker_ball.cosmetics.BallCollectionCatalog
import com.example.brick_breaker_ball.cosmetics.BallSpriteDefinition
import com.example.brick_breaker_ball.cosmetics.VolleyBallSelection
import com.example.brick_breaker_ball.economy.AdState
import com.example.brick_breaker_ball.economy.BoosterGrantFactory
import com.example.brick_breaker_ball.economy.DailyRewardStore
import com.example.brick_breaker_ball.economy.PurchaseGateway
import com.example.brick_breaker_ball.economy.PurchaseResult
import com.example.brick_breaker_ball.economy.RewardedAdGateway
import com.example.brick_breaker_ball.economy.RewardedAdResult
import com.example.brick_breaker_ball.economy.SHOP_ELIGIBLE_TYPES
import com.example.brick_breaker_ball.economy.ShopCatalog
import com.example.brick_breaker_ball.economy.ShopPagination
import com.example.brick_breaker_ball.economy.ShopProduct
import com.example.brick_breaker_ball.economy.StorePurchaseConfirmation
import com.example.brick_breaker_ball.economy.StorePurchaseConfirmationController
import com.example.brick_breaker_ball.economy.shopProductUiState
import com.example.brick_breaker_ball.gameplay.GameplayMode
import com.example.brick_breaker_ball.gameplay.PowerUpType
import com.example.brick_breaker_ball.gameplay.VolleyItemDescriptions
import com.example.brick_breaker_ball.gameplay.VolleyItemInfo
import com.example.brick_breaker_ball.gameplay.VolleyOrbCatalog
import com.example.brick_breaker_ball.gameplay.VolleySourceOrbDrops
import com.example.brick_breaker_ball.economy.OrbShopPricing
import java.util.Locale
import kotlin.math.ceil

private enum class ShopTab(val label: String, val iconName: String) {
    FEATURED("FEATURED", "featured"),
    BALLS("BALLS", "ball"),
    ORBS("ORBS", "orb"),
    FREE("FREE", "free")
}

private enum class ShopCurrency { COINS, GEMS }

enum class ShopReturnDestination { MAIN_MENU, PAUSED_GAME }

/**
 * Production-facing store.
 *
 * Design rules:
 * - one clear visual hierarchy instead of a dense prototype grid;
 * - four clearly described fixed Gem packs in the paid catalog;
 * - collection previews connect purchases to the actual progression loop;
 * - rewarded ads live in their own Free tab and grant only from the SDK reward callback;
 * - checkout uses Google Play's localized price and stays disabled without ProductDetails.
 *
 * ملاحظات صيانة عامة على هذا التحديث:
 * - المتجر يستخدم Midnight Forge palette المركزية وGradients مولّدة مباشرة، بدون Tint فوق Texture ملونة.
 * - أقسام BALLS و ITEMS قابلة للتمرير (سحب باللمس أو بالماوس) وتعرض كل
 *   العناصر الحقيقية بدل أول 4/6/6 عناصر فقط كما كان سابقًا.
 * - تم تكبير حجم البطاقات والأيقونات والخطوط في هذه الأقسام الثلاثة لتحسين وضوحها.
 *
 * ملاحظات صيانة — تمرير وضوح/ازدحام (هذا التحديث):
 * - نظام مسافات ثابت (SPACE_XS..SPACE_XL) ونظام تحجيم خط ثابت (SCALE_SECTION_TITLE..SCALE_CAPTION)
 *   بدل الأرقام السحرية المتفرقة؛ استخدمهما عند إضافة أي قسم جديد بدل رقم جديد عشوائي.
 * - الهيدر أصبح أقصر (شعار SHOP أصغر) والفرق استُخدم لتكبير التبويبات ولإعطاء بطاقة كل تبويب Top
 *   موحّد عبر PANEL_TOP بدل قيم متفرقة لكل تبويب.
 * - شريط الإحصائيات أصبح 3 Chips متساوية بدل سطر واحد مضغوط.
 * - بطاقات ITEMS أعيد بناؤها بقالب ثابت: Icon container 64×64 → Title → Badge OWNED بجانب
 *   العنوان مباشرة → Description بعرض كامل البطاقة وبخط أكبر (كانت مضغوطة يمين الأيقونة فقط).
 * - كل نص وصفي/تعليمات (الأوصاف، شرح HOW IT WORKS، رسائل الحالة) تحوّل لحالة أحرف عادية؛
 *   الأحرف الكبيرة (Uppercase) بقيت فقط للعناوين القصيرة والأزرار والـBadges.
 * - نص Google Test/Debug في تبويب FREE لم يعد يظهر دائمًا؛ يظهر فقط إذا BuildConfig.DEBUG.
 * - تبويب FREE أصبح متمركزًا رأسيًا (بطاقة المكافأة + HOW IT WORKS) بدل مسافة فارغة ضخمة واحدة
 *   قبل زر BACK — الفراغ الزائد يتوزّع تلقائيًا أعلى/أسفل الكتلة.
 * - زرا FULL ARSENAL / FORGE VAULT أسفل ITEMS هما أزرار شراء حقيقية (نفس منتجات FEATURED)، وليسا
 *   مسار Developer Access متاح في Debug وQA عند تشغيله من القائمة الرئيسية؛ يعرض DEV
 *   ويمنح مشتريات تجريبية. إصدار Play Release يعرض أسعار Google Play الفعلية فقط.
 * - زر BACK ثابت الموضع والحجم لكل الأقسام (نداء واحد خارج When في `render`)، لا حاجة لتعديل هنا.
 * - عنصر Floating الرمادي وSafe Area الخاصة به غير موجودين في هذا الملف (على الأرجح في ForgeScreen
 *   الأساس)؛ لم يُعدَّل هنا لعدم توفر ذلك الملف.
 */
class ShopScreen(
    game: BrickBreakerGame,
    private val returnDestination: ShopReturnDestination,
    private val shopMode: GameplayMode = game.progress.settings.gameplayMode,
    private val initialTabName: String = "FEATURED",
    private val initialBallSpriteName: String? = null
) : ForgeScreen(game) {

    companion object {
        // نظام مسافات ثابت (8 / 12 / 16 / 24 / 32) يُستخدم لكل الفجوات والـpadding الجديدة في هذا الملف.
        private const val SPACE_XS = 8f
        private const val SPACE_SM = 12f
        private const val SPACE_MD = 16f
        private const val SPACE_LG = 24f
        private const val SPACE_XL = 32f

        // تسلسل تحجيم واضح: Section title > Item title > Body > Caption. عنوان SHOP العلوي نصّي
        // ويستخدم titleFont بدل Texture مستقلة. استخدم هذه الثوابت بدل أرقام scale متفرقة عند إضافة نص جديد.
        private const val SCALE_SECTION_TITLE = .92f
        private const val SCALE_ITEM_TITLE = .86f
        private const val SCALE_BODY = .76f
        private const val SCALE_CAPTION = .64f

        // أعلى نقطة مشتركة لمناطق التمرير (BALLS / ITEMS)، بدل قيمة top مختلفة
        // لكل تبويب. الهيدر أعلاه أصبح أقصر تحديدًا ليتيح هذه القيمة مساحة كافية دون تمرير الشاشة.
        private const val PANEL_TOP = 1156f
    }

    // تنظيف Volley: حتى الاستدعاء باسم تبويب قديم لا يفتح صفحة البادلز بعد إزالة صورها.
    private val visibleTabs = listOf(ShopTab.FEATURED, ShopTab.BALLS, ShopTab.ORBS, ShopTab.FREE)
    private var activeTab = visibleTabs.firstOrNull { it.name == initialTabName || (initialTabName == "ITEMS" && it == ShopTab.ORBS) } ?: ShopTab.FEATURED
    private var shopGuidePage = if (game.heroGuideProgress.shouldTeachShopOrbs()) {
        if (activeTab == ShopTab.ORBS) 1 else 0
    } else -1
    private val shopGuideAnimation = GuideAnimation()
    private var skipShopGuideAfterExit = false
    private var shopGuideClock = 0f
    private var boardTabSelectionTime = 0f
    private val volleyAssets by lazy { VolleyAssetCatalog(game.assets.volley) }
    private val shopInventory = game.itemInventoryFor(shopMode)
    private val shopDailyRewards = game.dailyRewardStoreFor(shopMode)
    private var message = ""
    private var purchaseBusy = false
    private val purchaseConfirmation = StorePurchaseConfirmationController()
    private var displayedCoins = game.economy.coins.toDouble()
    private var displayedGems = game.economy.gems.toDouble()
    private val walletPeek = WalletBalancePeek()
    private var lastCoins = game.economy.coins
    private var lastGems = game.economy.gems
    private var walletPulse = 0f
    private val rewardFx = RewardFxSystem(game)
    private var tabTransitionTime = 0f
    private var tabTransitionDirection = 1f
    private val pageByTab = mutableMapOf<ShopTab, Int>().apply {
        initialBallSpriteName?.let { sprite ->
            val index = VolleyBallSelection.inDisplayOrder(game.assets.cosmetics.balls, shopMode)
                .indexOfFirst { it.spriteName == sprite }
            if (index >= 0) put(ShopTab.BALLS, index / 4)
        }
    }
    private var boardPageTransitionTime = 0f
    private var boardPageTransitionDirection = 1f
    private var previousArrowPulse = 0f
    private var nextArrowPulse = 0f
    private var adDialogOpen = false
    private var adPoolOpen = false
    private var adRequestPending = false
    private var confirmationPressedButton = 0
    private var adPressedButton = 0
    private var pointerArmed = false
    private var pointerStartX = 0f
    private var pointerStartY = 0f
    private var pointerLastX = 0f
    private var pointerLastY = 0f
    private var pointerStartedInBoard = false

    // Unified Midnight Forge palette. Accent colors are rendered as generated gradients,
    // never multiplied into the pre-colored UI atlas textures.
    private val muted = ForgeUiPalette.muted
    private val primary = ForgeUiPalette.primary
    private val primaryLight = ForgeUiPalette.primaryLight
    private val crimson = ForgeUiPalette.crimson
    private val success = ForgeUiPalette.success
    private val danger = ForgeUiPalette.danger

    // حالة التمرير الخاصة بكل تبويب قابل للتمرير (BALLS / ITEMS)، محفوظة بشكل مستقل
    // بحيث لا يفقد كل قسم موضع تمريره عند التنقل بين التبويبات.
    private val scrollOffsets = mutableMapOf<ShopTab, Float>()
    private var scrollDragActive = false
    private var lastTouchWorldY = 0f
    private val touchWorld = Vector2()
    private var scissorPushed = false
    private var freePreviewIndex = 0



    private fun productTitle(product: ShopProduct): String = product.title
    private fun productSubtitle(product: ShopProduct): String = product.subtitle

    private fun shopContentWidth() = if (isLandscape) responsiveContentWidth() else 790f
    private fun shopContentLeft() = if (isLandscape) responsiveContentLeft() else 55f
    private fun shopPanel(bottom: Float = 325f) = Rectangle(
        shopContentLeft(),
        bottom,
        shopContentWidth(),
        PANEL_TOP - bottom
    )

    /**
     * ملاحظة صيانة: الدالة `show` تُستدعى عند فتح شاشة المتجر. تتحقق أولاً من وجود مكافأة معلّقة لم
     * تُعرض بعد (مثلاً بعد رجوع اللاعب من الخلفية أثناء عرض إعلان) قبل تحميل إعلان المكافأة التالي.
     */
    override fun show() {
        installMouseWheelHandler(::handleMouseWheel)
        game.pendingRewards.load()?.let {
            game.navigate(RewardRevealScreen(game, it, returnDestination))
            return
        }
        game.monetization.purchaseGateway.refreshProducts()
        game.monetization.rewardedTalismanAdGateway.preload()
    }

    override fun hide() {
        removeMouseWheelHandler()
    }

    /**
     * ملاحظة صيانة: الدالة `render` هي حلقة الرسم الرئيسية لشاشة المتجر: ترسم الخلفية والهيدر
     * والتبويبات ثم محتوى التبويب النشط، وتجمع كل المناطق القابلة للمس في `actions` لتُفحص مرة واحدة
     * فقط في نهاية الإطار. لا تُضِف أي رسم بعد استدعاء `end()` في آخر الدالة.
     */
    override fun render(delta: Float) {
        shopGuideClock += delta.coerceIn(0f, .05f)
        walletPeek.update(delta)
        boardTabSelectionTime = (boardTabSelectionTime - delta.coerceIn(0f, .05f)).coerceAtLeast(0f)
        if (shopGuidePage >= 0) {
            if (!game.uiMotion.active) shopGuideAnimation.update(delta, game.progress.settings.reduceMotion)
            if (shopGuideAnimation.exitFinished && finishShopGuideStep()) return
        }
        if (lastCoins != game.economy.coins || lastGems != game.economy.gems) {
            lastCoins = game.economy.coins
            lastGems = game.economy.gems
            walletPulse = 1f
        }
        val walletSmoothing = (1f - kotlin.math.exp((-delta.coerceAtMost(.05f) * 8.5f).toDouble()).toFloat()).coerceIn(0f, 1f)
        displayedCoins += (game.economy.coins - displayedCoins) * walletSmoothing
        displayedGems += (game.economy.gems - displayedGems) * walletSmoothing
        if (kotlin.math.abs(game.economy.coins - displayedCoins) < .5) displayedCoins = game.economy.coins.toDouble()
        if (kotlin.math.abs(game.economy.gems - displayedGems) < .5) displayedGems = game.economy.gems.toDouble()
        walletPulse = (walletPulse - delta * 2.3f).coerceAtLeast(0f)
        rewardFx.update(delta)
        tabTransitionTime = (tabTransitionTime - delta.coerceAtMost(.05f)).coerceAtLeast(0f)
        boardPageTransitionTime = (boardPageTransitionTime - delta.coerceAtMost(.05f)).coerceAtLeast(0f)
        previousArrowPulse = (previousArrowPulse - delta * 4f).coerceAtLeast(0f)
        nextArrowPulse = (nextArrowPulse - delta * 4f).coerceAtLeast(0f)
        beginStore()
        val actions = mutableListOf<Pair<Rectangle, () -> Unit>>()
        val board = drawBoardShop(actions)
        drawBoardWallet(actions, board)
        if (message.isNotBlank()) drawBoardMessage(board)
        rewardFx.draw(batch)
        walletPeek.draw(batch, game.assets,
            Rectangle(board.x + board.width * .07f, board.y + board.height - 92f, board.width * .86f, 82f),
            game.economy.coins, game.economy.gems, game.progress.settings.reduceMotion)
        if (shopGuidePage >= 0 && purchaseConfirmation.pending == null && !adDialogOpen) {
            val screen = Rectangle(visibleLeft, visibleBottom, visibleWidth, visibleHeight)
            HeroGuideRenderer.drawDialogue(batch, game.assets,
                screen, shopGuideLines(),
                visibleBottom + visibleHeight * (if (shopGuidePage == 1) .10f else .40f),
                progress = when (shopGuidePage) {
                    0 -> "TAP THE HIGHLIGHTED ORBS TAB"
                    1 -> "TAP TO CONTINUE  |  2/3"
                    else -> "TAP THE HIGHLIGHTED BAG BUTTON"
                },
                speaker = GuideSpeaker.ORB_SAGE, animation = shopGuideAnimation,
                focus = shopGuideTarget(board), reduceMotion = game.progress.settings.reduceMotion)
            HeroGuideRenderer.drawSkip(batch, game.assets, screen)
        }
        val confirmationButtons = drawPurchaseConfirmation()
        val adButtons = if (adDialogOpen) drawRewardedAdDialog() else null
        end()
        // Upload the Orb icons only after the shop has drawn a complete first frame.
        if (activeTab == ShopTab.ORBS && !game.assets.orbIconsLoaded()) game.assets.orbIcon(0)

        if (purchaseConfirmation.pending != null) {
            pointerArmed = false
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                confirmationPressedButton = 0
                purchaseConfirmation.cancel()
                game.uiFeedback.emit(UiFeedbackKind.BACK)
            } else if (confirmationButtons != null) {
                val (no, yes) = confirmationButtons
                if (Gdx.input.justTouched()) {
                    val p = touchPoint()
                    confirmationPressedButton = when {
                        no.contains(p.x, p.y) -> 1
                        yes.contains(p.x, p.y) -> 2
                        else -> 0
                    }
                } else if (!Gdx.input.isTouched && confirmationPressedButton != 0) {
                    val p = touchPoint()
                    val pressed = confirmationPressedButton
                    confirmationPressedButton = 0
                    if (pressed == 1 && no.contains(p.x, p.y)) {
                        purchaseConfirmation.cancel()
                    } else if (pressed == 2 && yes.contains(p.x, p.y)) {
                        purchaseConfirmation.confirm()
                    }
                }
            }
            return
        }
        confirmationPressedButton = 0
        if (adDialogOpen) {
            pointerArmed = false
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
                adPressedButton = 0
                if (!adRequestPending) {
                    if (adPoolOpen) adPoolOpen = false else adDialogOpen = false
                }
            } else if (adButtons != null) {
                if (Gdx.input.justTouched()) {
                    val p = touchPoint()
                    adPressedButton = when {
                        adButtons.first.contains(p.x, p.y) -> 1
                        !adPoolOpen && adButtons.second.contains(p.x, p.y) -> 2
                        !adPoolOpen && adButtons.third.contains(p.x, p.y) -> 3
                        else -> 0
                    }
                } else if (!Gdx.input.isTouched && adPressedButton != 0) {
                    val p = touchPoint()
                    val pressed = adPressedButton
                    adPressedButton = 0
                    if (!adRequestPending) {
                        if (pressed == 1 && adButtons.first.contains(p.x, p.y)) {
                            if (adPoolOpen) adPoolOpen = false else adDialogOpen = false
                        } else if (pressed == 2 && adButtons.second.contains(p.x, p.y)) {
                            handleRewardTap()
                        } else if (pressed == 3 && adButtons.third.contains(p.x, p.y)) {
                            adPoolOpen = true
                        }
                    }
                }
            }
            return
        }
        adPressedButton = 0
        if (shopGuidePage >= 0) {
            pointerArmed = false
            if (game.uiMotion.active || shopGuideAnimation.exiting) return
            val point = touchPoint()
            val screen = Rectangle(visibleLeft, visibleBottom, visibleWidth, visibleHeight)
            if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE) ||
                (Gdx.input.justTouched() && HeroGuideRenderer.skipBounds(screen).contains(point.x, point.y))) {
                skipShopGuideAfterExit = true
                shopGuideAnimation.beginExit()
            } else if (Gdx.input.justTouched() && shopGuideAnimation.revealOrContinue(shopGuideLines()) &&
                (shopGuidePage == 1 || shopGuideTarget(board).contains(point.x, point.y))) {
                shopGuideAnimation.beginExit()
            }
            return
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            goBack()
            return
        }
        handleBoardPointer(board, actions)
    }

    private fun startShopGuide() {
        shopGuidePage = if (activeTab == ShopTab.ORBS) 1 else 0
        skipShopGuideAfterExit = false
        shopGuideAnimation.reset()
    }

    private fun shopGuideLines(): List<String> = when (shopGuidePage) {
        0 -> listOf("ORBS ADD POWER WHEN EQUIPPED.", "OPEN THIS TAB TO SEE ORB OFFERS.")
        1 -> listOf("HERE YOU BUY OR UPGRADE ORBS.", "YOU CAN ALSO EARN ORBS IN STAGES.")
        else -> listOf("OWNED ORBS LIVE IN YOUR BAG.", "OPEN BAG; I'LL SHOW YOU THE SLOTS.")
    }

    private fun shopGuideTarget(board: Rectangle): Rectangle = when (shopGuidePage) {
        0 -> {
            val gap = board.width * .008f
            val width = (board.width * .89f - gap * (visibleTabs.size - 1)) / visibleTabs.size
            Rectangle(board.x + board.width * .055f + visibleTabs.indexOf(ShopTab.ORBS) * (width + gap),
                board.y + board.height * .699f, width, board.height * .041f)
        }
        1 -> game.assets.boards.placement("shop_board", "shop_top_left_slot", board)
        else -> game.assets.boards.placement("shop_board", "shop_open_bag_button", board)
    }

    /** Returns true only when navigation replaces this screen. */
    private fun finishShopGuideStep(): Boolean {
        if (skipShopGuideAfterExit) {
            shopGuidePage = -1
            shopGuideClock = 0f
            game.heroGuideProgress.completeShopOrbs()
        } else when (shopGuidePage) {
            0 -> { selectBoardTab(ShopTab.ORBS); shopGuidePage = 1 }
            1 -> shopGuidePage = 2
            else -> {
                shopGuidePage = -1
                game.navigate(VolleyOrbsScreen(game, returnDestination, shopMode,
                    returnToShop = true, guideFromShop = true))
                return true
            }
        }
        shopGuideAnimation.reset()
        return false
    }

    private data class BoardShopAction(val label: String, val enabled: Boolean = true, val run: () -> Unit)
    private data class BoardShopOffer(
        val title: String,
        val status: String,
        val image: () -> TextureRegion?,
        val first: BoardShopAction?,
        val second: BoardShopAction? = null,
        val preview: (() -> Unit)? = null
    )

    private val boardOfferTextLayout = GlyphLayout()

    /** نصوص بطاقات اللوحة يجب أن تبقى داخل عرضها الحقيقي حتى مع أسماء الكرات والأسعار الطويلة. */
    private fun drawBoardOfferText(font: BitmapFont, text: String, x: Float, y: Float, width: Float, scale: Float) {
        val oldX = font.data.scaleX
        val oldY = font.data.scaleY
        val desired = scale * uiTextScale
        font.data.setScale(desired)
        boardOfferTextLayout.setText(font, text)
        if (boardOfferTextLayout.width > width && boardOfferTextLayout.width > 0f) {
            font.data.setScale(desired * width / boardOfferTextLayout.width * .97f)
        }
        font.draw(batch, text, x, y, width, Align.center, false)
        font.data.setScale(oldX, oldY)
    }

    private fun boardOffers(): List<BoardShopOffer> = when (activeTab) {
        ShopTab.FEATURED -> ShopCatalog.products.map { product ->
            val price = displayedPrice(product)
            BoardShopOffer(
                product.title,
                product.subtitle,
                { game.assets.currencyGemIcon },
                BoardShopAction(price?.let { "BUY $it" } ?: "UNAVAILABLE", price != null && !purchaseBusy) {
                    if (price != null) {
                        askPurchaseConfirmation(
                            product.title,
                            price,
                            "Receive ${product.gemAmount} Gems after payment is confirmed."
                        ) { purchase(product) }
                    }
                }
            )
        }

        ShopTab.BALLS -> VolleyBallSelection.inDisplayOrder(game.assets.cosmetics.balls, shopMode).mapIndexed { index, ball ->
            val owned = game.developmentAccess.canUseCosmetic(
                VolleyBallSelection.canUse(shopMode, ball, game.cosmeticProgression.ownsBall(ball))
            )
            val offer = BallCollectionCatalog.offer(ball)
            val epicProduct = ShopCatalog.epicProductFor(ball.spriteName)
            val epicPrice = epicProduct?.let(game.monetization.purchaseGateway::price)
            BoardShopOffer(
                ball.name,
                if (owned) {
                    "OWNED"
                } else {
                    "${offer.rarity.name} • ${game.cosmeticProgression.ballRequirement(ball)}"
                },
                { game.assets.cosmetics.ballRegion(ball) },
                if (owned) {
                    BoardShopAction("OWNED") { openCustomization(ball) }
                } else {
                    when {
                        offer.coins != null -> BoardShopAction("${offer.coins} COINS") {
                            askPurchaseConfirmation(ball.name, "${offer.coins} COINS", "Permanently unlock this ball.") {
                                unlockBallWithCoins(ball, offer.coins)
                            }
                        }

                        offer.gems != null -> BoardShopAction("${offer.gems} GEMS") {
                            askPurchaseConfirmation(ball.name, "${offer.gems} GEMS", "Permanently unlock this ball.") {
                                unlockBallWithGems(ball, offer.gems)
                            }
                        }

                        else -> BoardShopAction("VIEW") { openBallDetails(ball) }
                    }
                },
                if (owned || epicProduct == null) {
                    null
                } else {
                    BoardShopAction(epicPrice?.let { "BUY $it" } ?: "UNAVAILABLE", epicPrice != null) {
                        if (epicPrice != null) {
                            askPurchaseConfirmation(
                                ball.name,
                                epicPrice,
                                "Permanent Epic ball. Google Play confirms the price."
                            ) {
                                purchase(epicProduct)
                            }
                        }
                    }
                },
                preview = { openBallDetails(ball) }
            )
        }

        ShopTab.ORBS -> VolleyOrbCatalog.families().mapIndexedNotNull { index, family ->
            if (!VolleyOrbCatalog.isPlayableFamily(family)) return@mapIndexedNotNull null
            val currentRank = game.progress.orbRank(family)
            val next = VolleyOrbCatalog.nextRank(family, currentRank)
            val coinPrice = next?.let { OrbShopPricing.coins(it.rank) }
            val gemPrice = next?.let { OrbShopPricing.gems(it.rank) }
            BoardShopOffer(
                family.replace('_', ' ').uppercase(),
                if (next == null) "MAX RANK" else "RANK $currentRank  ->  ${next.rank}  |  ORB COPY",
                { if (game.assets.orbIconsLoaded()) game.assets.orbIcon(
                    (next ?: VolleyOrbCatalog.atRank(family, currentRank))?.id ?: "CRIT0") else null },
                coinPrice?.let { price -> BoardShopAction("$price COINS") {
                    askPurchaseConfirmation(family, "$price COINS", "Upgrade this Orb to rank ${next!!.rank}.") {
                        purchaseOrbRank(family, false, price)
                    }
                } } ?: BoardShopAction("MAX", false) {},
                gemPrice?.let { price -> BoardShopAction("$price GEMS") {
                    askPurchaseConfirmation(family, "$price GEMS", "Upgrade this Orb to rank ${next!!.rank}.") {
                        purchaseOrbRank(family, true, price)
                    }
                } }
            )
        }

        ShopTab.FREE -> listOf(
            BoardShopOffer(
                if (game.progress.orbStashCount < 150) "ORB + UPGRADE POINT" else "UPGRADE POINT (BAG FULL)",
                "${shopDailyRewards.currentState(System.currentTimeMillis()).claimedToday}/" +
                    "${shopDailyRewards.currentState(System.currentTimeMillis()).dailyLimit} TODAY",
                { if (game.assets.orbIconsLoaded()) game.assets.orbIcon("CRIT0") else game.assets.atlasUiIcon("orb") },
                BoardShopAction("GET FREE") { adDialogOpen = true }
            ),
            BoardShopOffer(
                "DAILY REWARD",
                "14-DAY CALENDAR",
                { game.assets.atlasUiIcon("featured") },
                BoardShopAction("OPEN") {
                    game.navigate(DailyRewardsScreen(game, returnDestination, shopMode), ScreenTransitionStyle.FADE_THROUGH)
                }
            )
        )
    }

    private fun drawBoardShop(actions: MutableList<Pair<Rectangle, () -> Unit>>): Rectangle {
        val insets = safeInsets
        val area = Rectangle(
            visibleLeft + insets.left + 24f,
            visibleBottom + insets.bottom + 18f,
            visibleWidth - insets.left - insets.right - 48f,
            visibleHeight - insets.top - insets.bottom - 200f
        )
        val board = game.assets.boards.contain("shop_board", area)
        batch.color = Color.WHITE
        batch.draw(game.assets.boards.requireRegion("shop_board"), board.x, board.y, board.width, board.height)
        val guideBounds = Rectangle(board.x - 18f, board.y + board.height * .765f,
            board.width * .16f, board.width * .16f)
        val guideVisual = Rectangle(guideBounds).also {
            if (!game.progress.settings.reduceMotion) it.y += MathUtils.sin(shopGuideClock * 2.4f) * 3f
        }
        if (shopGuidePage < 0) HeroGuideRenderer.drawCompanion(batch, game.assets, guideVisual,
            GuideSpeaker.ORB_SAGE, if (game.progress.settings.reduceMotion) 1f else smootherStep(shopGuideClock / .4f))
        actions += guideBounds to { startShopGuide() }

        val tabGap = board.width * .008f
        val tabsLeft = board.x + board.width * .055f
        val tabsWidth = board.width * .89f
        val tabWidth = (tabsWidth - tabGap * (visibleTabs.size - 1)) / visibleTabs.size
        val tabY = board.y + board.height * .699f
        val tabH = board.height * .041f
        visibleTabs.forEachIndexed { index, tab ->
            val rect = Rectangle(tabsLeft + index * (tabWidth + tabGap), tabY, tabWidth, tabH)
            val sprite = if (tab == activeTab) "ui_button_green_blank" else "ui_button_dark_blank"
            val pop = if (tab == activeTab && !game.progress.settings.reduceMotion)
                MathUtils.sin((1f - boardTabSelectionTime / .32f).coerceIn(0f, 1f) * MathUtils.PI) * .045f else 0f
            batch.draw(game.assets.boards2.requireRegion(sprite), rect.x - rect.width * pop * .5f,
                rect.y - rect.height * pop * .5f, rect.width * (1f + pop), rect.height * (1f + pop))
            game.assets.shopFont.color = Color.WHITE
            fittedText(
                game.assets.shopFont,
                tab.label,
                rect.x + 6f,
                rect.y + rect.height * .69f,
                rect.width - 12f,
                if (tabWidth < 125f) .49f else .62f
            )
            actions += rect to { selectBoardTab(tab) }
        }
        game.assets.shopFont.color = Color.WHITE

        val offers = boardOffers()
        val pageCount = ShopPagination.pageCount(offers.size)
        val page = (pageByTab[activeTab] ?: 0).coerceIn(0, pageCount - 1)
        pageByTab[activeTab] = page
        val slotNames = listOf("shop_top_left_slot", "shop_top_right_slot", "shop_bottom_left_slot", "shop_bottom_right_slot")
        val slide = if (game.progress.settings.reduceMotion) {
            0f
        } else {
            smootherStep(boardPageTransitionTime / .24f) * board.width * .02f * boardPageTransitionDirection
        }
        slotNames.forEachIndexed { index, name ->
            val offer = offers.getOrNull(page * 4 + index) ?: return@forEachIndexed
            val slot = game.assets.boards.placement("shop_board", name, board)
            drawBoardOffer(offer, Rectangle(slot.x + slide, slot.y, slot.width, slot.height), actions)
        }
        drawBoardPagination(board, page, pageCount, actions)
        val back = game.assets.boards.placement("shop_board", "shop_back_button", board)
        val bag = game.assets.boards.placement("shop_board", "shop_open_bag_button", board)
        actions += back to { goBack() }
        actions += bag to { game.navigate(VolleyOrbsScreen(game, returnDestination, shopMode, returnToShop = true)) }
        return board
    }

    private fun drawBoardOffer(
        offer: BoardShopOffer,
        slot: Rectangle,
        actions: MutableList<Pair<Rectangle, () -> Unit>>
    ) {
        // الإطار الحجري أضيق من مستطيل الـslot: احجز حافة مستقلة للنص وأخرى للأزرار.
        val textInset = slot.width * .115f
        val buttonInset = slot.width * .065f
        val content = Rectangle(
            slot.x + textInset,
            slot.y + slot.height * .07f,
            slot.width - textInset * 2f,
            slot.height * .86f
        )
        val buttonArea = Rectangle(
            slot.x + buttonInset,
            content.y,
            slot.width - buttonInset * 2f,
            content.height * .30f
        )
        game.assets.accentHeadingFont.color = Color.WHITE
        drawBoardOfferText(
            game.assets.accentHeadingFont,
            offer.title.uppercase(),
            content.x,
            content.y + content.height * .92f,
            content.width,
            .68f
        )
        offer.image()?.let { image ->
            drawFit(
                image,
                content.x + content.width * .32f,
                content.y + content.height * .46f,
                content.width * .36f,
                content.height * .32f
            )
        }
        game.assets.shopFont.color = Color.WHITE
        drawBoardOfferText(
            game.assets.shopFont,
            offer.status,
            content.x,
            content.y + content.height * .43f,
            content.width,
            .58f
        )
        val buttons = listOfNotNull(offer.first, offer.second)
        val gap = 5f
        val buttonW = (buttonArea.width - gap * (buttons.size - 1)) / buttons.size
        buttons.forEachIndexed { index, button ->
            val rect = Rectangle(
                buttonArea.x + index * (buttonW + gap),
                buttonArea.y,
                buttonW,
                buttonArea.height
            )
            val tagName = when {
                button.label.endsWith(" COINS") -> "coin_price_tag"
                button.label.endsWith(" GEMS") -> "diamond_price_tag"
                button.label.startsWith("BUY ") -> "real_money_price_tag"
                else -> if (button.enabled) "ui_button_green_blank" else "ui_button_dark_blank"
            }
            val tag = game.assets.boards2.requireRegion(tagName)
            val tagAspect = tag.regionWidth.toFloat() / tag.regionHeight
            // الزر الواحد أعرض من الأصل المصغّر؛ والأزرار المزدوجة تملأ العرض المتاح.
            val tagWidth = minOf(
                rect.width * (if (buttons.size == 1) .82f else .98f),
                rect.height * tagAspect * (if (buttons.size == 1) 1.2f else 1f)
            )
            val tagHeight = rect.height * .94f
            val tagRect = Rectangle(
                rect.x + (rect.width - tagWidth) * .5f,
                rect.y + (rect.height - tagHeight) * .5f,
                tagWidth,
                tagHeight
            )
            batch.draw(tag, tagRect.x, tagRect.y, tagRect.width, tagRect.height)
            val currencyTag = tagName == "coin_price_tag" || tagName == "diamond_price_tag"
            val priceText = when (tagName) {
                "coin_price_tag" -> button.label.removeSuffix(" COINS")
                "diamond_price_tag" -> button.label.removeSuffix(" GEMS")
                "real_money_price_tag" -> button.label.removePrefix("BUY ")
                else -> button.label
            }
            val labelInset = if (currencyTag) tagRect.width * .26f else 3f
            drawBoardOfferText(
                game.assets.shopFont,
                priceText,
                tagRect.x + labelInset,
                tagRect.y + tagRect.height * .70f,
                tagRect.width - labelInset - 3f,
                if (buttons.size == 1) .58f else .52f
            )
            if (button.enabled) actions += rect to button.run
        }
        // Selecting the artwork/name follows the primary action; the currency buttons above
        // retain priority when a product offers both Coins and Gems.
        val previewAction = offer.preview ?: offer.first?.takeIf { it.enabled }?.run
        previewAction?.let { actions += slot to it }
    }

    private fun drawBoardPagination(
        board: Rectangle,
        page: Int,
        total: Int,
        actions: MutableList<Pair<Rectangle, () -> Unit>>
    ) {
        val previous = game.assets.boards.placement("shop_board", "shop_scroll_previous_button", board)
        val next = game.assets.boards.placement("shop_board", "shop_scroll_next_button", board)
        val between = Rectangle(
            previous.x + previous.width + 4f,
            previous.y + 5f,
            next.x - previous.x - previous.width - 8f,
            minOf(previous.height, next.height) - 10f
        )
        val bar = game.assets.boards2.contain("pagination_bar", between)
        batch.draw(game.assets.boards2.requireRegion("pagination_bar"), bar.x, bar.y, bar.width, bar.height)
        // The authored bar reads left to right. Its JSON area labels are reversed visually.
        val currentArea = game.assets.boards2.placement("pagination_bar", "pagination_total_pages_area", bar)
        val totalArea = game.assets.boards2.placement("pagination_bar", "pagination_current_page_area", bar)
        game.assets.hudLabelFont.color = Color.WHITE
        fittedText(
            game.assets.hudLabelFont,
            (page + 1).toString(),
            currentArea.x,
            currentArea.y + currentArea.height * .9f,
            currentArea.width,
            .65f
        )
        fittedText(
            game.assets.hudLabelFont,
            total.toString(),
            totalArea.x,
            totalArea.y + totalArea.height * .9f,
            totalArea.width,
            .65f
        )
        if (canAdvanceBoard(-1, page, total)) actions += previous to { advanceBoard(-1) }
        if (canAdvanceBoard(1, page, total)) actions += next to { advanceBoard(1) }
        drawArrowPulse(previous, previousArrowPulse)
        drawArrowPulse(next, nextArrowPulse)
    }

    private fun drawArrowPulse(rect: Rectangle, pulse: Float) {
        if (pulse <= 0f || game.progress.settings.reduceMotion) return
        val glow = game.assets.softGlowRegion
        batch.color = Color(1f, .77f, .32f, .28f * pulse)
        batch.draw(
            glow,
            rect.x - rect.width * .16f,
            rect.y - rect.height * .13f,
            rect.width * 1.32f,
            rect.height * 1.26f
        )
        batch.color = Color.WHITE
    }

    private fun canAdvanceBoard(step: Int, page: Int, total: Int): Boolean = page + step in 0 until total || visibleTabs.indexOf(activeTab) + step in visibleTabs.indices

    private fun selectBoardTab(tab: ShopTab) {
        if (activeTab == tab) return
        val direction = if (visibleTabs.indexOf(tab) > visibleTabs.indexOf(activeTab)) 1 else -1
        activeTab = tab
        if (tab == ShopTab.ORBS && shopGuidePage < 0 && game.heroGuideProgress.shouldTeachShopOrbs()) {
            shopGuidePage = 1
            shopGuideAnimation.reset()
        }
        boardTabSelectionTime = if (game.progress.settings.reduceMotion) 0f else .32f
        boardPageTransitionDirection = direction.toFloat()
        boardPageTransitionTime = if (game.progress.settings.reduceMotion) 0f else .24f
        message = ""
        game.uiFeedback.emit(UiFeedbackKind.CONFIRM)
    }

    private fun advanceBoard(step: Int) {
        val total = ShopPagination.pageCount(boardOffers().size)
        val current = pageByTab[activeTab] ?: 0
        val next = (current + step).coerceIn(0, total - 1)
        if (next != current) {
            pageByTab[activeTab] = next
            boardPageTransitionDirection = step.toFloat()
            boardPageTransitionTime = if (game.progress.settings.reduceMotion) 0f else .24f
            if (step < 0) previousArrowPulse = 1f else nextArrowPulse = 1f
            game.uiFeedback.emit(UiFeedbackKind.TAP)
        } else {
            visibleTabs.getOrNull(visibleTabs.indexOf(activeTab) + step)?.let { tab ->
                if (step < 0) previousArrowPulse = 1f else nextArrowPulse = 1f
                selectBoardTab(tab)
            }
        }
    }

    private fun drawBoardWallet(actions: MutableList<Pair<Rectangle, () -> Unit>>, board: Rectangle) {
        val atlas = game.assets.boards
        val scale = minOf(1.35f, board.width / 570f)
        val gap = 8f * scale
        val gemW = 164f * scale
        val coinW = 208f * scale
        val freeW = 133f * scale
        val h = 54f * scale
        val totalW = gemW + coinW + freeW + gap * 2f
        val x = board.x + (board.width - totalW) * .5f
        val y = board.y + board.height + 8f
        val gems = Rectangle(x, y, gemW, h)
        val coins = Rectangle(gems.x + gemW + gap, y, coinW, h)
        val free = Rectangle(coins.x + coinW + gap, y + 1.5f * scale, freeW, 51f * scale)
        batch.draw(atlas.requireRegion("shop_diamond_wallet_box"), gems.x, gems.y, gems.width, gems.height)
        batch.draw(atlas.requireRegion("shop_coin_wallet_box"), coins.x, coins.y, coins.width, coins.height)
        batch.draw(atlas.requireRegion("watch_free_ad_button"), free.x, free.y, free.width, free.height)
        val gemsValue = atlas.placement("shop_diamond_wallet_box", "shop_diamond_balance_value_area", gems)
        val coinsValue = atlas.placement("shop_coin_wallet_box", "shop_coin_balance_value_area", coins)
        val gemsPlus = atlas.placement("shop_diamond_wallet_box", "shop_diamond_plus_button", gems)
        val coinsPlus = atlas.placement("shop_coin_wallet_box", "shop_coin_plus_button", coins)
        game.assets.hudValueFont.color = Color.WHITE
        fittedText(
            game.assets.hudValueFont,
            WalletText.compact(displayedGems.toInt()),
            gemsValue.x,
            gemsValue.y + gemsValue.height * .72f,
            gemsValue.width,
            .58f + walletPulse * .04f
        )
        fittedText(
            game.assets.hudValueFont,
            WalletText.compact(displayedCoins.toInt()),
            coinsValue.x,
            coinsValue.y + coinsValue.height * .75f,
            coinsValue.width,
            .58f + walletPulse * .04f
        )
        actions += gemsValue to { walletPeek.show(coins = false) }
        actions += coinsValue to { walletPeek.show(coins = true) }
        actions += gemsPlus to {
            selectBoardTab(ShopTab.FEATURED)
            pageByTab[ShopTab.FEATURED] = 0
        }
        actions += coinsPlus to { message = "Earn Coins by clearing Volley stages." }
        actions += free to { adDialogOpen = true }
    }

    private fun drawBoardMessage(board: Rectangle) {
        val rect = Rectangle(
            board.x + board.width * .07f,
            board.y + board.height * .22f,
            board.width * .86f,
            42f
        )
        batch.color = Color(.10f, .04f, .025f, .90f)
        batch.draw(game.assets.solidUiRegion, rect.x, rect.y, rect.width, rect.height)
        batch.color = Color.WHITE
        fittedText(game.assets.shopFont, message, rect.x + 8f, rect.y + rect.height * .68f, rect.width - 16f, .55f)
    }

    private fun handleBoardPointer(board: Rectangle, actions: List<Pair<Rectangle, () -> Unit>>) {
        if (Gdx.input.justTouched()) {
            val p = touchPoint()
            pointerArmed = true
            pointerStartX = p.x
            pointerStartY = p.y
            pointerLastX = p.x
            pointerLastY = p.y
            pointerStartedInBoard = board.contains(p.x, p.y)
            return
        }
        if (!pointerArmed) return
        if (Gdx.input.isTouched) {
            val p = touchPoint()
            pointerLastX = p.x
            pointerLastY = p.y
            return
        }
        pointerArmed = false
        val p = touchPoint()
        val endX = if (kotlin.math.abs(pointerLastX - pointerStartX) > kotlin.math.abs(p.x - pointerStartX)) pointerLastX else p.x
        val endY = if (kotlin.math.abs(pointerLastY - pointerStartY) > kotlin.math.abs(p.y - pointerStartY)) pointerLastY else p.y
        val dx = endX - pointerStartX
        val dy = endY - pointerStartY
        if (pointerStartedInBoard && kotlin.math.abs(dx) > 65f && kotlin.math.abs(dx) > kotlin.math.abs(dy) * 1.2f) {
            advanceBoard(if (dx < 0f) 1 else -1)
            return
        }
        if (kotlin.math.abs(dx) > 24f || kotlin.math.abs(dy) > 24f) return
        actions.firstOrNull { it.first.contains(endX, endY) }?.second?.invoke()
    }

    private fun drawRewardedAdDialog(): Triple<Rectangle, Rectangle, Rectangle> {
        val screen = Rectangle(visibleLeft, visibleBottom, visibleWidth, visibleHeight)
        batch.color = Color(0f, 0f, 0f, .76f)
        batch.draw(game.assets.solidUiRegion, screen.x, screen.y, screen.width, screen.height)
        batch.color = Color.WHITE
        if (adPoolOpen) return drawRewardedOrbPool(screen)
        val board = game.assets.boards2.contain(
            "rewarded_ad_board",
            Rectangle(
                screen.x + screen.width * .08f,
                screen.y + screen.height * .23f,
                screen.width * .84f,
                screen.height * .54f
            )
        )
        batch.draw(game.assets.boards2.requireRegion("rewarded_ad_board"), board.x, board.y, board.width, board.height)
        // The original board artwork depicts discontinued random BOOSTS. Cover only that
        // illustration and its legacy subtitle with the exact current Orb reward.
        val rewardPanel = Rectangle(board.x + board.width * .06f,
            board.y + board.height * .325f, board.width * .88f, board.height * .315f)
        game.assets.uiRenderer.drawGradientPanel(batch, rewardPanel, ForgeUiRenderer.GradientStyle.NEUTRAL)
        val orbArt = game.assets.atlasUiIcon("orb")
        if (orbArt != null) {
            val size = rewardPanel.height * .58f
            batch.color = Color.WHITE
            batch.draw(orbArt, rewardPanel.x + 18f, rewardPanel.y + (rewardPanel.height - size) / 2f, size, size)
        }
        game.assets.shopFont.color = Color.WHITE
        val bagFull = game.progress.orbStashCount >= 150
        fittedText(game.assets.shopFont, if (bagFull) "1 UPGRADE POINT" else "1 RANDOM STANDARD ORB",
            rewardPanel.x + rewardPanel.width * .23f, rewardPanel.y + rewardPanel.height * .77f,
            rewardPanel.width * .73f, .68f)
        fittedText(game.assets.shopFont, if (bagFull) "BAG FULL: NO ORB" else "+ 1 UPGRADE POINT",
            rewardPanel.x + rewardPanel.width * .23f, rewardPanel.y + rewardPanel.height * .49f,
            rewardPanel.width * .73f, .65f)
        fittedText(game.assets.shopFont, "COMPLETE THE AD TO EARN",
            rewardPanel.x + rewardPanel.width * .23f, rewardPanel.y + rewardPanel.height * .22f,
            rewardPanel.width * .73f, .50f)
        val watch = game.assets.boards2.placement("rewarded_ad_board", "rewarded_ad_watch_button", board)
        // The subtitle, icons, and WATCH / AD label belong to the artist's board. Only the
        // small existing count line changes after a real earned reward.
        val daily = shopDailyRewards.currentState(System.currentTimeMillis())
        if (daily.claimedToday != 0 || daily.dailyLimit != 5) {
            val countDigits = Rectangle(
                watch.x + watch.width * .275f,
                watch.y + watch.height * .15f,
                watch.width * .075f,
                watch.height * .19f
            )
            val authoredButton = game.assets.boards2.requireRegion("rewarded_ad_watch_button")
            val cleanBlue = TextureRegion(authoredButton, 74, 59, 16, 17)
            batch.draw(cleanBlue, countDigits.x, countDigits.y, countDigits.width, countDigits.height)
            val oldColor = Color(game.assets.shopFont.color)
            game.assets.shopFont.color = Color(.72f, .86f, 1f, 1f)
            fittedText(
                game.assets.shopFont,
                "${daily.claimedToday}/${daily.dailyLimit}",
                countDigits.x,
                countDigits.y + countDigits.height * .40f,
                countDigits.width,
                .48f
            )
            game.assets.shopFont.color = oldColor
        }
        // Exactly one point per AdMob-earned reward; no random legacy boosts.
        // The old board illustration is decorative; draw the precise reward on top.
        val legendTop = board.y - screen.height * .025f
        game.assets.shopFont.color = Color(1f, .91f, .74f, 1f)
        fittedText(game.assets.shopFont, "FULL BAG: 1 UPGRADE POINT ONLY", board.x + 15f,
            legendTop, board.width - 30f, .58f)
        val details = Rectangle(board.x + board.width * .12f,
            legendTop - screen.height * .075f, board.width * .76f, screen.height * .05f)
        game.assets.uiRenderer.drawGradientPanel(batch, details, ForgeUiRenderer.GradientStyle.NEUTRAL)
        fittedText(game.assets.shopFont, "POSSIBLE ORBS", details.x + 8f,
            details.y + details.height * .66f, details.width - 16f, .68f)
        game.assets.shopFont.color = Color.WHITE
        val close = Rectangle(
            board.x + board.width * .88f,
            board.y + board.height * .79f,
            board.width * .12f,
            board.height * .18f
        )
        batch.draw(game.assets.boards2.requireRegion("ui_button_red_blank"), close.x, close.y, close.width, close.height)
        fittedText(game.assets.shopFont, "X", close.x + 2f, close.y + close.height * .7f, close.width - 4f, .74f)
        return Triple(close, watch, details)
    }

    /** Viewing outcomes never launches an ad. The same pool drives the actual grant. */
    private fun drawRewardedOrbPool(screen: Rectangle): Triple<Rectangle, Rectangle, Rectangle> {
        val panel = Rectangle(screen.x + screen.width * .06f, screen.y + screen.height * .17f,
            screen.width * .88f, screen.height * .66f)
        game.assets.uiRenderer.drawGradientPanel(batch, panel, ForgeUiRenderer.GradientStyle.NEUTRAL)
        game.assets.shopFont.color = Color.WHITE
        fittedText(game.assets.shopFont, "POSSIBLE STANDARD ORBS", panel.x + 16f,
            panel.y + panel.height * .94f, panel.width - 32f, .82f)
        val pool = VolleySourceOrbDrops.rewardedStandardPool()
        fittedText(game.assets.shopFont, "1 RANDOM ORB - EACH TYPE HAS THE SAME CHANCE",
            panel.x + 16f, panel.y + panel.height * .88f, panel.width - 32f, .54f)
        if (!game.assets.orbIconsLoaded()) game.assets.orbIcon(0)
        val columns = 3
        val rows = ((pool.size + columns - 1) / columns).coerceAtLeast(1)
        val cellWidth = (panel.width - 24f) / columns
        val cellHeight = panel.height * .58f / rows
        pool.forEachIndexed { index, orb ->
            val x = panel.x + 12f + index % columns * cellWidth
            val y = panel.y + panel.height * .82f - (index / columns + 1) * cellHeight
            val size = minOf(cellHeight * .62f, cellWidth * .58f)
            game.assets.orbIcon(orb.id)?.let { icon ->
                batch.draw(icon, x + (cellWidth - size) / 2f, y + cellHeight * .30f, size, size)
            }
            val label = orb.name.removePrefix("STANDARD ").removeSuffix(" ORB")
            fittedText(game.assets.shopFont, label, x + 4f, y + cellHeight * .23f,
                cellWidth - 8f, .56f)
        }
        fittedText(game.assets.shopFont, "ORB STATS MAY VARY. +1 UPGRADE POINT GUARANTEED.",
            panel.x + 12f, panel.y + panel.height * .20f, panel.width - 24f, .51f)
        fittedText(game.assets.shopFont, "FULL BAG: 1 UPGRADE POINT ONLY",
            panel.x + 12f, panel.y + panel.height * .15f, panel.width - 24f, .54f)
        val back = Rectangle(panel.x + panel.width * .18f, panel.y + panel.height * .035f,
            panel.width * .64f, panel.height * .08f)
        game.assets.uiRenderer.drawGradientPanel(batch, back, ForgeUiRenderer.GradientStyle.NEUTRAL)
        fittedText(game.assets.shopFont, "BACK TO OFFER", back.x + 8f,
            back.y + back.height * .68f, back.width - 16f, .70f)
        return Triple(back, Rectangle(), Rectangle())
    }

    private fun drawTabTransition() {
        if (tabTransitionTime <= 0f || game.progress.settings.reduceMotion) return
        val p = 1f - (tabTransitionTime / .24f).coerceIn(0f, 1f)
        val panel = shopPanel(300f)
        val alpha = (1f - p) * .18f
        batch.color = Color(.01f, .025f, .055f, alpha)
        batch.draw(game.assets.solidUiRegion, panel.x, panel.y, panel.width, panel.height)
        val bandW = panel.width * .05f
        val x = if (tabTransitionDirection > 0f) {
            panel.x + panel.width * p
        } else {
            panel.x + panel.width * (1f - p)
        }
        batch.color = Color(ForgeUiPalette.ivory).also { it.a = (1f - p) * .30f }
        batch.draw(game.assets.softGlowRegion, x - bandW / 2f, panel.y, bandW, panel.height)
        batch.color = Color.WHITE
    }

    /**
     * ملاحظة صيانة: الدالة `beginStore` تجهّز الخلفية وتبدأ `batch` لكل إطار؛ يجب أن تُستدعى مرة واحدة
     * فقط في بداية `render` قبل أي عملية رسم أخرى.
     */
    private fun beginStore() {
        WorldVideoBackgrounds.hide()
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f)
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT)
        viewport.apply()
        batch.projectionMatrix = camera.combined
        batch.begin()
        batch.color = Color.WHITE
        drawFullscreen(game.assets.volleyWorldBackground(1))
        drawLandscapeScrim(.48f)
        batch.color = Color(.003f, .006f, .012f, .46f)
        drawFullscreenPanel(game.assets.solidUiRegion)
        batch.color = Color.WHITE
    }

    /**
     * ملاحظة صيانة: الدالة `drawHeader` ترسم عنوان "SHOP" النصي وشريط الحالة العلوي (عدد الكرات/المضارب/
     * الأدوات المملوكة حاليًا)؛ لا تُعيد أي مستطيل قابل للمس ولا تُغيّر حالة اللعبة.
     */
    private fun drawHeader() {
        batch.color = Color.WHITE
        // عنوان نصي بدل صورة زر SHOP، حتى يطابق أسلوب بقية عناوين الواجهة.
        game.assets.titleFont.color = ForgeUiPalette.textPrimary
        fittedText(game.assets.titleFont, "VOLLEY SHOP", 55f, 1560f, 790f, .92f)
        game.assets.titleFont.color = ForgeUiPalette.textPrimary

        // خانات الإحصاء تُحسب من العناصر المتاحة فعلياً في Volley.
        val chipGap = SPACE_SM
        val chipHeight = 68f
        val chipY = 1388f
        val statsWidth = if (isLandscape) minOf(shopContentWidth(), 1500f) else 800f
        val statsLeft = 450f - statsWidth / 2f
        val stats = if (isLandscape) {
            listOf(
                "BALLS" to "${game.cosmeticProgression.ownedBallCount()}/${game.cosmeticProgression.totalBallCount()}",
                "MODE" to "VOLLEY",
                "ORBS" to "${VolleyOrbCatalog.families().count { game.progress.orbRank(it) >= 0 }}",
                "COINS" to displayedCoins.toInt().toString(),
                "GEMS" to displayedGems.toInt().toString()
            )
        } else {
            listOf(
                "BALLS" to "${game.cosmeticProgression.ownedBallCount()}/${game.cosmeticProgression.totalBallCount()}",
                "MODE" to "VOLLEY",
                "ORBS" to "${VolleyOrbCatalog.families().count { game.progress.orbRank(it) >= 0 }}"
            )
        }
        val chipWidth = (statsWidth - chipGap * (stats.size - 1)) / stats.size
        stats.forEachIndexed { index, (label, value) ->
            val chipX = statsLeft + index * (chipWidth + chipGap)
            game.assets.uiRenderer.drawGradientPanel(
                batch,
                Rectangle(chipX, chipY, chipWidth, chipHeight),
                ForgeUiRenderer.GradientStyle.NEUTRAL
            )
            if ((label == "COINS" || label == "GEMS") && walletPulse > 0f) {
                batch.color = Color(ForgeUiPalette.primary).also { it.a = walletPulse * .16f }
                batch.draw(game.assets.softGlowRegion, chipX - 8f, chipY - 8f, chipWidth + 16f, chipHeight + 16f)
                batch.color = Color.WHITE
            }
            game.assets.shopFont.color = muted
            val walletIcon = when (label) {
                "COINS" -> game.assets.currencyCoinIcon
                "GEMS" -> game.assets.currencyGemIcon
                else -> null
            }
            if (walletIcon != null) {
                drawFit(walletIcon, chipX + 8f, chipY + 15f, 35f, 35f)
            }
            val textInset = if (walletIcon != null) 45f else SPACE_SM
            fittedText(game.assets.shopFont, label, chipX + textInset, chipY + chipHeight - 16f, chipWidth - textInset - SPACE_SM, SCALE_CAPTION)
            fittedText(game.assets.hudLabelFont, value, chipX + textInset, chipY + 28f, chipWidth - textInset - SPACE_SM, SCALE_ITEM_TITLE)
            game.assets.shopFont.color = ForgeUiPalette.textPrimary
        }
    }

    /**
     * ملاحظة صيانة: الدالة `drawTabs` ترسم أزرار التبويبات الخمسة أعلى الشاشة وتعيد مستطيلاتها مع
     * نوع كل تبويب لتُفحص لاحقًا في `render` عند اللمس. تم تكبير الارتفاع والخط قليلاً هنا فقط لتحسين
     * سهولة اللمس ووضوح القراءة على الشاشات الصغيرة.
     */
    private fun drawTabs(): List<Pair<Rectangle, ShopTab>> {
        val y = 1280f
        val height = 92f
        val gap = 10f
        val tabsWidth = if (isLandscape) minOf(shopContentWidth(), 1800f) else 844f
        val x0 = 450f - tabsWidth / 2f
        val width = (tabsWidth - gap * (visibleTabs.size - 1)) / visibleTabs.size
        return visibleTabs.mapIndexed { index, tab ->
            val x = x0 + index * (width + gap)
            val rect = Rectangle(x, y, width, height)
            game.assets.uiRenderer.drawGradientButton(
                batch,
                rect,
                if (activeTab == tab) ForgeUiRenderer.GradientStyle.PRIMARY else ForgeUiRenderer.GradientStyle.NEUTRAL
            )
            if (activeTab == tab) {
                val pulse = selectionPulse()
                batch.color = Color(ForgeUiPalette.primary).also { it.a = .09f + pulse * .08f }
                batch.draw(game.assets.softGlowRegion, rect.x, rect.y - 12f, rect.width, 18f)
                batch.color = Color.WHITE
                game.assets.uiRenderer.drawGradientProgress(
                    batch,
                    Rectangle(rect.x + 16f, rect.y + 2f, rect.width - 32f, 5f),
                    ForgeUiRenderer.GradientStyle.PRIMARY
                )
            }
            val icon = game.assets.atlasUiIcon(tab.iconName) ?: com.badlogic.gdx.graphics.g2d.TextureRegion(game.assets.gameIcon(tab.iconName))
            val iconSize = minOf(48f, rect.height * .52f, rect.width * .20f)
            val compact = rect.width < 170f
            val iconX = if (compact) rect.x + (rect.width - iconSize) / 2f else rect.x + 12f
            val iconY = rect.y + (rect.height - iconSize) / 2f + if (compact) 7f else 0f
            batch.color = Color.WHITE
            batch.draw(icon, iconX, iconY, iconSize, iconSize)
            game.assets.hudLabelFont.color = if (activeTab == tab) ForgeUiPalette.textPrimary else ForgeUiPalette.textSecondary
            if (!compact) {
                val textX = iconX + iconSize + 8f
                fittedText(game.assets.hudLabelFont, tab.label, textX, rect.y + 63f, rect.x + rect.width - SPACE_SM - textX, .70f)
            } else {
                fittedText(game.assets.hudLabelFont, tab.label, rect.x + 5f, rect.y + 24f, rect.width - 10f, .52f)
            }
            game.assets.hudLabelFont.color = ForgeUiPalette.textPrimary
            rect to tab
        }
    }

    /**
     * ملاحظة صيانة: الدالة `drawSectionTitle` ترسم عنوان القسم وسطره الفرعي بنفس الموضع الثابت أعلى
     * كل تبويب. عدّل الإحداثيات هنا فقط إن أردت تغيير مكان كل العناوين دفعة واحدة في كل الأقسام.
     */
    private fun drawSectionTitle(title: String, subtitle: String) {
        val left = shopContentLeft()
        val width = shopContentWidth()
        game.assets.accentHeadingFont.color = primaryLight
        fittedText(game.assets.accentHeadingFont, title, left, 1240f, width, SCALE_SECTION_TITLE)
        game.assets.accentHeadingFont.color = ForgeUiPalette.textPrimary
        game.assets.shopFont.color = muted
        fittedText(game.assets.shopFont, subtitle, left, 1200f, width, SCALE_BODY)
        game.assets.shopFont.color = ForgeUiPalette.textPrimary
    }

    // ==================================================================================
    // أدوات التمرير (Scroll) العامة — تُستخدم من قبل BALLS و ITEMS فقط، لأن هذه
    // الأقسام الثلاثة هي التي تحتوي على قوائم طويلة قد تتجاوز ارتفاع الشاشة.
    // ==================================================================================

    /**
     * ملاحظة صيانة: الدالة `scrollOffset` تعيد إزاحة التمرير الحالية المحفوظة لتبويب معيّن (صفر إن لم
     * يُمرَّر بعد أبدًا). لا تُغيّر أي حالة، للقراءة فقط.
     */
    private fun scrollOffset(tab: ShopTab): Float = scrollOffsets.getOrDefault(tab, 0f)

    /**
     * ملاحظة صيانة: الدالة `maxScrollOffset` تحسب أقصى إزاحة تمرير ممكنة بحيث لا تُكشف مساحة فارغة
     * أسفل آخر عنصر. راجعها إن غيّرت ارتفاع البطاقات أو المسافات بينها في أي قسم قابل للتمرير.
     */
    private fun maxScrollOffset(contentHeight: Float, panelBounds: Rectangle): Float = (contentHeight - panelBounds.height).coerceAtLeast(0f)

    /**
     * ملاحظة صيانة: الدالة `updateScrollDrag` تتابع لمسة المستخدم (لمس شاشة حقيقي أو سحب بالماوس على
     * سطح المكتب) وتُحدّث إزاحة تمرير التبويب المعطى تبعًا لحركة الإصبع/المؤشر. يجب استدعاؤها مرة واحدة
     * فقط في كل إطار، قبل رسم بطاقات القسم مباشرة. تُحوَّل إحداثيات اللمس من شاشة الجهاز إلى عالم
     * اللعبة عبر `viewport.unproject` حتى تعمل بشكل صحيح مهما كان حجم الشاشة الفعلي أو نسبة أبعادها.
     */
    private fun updateScrollDrag(tab: ShopTab, panelBounds: Rectangle, contentHeight: Float) {
        if (purchaseConfirmation.pending != null) {
            scrollDragActive = false
            return
        }
        val maxOffset = maxScrollOffset(contentHeight, panelBounds)
        var offset = scrollOffsets.getOrDefault(tab, 0f)

        if (Gdx.input.isTouched()) {
            touchWorld.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
            viewport.unproject(touchWorld)
            if (Gdx.input.justTouched()) {
                // بداية لمسة جديدة: فقط نحدد إن كانت داخل منطقة هذا القسم القابل للتمرير، بدون أي تحريك بعد.
                scrollDragActive = panelBounds.contains(touchWorld.x, touchWorld.y)
                lastTouchWorldY = touchWorld.y
            } else if (scrollDragActive) {
                offset += touchWorld.y - lastTouchWorldY
                lastTouchWorldY = touchWorld.y
            }
        } else {
            scrollDragActive = false
        }

        scrollOffsets[tab] = offset.coerceIn(0f, maxOffset)
    }

    private fun handleMouseWheel(amountY: Float): Boolean {
        if (purchaseConfirmation.pending != null) return true
        if (amountY == 0f || activeTab !in setOf(ShopTab.BALLS, ShopTab.ORBS)) return false
        touchWorld.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
        viewport.unproject(touchWorld)
        val panelBounds = shopPanel(if (activeTab == ShopTab.ORBS) 345f else 325f)
        if (!panelBounds.contains(touchWorld.x, touchWorld.y)) return false

        val contentHeight = when (activeTab) {
            ShopTab.BALLS -> {
                val columns = if (isLandscape) 4 else 2
                val rows = (game.assets.cosmetics.balls.size + columns - 1) / columns
                val height = if (isLandscape) 310f else 255f
                12f + rows * (height + 24f) - 24f + 12f
            }

            ShopTab.ORBS -> {
                val columns = if (isLandscape) 3 else 2
                val rows = (SHOP_ELIGIBLE_TYPES.size + columns - 1) / columns
                val height = if (isLandscape) 300f else 278f
                12f + rows * (height + 24f) - 24f + 12f
            }

            else -> 0f
        }
        val maxOffset = maxScrollOffset(contentHeight, panelBounds)
        val next = (scrollOffset(activeTab) + amountY * 230f).coerceIn(0f, maxOffset)
        scrollOffsets[activeTab] = next
        return true
    }

    /**
     * ملاحظة صيانة: الدالة `beginClippedRegion` تبدأ قص الرسم (Scissor Test) عند حدود مستطيل معيّن،
     * بحيث لا تظهر أي بطاقة خارج منطقة العرض القابلة للتمرير. يجب إغلاقها دائمًا عبر
     * `endClippedRegion` بعد الانتهاء من رسم كل عناصر القسم، وإلا سيبقى القص فعّالاً لبقية الشاشة بالخطأ.
     */
    private fun beginClippedRegion(rect: Rectangle) {
        batch.flush()

        val scissors = Rectangle()
        ScissorStack.calculateScissors(
            camera,
            batch.transformMatrix,
            rect,
            scissors
        )

        scissorPushed = ScissorStack.pushScissors(scissors)
    }

    /**
     * ملاحظة صيانة: الدالة `endClippedRegion` تُنهي القص الذي بدأه `beginClippedRegion`؛ لا تُستدعى
     * أبدًا بمفردها دون استدعاء `beginClippedRegion` قبلها في نفس الإطار.
     */
    private fun endClippedRegion() {
        batch.flush()

        if (scissorPushed) {
            ScissorStack.popScissors()
            scissorPushed = false
        }
    }

    /**
     * ملاحظة صيانة: الدالة `drawScrollTrack` ترسم شريط تمرير رفيعًا على الحافة اليمنى لمنطقة قابلة
     * للتمرير لتنبيه اللاعب بصريًا أن بإمكانه السحب لرؤية المزيد. لا ترسم شيئًا إن كان كل المحتوى ظاهرًا
     * أصلاً (`maxOffset` قريب من الصفر).
     */
    private fun drawScrollTrack(panelBounds: Rectangle, offset: Float, maxOffset: Float) {
        if (maxOffset <= 1f) return
        val trackX = panelBounds.x + panelBounds.width - 8f
        game.assets.uiRenderer.drawGradientPanel(
            batch,
            Rectangle(trackX, panelBounds.y, 6f, panelBounds.height),
            ForgeUiRenderer.GradientStyle.NEUTRAL
        )
        val thumbHeight = (panelBounds.height * (panelBounds.height / (panelBounds.height + maxOffset)))
            .coerceIn(60f, panelBounds.height)
        val progress = (offset / maxOffset).coerceIn(0f, 1f)
        val thumbY = panelBounds.y + (panelBounds.height - thumbHeight) * (1f - progress)
        game.assets.uiRenderer.drawGradientProgress(
            batch,
            Rectangle(trackX, thumbY, 6f, thumbHeight),
            ForgeUiRenderer.GradientStyle.PRIMARY
        )
    }

    /** Four offers fit in the featured area with a larger hero icon and clear reward text. */
    private fun drawFeatured(actions: MutableList<Pair<Rectangle, () -> Unit>>) {
        drawSectionTitle(
            "GEM STORE",
            "Buy Gems securely with Google Play, then upgrade Orbs or unlock Balls."
        )

        val products = ShopCatalog.products
        products.forEachIndexed { index, product ->
            val columns = if (isLandscape) 2 else 1
            val gap = if (isLandscape) SPACE_LG else 0f
            val cardWidth = if (isLandscape) (shopContentWidth() - gap) / 2f else 790f
            val cardHeight = if (isLandscape) 400f else 205f
            val column = index % columns
            val row = index / columns
            val x = if (isLandscape) shopContentLeft() + column * (cardWidth + gap) else 55f
            val y = if (isLandscape) 735f - row * 424f else 940f - index * 224f
            val accent = when (index) {
                0 -> primary
                1 -> crimson
                2 -> primary
                else -> success
            }
            val buy = drawWideProductCard(product, x, y, cardWidth, cardHeight, accent)
            if (game.developmentAccess.canMakeTestPurchase(
                    game.monetization.purchaseGateway.price(product) != null,
                    purchaseBusy
                )
            ) {
                actions += buy to {
                    val localizedPrice = displayedPrice(product)
                    if (localizedPrice != null) {
                        askPurchaseConfirmation(
                            product.title,
                            localizedPrice,
                            if (game.developmentAccess.enabled) {
                                "Development test grant: +${product.gemAmount} Gems."
                            } else {
                                "Receive ${product.gemAmount} Gems. Google Play confirms payment."
                            }
                        ) { purchase(product) }
                    }
                }
            }
        }
    }

    /** Draws every ball as an individual unlock; there are no ball themes/groups anymore. */
    private fun drawBalls(actions: MutableList<Pair<Rectangle, () -> Unit>>) {
        val balls = VolleyBallSelection.inDisplayOrder(game.assets.cosmetics.balls, shopMode)
        val ownedCount = balls.count { VolleyBallSelection.canUse(shopMode, it, game.cosmeticProgression.ownsBall(it)) }
        val nextUnlock = balls.asSequence()
            .filterNot { VolleyBallSelection.canUse(shopMode, it, game.cosmeticProgression.ownsBall(it)) }
            .mapNotNull { ball -> game.cosmeticProgression.ballUnlockStage(ball)?.let { ball to it } }
            .minByOrNull { it.second }
        val nextText = nextUnlock?.let { "Next unlock: Stage ${it.second}" } ?: "All balls unlocked"
        drawSectionTitle("BALL COLLECTION", "$ownedCount/${balls.size} balls unlocked. $nextText.")

        val panelBounds = shopPanel(325f)
        val columns = if (isLandscape) 4 else 2
        val cardHeight = if (isLandscape) 310f else 255f
        val colGap = if (isLandscape) SPACE_LG else SPACE_MD
        val rowGap = SPACE_LG
        val cardWidth = (panelBounds.width - 20f - colGap * (columns - 1)) / columns
        val colOffset = cardWidth + colGap
        val rows = if (balls.isEmpty()) 0 else ceil(balls.size / columns.toDouble()).toInt()
        val rowSpacing = cardHeight + rowGap
        val clipInset = SPACE_SM
        val contentHeight = if (rows == 0) 0f else rows * rowSpacing - rowGap + clipInset * 2f
        updateScrollDrag(ShopTab.BALLS, panelBounds, contentHeight)
        val offset = scrollOffset(ShopTab.BALLS)

        beginClippedRegion(panelBounds)
        balls.forEachIndexed { index, definition ->
            val col = index % columns
            val row = index / columns
            val y = panelBounds.y + panelBounds.height - clipInset - cardHeight - row * rowSpacing + offset
            if (y + cardHeight < panelBounds.y || y > panelBounds.y + panelBounds.height) return@forEachIndexed

            val rect = Rectangle(panelBounds.x + col * colOffset, y, cardWidth, cardHeight)
            val owned = game.developmentAccess.canUseCosmetic(
                VolleyBallSelection.canUse(shopMode, definition, game.cosmeticProgression.ownsBall(definition))
            )
            val settings = game.progress.settings
            val equipped = owned && run {
                settings.selectedVolleyBallGroupName == definition.groupName &&
                    settings.selectedVolleyBallSpriteName == definition.spriteName
            }
            val offer = BallCollectionCatalog.offer(definition)
            framedPanel(rect, if (equipped) primary else ForgeUiPalette.neutralBorder)

            fittedText(
                game.assets.hudLabelFont,
                definition.name.uppercase(),
                rect.x + 18f,
                rect.y + rect.height - 30f,
                rect.width - 36f,
                SCALE_ITEM_TITLE
            )

            val previewSize = if (isLandscape) 148f else 112f
            val previewX = rect.x + (rect.width - previewSize) / 2f
            game.assets.cosmetics.ballRegion(definition)?.let {
                drawFit(it, previewX, rect.y + if (isLandscape) 90f else 82f, previewSize, previewSize)
            }
            if (!owned) drawLockBadge(previewX + previewSize - 46f, rect.y + if (isLandscape) 192f else 148f, 46f)
            game.assets.shopFont.color = when {
                equipped -> success
                owned -> muted
                else -> primaryLight
            }
            fittedText(
                game.assets.shopFont,
                when {
                    equipped -> "EQUIPPED"
                    owned -> "UNLOCKED"
                    else -> "${offer.rarity.name} • ${game.cosmeticProgression.ballRequirement(definition)}"
                },
                rect.x + 18f,
                rect.y + 69f,
                rect.width - 36f,
                SCALE_BODY
            )
            game.assets.shopFont.color = ForgeUiPalette.textPrimary
            if (!owned && !game.developmentAccess.enabled) {
                val epicProduct = ShopCatalog.epicProductFor(definition.spriteName)
                val buttonGap = 8f
                val buttonWidth = if (epicProduct != null) (rect.width - 36f - buttonGap) / 2f else rect.width - 36f
                val firstRect = Rectangle(rect.x + 18f, rect.y + 12f, buttonWidth, 48f)
                when {
                    offer.coins != null -> {
                        val price = offer.coins
                        drawCurrencyPriceButton(firstRect, price, ShopCurrency.COINS, game.economy.coins >= price)
                        actions += firstRect to {
                            askPurchaseConfirmation(definition.name, "$price COINS", "Permanently unlock this ball.") {
                                unlockBallWithCoins(definition, price)
                            }
                        }
                    }

                    offer.gems != null -> {
                        val price = offer.gems
                        drawCurrencyPriceButton(firstRect, price, ShopCurrency.GEMS, game.economy.gems >= price)
                        actions += firstRect to {
                            askPurchaseConfirmation(definition.name, "$price GEMS", "Permanently unlock this ball.") {
                                unlockBallWithGems(definition, price)
                            }
                        }
                    }
                }
                if (epicProduct != null) {
                    val price = game.monetization.purchaseGateway.price(epicProduct)
                    val playRect = Rectangle(firstRect.x + buttonWidth + buttonGap, firstRect.y, buttonWidth, firstRect.height)
                    val playButton = shopButton(
                        price?.let { "BUY $it" } ?: "PLAY STORE",
                        playRect.x,
                        playRect.y,
                        playRect.width,
                        playRect.height,
                        primary,
                        .55f
                    )
                    if (price != null) {
                        actions += playButton to {
                            askPurchaseConfirmation(
                                definition.name,
                                price,
                                "Permanent Epic ball. Google Play confirms payment."
                            ) { purchase(epicProduct) }
                        }
                    }
                }
            }
        }
        endClippedRegion()
        drawScrollTrack(panelBounds, offset, maxScrollOffset(contentHeight, panelBounds))

        val openWidth = if (isLandscape) 700f else 530f
        val open = shopButton("OPEN BALL COLLECTION", 450f - openWidth / 2f, 205f, openWidth, 96f, primary, .88f, iconName = "ball")
        actions += open to { openCustomization() }
    }

    /** A compact seven-day progress rail, visible in both orientations without scrolling. */
    private fun drawSevenDayStreak() {
        val status = game.economy.dailyStreakStatus()
        val x = shopContentLeft()
        val w = shopContentWidth()
        val y = if (isLandscape) 1068f else 1031f
        val h = 112f
        val rail = Rectangle(x, y, w, h)
        framedPanel(rail, ForgeUiPalette.neutralBorder)
        val headline = if (status.claimedToday) {
            "DAY ${status.dayToClaim}/7 COLLECTED  •  COME BACK TOMORROW"
        } else {
            "DAILY CLAIM READY  •  DAY ${status.dayToClaim}/7 IN TODAY'S REWARDS"
        }
        game.assets.shopFont.color = ForgeUiPalette.textPrimary
        fittedText(game.assets.shopFont, headline, x + 14f, y + 94f, w - 28f, .68f)
        val outerPad = 18f
        val gap = 9f
        val chipW = (w - outerPad * 2f - gap * 6f) / 7f
        repeat(7) { i ->
            val day = i + 1
            val isCurrent = day == status.dayToClaim
            val style = when {
                isCurrent -> ForgeUiRenderer.GradientStyle.SUCCESS
                day <= status.completedDays -> ForgeUiRenderer.GradientStyle.PRIMARY
                else -> ForgeUiRenderer.GradientStyle.NEUTRAL
            }
            val chip = Rectangle(x + outerPad + i * (chipW + gap), y + 13f, chipW, 58f)
            game.assets.uiRenderer.drawGradientButton(batch, chip, style)
            game.assets.shopFont.color = game.assets.uiRenderer.labelColor(style)
            fittedText(game.assets.shopFont, if (day == 7) "GIFT 7" else "DAY $day", chip.x + 3f, chip.y + 38f, chip.width - 6f, .63f)
        }
        game.assets.shopFont.color = ForgeUiPalette.textPrimary
    }

    private fun drawCollectionProgress(x: Float, y: Float, width: Float) {
        // أزياء المضارب أُزيلت من مسار Volley؛ مؤشر المجموعة يحسب الكرات فقط.
        val total = game.cosmeticProgression.totalBallCount()
        val owned = game.cosmeticProgression.ownedBallCount()
        val progress = if (total == 0) 0f else owned.toFloat() / total
        game.assets.uiRenderer.drawGradientProgress(
            batch,
            Rectangle(x, y, width, 28f),
            ForgeUiRenderer.GradientStyle.NEUTRAL
        )
        game.assets.uiRenderer.drawGradientProgress(
            batch,
            Rectangle(x + 3f, y + 3f, (width - 6f) * progress, 22f),
            ForgeUiRenderer.GradientStyle.SUCCESS
        )
        fittedText(game.assets.shopFont, "$owned / $total COSMETICS", x, y + 52f, width, .64f)
    }

    /** Fixed-quantity Gem pack: no random draws or paid Items. */
    private fun drawWideProductCard(
        product: ShopProduct,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        accent: Color
    ): Rectangle {
        val rect = Rectangle(x, y, width, height)
        framedPanel(rect, if (isLandscape) ForgeUiPalette.neutralBorder else accent)
        val heroSize = if (isLandscape) 220f else 122f
        val hero = Rectangle(rect.x + 20f, rect.y + if (isLandscape) 92f else 43f, heroSize, heroSize)
        game.assets.uiRenderer.drawGradientPanel(batch, hero, ForgeUiRenderer.GradientStyle.NEUTRAL)
        drawFit(game.assets.currencyGemIcon, hero.x + 8f, hero.y + 8f, hero.width - 16f, hero.height - 16f)

        val detailX = rect.x + if (isLandscape) 264f else 162f
        val purchaseWidth = if (isLandscape) 230f else 193f
        val purchaseX = rect.x + rect.width - purchaseWidth - 27f
        val detailWidth = (purchaseX - detailX - 20f).coerceAtLeast(220f)
        fittedText(game.assets.hudLabelFont, product.title, detailX, rect.y + rect.height - 35f, detailWidth, .92f)
        game.assets.shopFont.color = primaryLight
        fittedText(
            game.assets.shopFont,
            "+${product.gemAmount} GEMS",
            detailX,
            rect.y + (if (isLandscape) rect.height - 87f else 140f),
            detailWidth,
            .86f
        )
        game.assets.shopFont.color = muted
        fittedText(
            game.assets.shopFont,
            "GUARANTEED • NO RANDOM ITEMS",
            detailX,
            rect.y + if (isLandscape) 110f else 72f,
            detailWidth,
            .62f
        )
        fittedText(
            game.assets.shopFont,
            "Spend Gems on any Item in the Items tab.",
            detailX,
            rect.y + if (isLandscape) 78f else 37f,
            detailWidth,
            .61f
        )
        game.assets.shopFont.color = ForgeUiPalette.textPrimary

        val localizedPrice = displayedPrice(product)
        val state = shopProductUiState(localizedPrice, purchaseBusy)
        game.assets.shopFont.color = if (localizedPrice == null) muted else primaryLight
        val priceNote = when {
            game.developmentAccess.enabled -> "TEST GRANT"
            localizedPrice == null -> "NOT AVAILABLE ON GOOGLE PLAY"
            else -> "GOOGLE PLAY PRICE"
        }
        fittedText(
            game.assets.shopFont,
            priceNote,
            purchaseX,
            rect.y + if (isLandscape) 165f else rect.height - 50f,
            purchaseWidth,
            .64f
        )
        game.assets.shopFont.color = ForgeUiPalette.textPrimary
        return shopButton(
            if (localizedPrice == null) "UNAVAILABLE" else state.buttonLabel,
            purchaseX,
            rect.y + if (isLandscape) 42f else 50f,
            purchaseWidth,
            if (isLandscape) 105f else 83f,
            if (state.enabled) accent else ForgeUiPalette.disabled,
            .78f
        )
    }

    /**
     * ملاحظة صيانة: الدالة `compactProductButton` ترسم زر شراء مضغوطًا (يُستخدم أسفل تبويب ITEMS)؛
     * التسمية تتغيّر تلقائيًا حسب توفر السعر الفعلي من Google Play في تلك اللحظة.
     */
    private fun compactProductButton(product: ShopProduct, x: Float, y: Float, w: Float, accent: Color): Rectangle {
        val price = displayedPrice(product)
        val displayTitle = productTitle(product)
        val title = if (price != null) "$displayTitle • $price" else "$displayTitle • UNAVAILABLE"
        return shopButton(title, x, y, w, 94f, if (price != null) accent else ForgeUiPalette.disabled, .66f)
    }

    /**
     * ملاحظة صيانة: الدالة `drawFreeRewardCard` ترسم بطاقة الإعلان المكافئ اليومي وتعيد مستطيل الزر
     * فقط؛ حالة الزر (نصه وتفعيله) تعتمد بالكامل على `DailyRewardStore` و`RewardedAdGateway.state` ولا
     * تُمنح أي مكافأة من هذه الدالة مباشرة — المنح يحدث فقط داخل `claimReward`.
     */
    private fun framedPanel(rect: Rectangle, accent: Color) {
        game.assets.uiRenderer.drawGradientBorderPanel(batch, rect, gradientStyleFor(accent), 5f)
    }

    /**
     * ملاحظة صيانة: الدالة `drawMessage` ترسم شريط الرسالة السفلي (نتائج الشراء أو الإعلان)؛ تُستدعى
     * فقط من `render` عندما تكون `message` غير فارغة.
     */
    private fun drawMessage() {
        val width = if (isLandscape) minOf(shopContentWidth(), 1500f) else 760f
        val left = 450f - width / 2f
        batch.color = Color(ForgeUiPalette.navy).also { it.a = .98f }
        batch.draw(game.assets.solidUiRegion, left, 175f, width, 70f)
        batch.color = Color.WHITE
        fittedText(game.assets.shopFont, message, left + 20f, 219f, width - 40f, .74f)
    }

    /**
     * ملاحظة صيانة: الدالة `drawFit` ترسم أي `TextureRegion` داخل صندوق معيّن مع الحفاظ على نسبة
     * العرض إلى الارتفاع (Aspect Ratio) وتوسيطه بداخله؛ تُستخدم لكل أيقونات المضارب والكرات.
     */
    private fun drawFit(region: TextureRegion, x: Float, y: Float, width: Float, height: Float) {
        val scale = minOf(width / region.regionWidth, height / region.regionHeight)
        val drawWidth = region.regionWidth * scale
        val drawHeight = region.regionHeight * scale
        batch.color = Color.WHITE
        batch.draw(region, x + (width - drawWidth) / 2f, y + (height - drawHeight) / 2f, drawWidth, drawHeight)
    }

    /** Price control that stays legible at phone scale and uses the real currency artwork. */
    private fun drawCurrencyPriceButton(
        rect: Rectangle,
        amount: Int,
        currency: ShopCurrency,
        affordable: Boolean
    ) {
        val style = when {
            !affordable -> ForgeUiRenderer.GradientStyle.NEUTRAL
            currency == ShopCurrency.COINS -> ForgeUiRenderer.GradientStyle.SUCCESS
            else -> ForgeUiRenderer.GradientStyle.PRIMARY
        }
        game.assets.uiRenderer.drawGradientButton(batch, rect, style)
        val icon = if (currency == ShopCurrency.COINS) game.assets.currencyCoinIcon else game.assets.currencyGemIcon
        val iconSize = minOf(rect.height * .68f, rect.width * .30f)
        drawFit(icon, rect.x + 8f, rect.y + (rect.height - iconSize) / 2f, iconSize, iconSize)
        game.assets.shopFont.color = game.assets.uiRenderer.labelColor(style)
        fittedText(
            game.assets.shopFont,
            amount.toString(),
            rect.x + iconSize + 14f,
            rect.y + rect.height * .68f,
            rect.width - iconSize - 22f,
            .72f
        )
        game.assets.shopFont.color = ForgeUiPalette.textPrimary
    }

    /**
     * ملاحظة صيانة: الدالة `shopButton` ترسم زرًا موحّد الشكل مع نص يتكيّف مع عرضه، وتعيد مستطيله
     * ليُفحص لاحقًا عند اللمس؛ لا تحتوي على أي منطق نقر بحد ذاتها.
     */
    private fun shopButton(
        text: String,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        tint: Color,
        scale: Float = .82f,
        iconName: String? = null
    ): Rectangle {
        val rect = Rectangle(x, y, w, h)
        val point = touchPoint()
        val pressed = Gdx.input.isTouched && UiMetrics.containsExpanded(rect, point.x, point.y)
        val visual = Pressable.visualRect(rect, pressed, game.progress.settings.reduceMotion)
        if (pressed) {
            batch.color = Color(ForgeUiPalette.primary).also { it.a = .14f }
            batch.draw(game.assets.softGlowRegion, visual.x - 10f, visual.y - 10f, visual.width + 20f, visual.height + 20f)
            batch.color = Color.WHITE
        }
        game.assets.uiRenderer.drawGradientButton(batch, visual, gradientStyleFor(tint))
        val icon = iconName?.let { game.assets.gameIcon(it) }
        val iconSize = if (icon != null) minOf(visual.height * .58f, visual.width * .18f) else 0f
        val textX = if (icon != null) visual.x + 14f + iconSize + 10f else visual.x + 8f
        if (icon != null) {
            batch.color = Color.WHITE
            batch.draw(icon, visual.x + 14f, visual.y + (visual.height - iconSize) / 2f, iconSize, iconSize)
        }
        game.assets.shopFont.color = game.assets.uiRenderer.labelColor(gradientStyleFor(tint))
        fittedText(game.assets.shopFont, text, textX, visual.y + visual.height * .67f, visual.x + visual.width - 12f - textX, scale)
        game.assets.shopFont.color = ForgeUiPalette.textPrimary
        return rect
    }

    private fun gradientStyleFor(color: Color): ForgeUiRenderer.GradientStyle = when {
        color === ForgeUiPalette.disabled || color.a < .72f -> ForgeUiRenderer.GradientStyle.DISABLED

        // Danger and primary are both Crimson #9C2831. Match crimson FIRST;
        // the previous order classified these buttons as ivory (CRIMSON style).
        color == primary || color == primaryLight || color == ForgeUiPalette.primaryDark || color == danger -> ForgeUiRenderer.GradientStyle.PRIMARY

        // SUCCESS and CRIMSON aliases both map to the same ivory palette family.
        color == success || color == ForgeUiPalette.successLight || color == ForgeUiPalette.successDark ||
            color == crimson || color == ForgeUiPalette.crimsonLight || color == ForgeUiPalette.crimsonDark ->
            ForgeUiRenderer.GradientStyle.SUCCESS

        else -> ForgeUiRenderer.GradientStyle.NEUTRAL
    }

    private fun wrappedText(
        font: com.badlogic.gdx.graphics.g2d.BitmapFont,
        text: String,
        x: Float,
        y: Float,
        width: Float,
        scale: Float
    ) {
        val oldX = font.data.scaleX
        val oldY = font.data.scaleY
        font.data.setScale(scale * uiTextScale)
        font.draw(batch, text, x, y, width, Align.left, true)
        font.data.setScale(oldX, oldY)
    }

    /** ملاحظة صيانة: الدالة `openCustomization` تفتح شاشة التخصيص الكاملة، وتُخبرها إن كانت اللعبة متوقفة مؤقتًا حتى تعرف طريق الرجوع الصحيح. */
    private fun openCustomization(ball: BallSpriteDefinition? = null) {
        game.navigate(
            CustomizationScreen(
                game,
                returnToPausedGame = returnDestination == ShopReturnDestination.PAUSED_GAME,
                returnToShop = true,
                shopReturnDestination = returnDestination,
                shopReturnMode = shopMode,
                initialBallSpriteName = ball?.spriteName
            )
        )
    }

    private fun openBallDetails(ball: BallSpriteDefinition) {
        game.navigate(BallPowerDetailScreen(game, ball,
            returnToPausedGame = returnDestination == ShopReturnDestination.PAUSED_GAME,
            returnToShop = true, shopReturnDestination = returnDestination, shopReturnMode = shopMode))
    }

    /** ملاحظة صيانة: الدالة `handleRewardTap` تُستدعى فقط عند لمس زر الإعلان المكافئ؛ تختار الإجراء المناسب (عرض الإعلان/إعادة تحميله/رسالة انتظار) حسب حالة `RewardedAdGateway` الحالية دون منح أي مكافأة هنا. */
    private fun handleRewardTap() {
        val now = System.currentTimeMillis()
        if (adRequestPending || !shopDailyRewards.canWatch(now)) return
        when (game.monetization.rewardedTalismanAdGateway.state) {
            AdState.READY -> claimReward()

            AdState.UNAVAILABLE -> {
                message = "Retrying the rewarded ad…"
                game.monetization.rewardedTalismanAdGateway.preload()
            }

            AdState.LOADING -> message = "Rewarded video is loading…"

            AdState.SHOWING -> message = "Finish the video to earn your Item."

            AdState.DISABLED -> message = if (BuildConfig.DEBUG) "Rewarded ad unavailable (debug)." else "Rewarded ads are currently unavailable."
        }
    }

    /** Ask BEFORE deducting Coins/Gems or launching the Play Billing checkout. */
    private fun askPurchaseConfirmation(
        name: String,
        cost: String,
        detail: String,
        action: () -> Unit
    ) {
        if (purchaseBusy) return
        purchaseConfirmation.request(
            StorePurchaseConfirmation(name, cost, detail, action)
        )
    }

    /** The final shop layer: input below this dialog is ignored until confirm/cancel. */
    private fun drawPurchaseConfirmation(): Pair<Rectangle, Rectangle>? {
        val requested = purchaseConfirmation.pending ?: return null
        val hit = BoardConfirmationRenderer.draw(
            batch,
            game.assets,
            game.assets.shopFont,
            Rectangle(visibleLeft, visibleBottom, visibleWidth, visibleHeight),
            UiConfirmation(
                UiConfirmationPurpose.PURCHASE,
                requested.detail,
                requested.itemName,
                requested.costLabel,
                onConfirm = {}
            )
        )
        return hit.no to hit.yes
    }

    /** ملاحظة صيانة: الدالة `purchase` تبدأ تدفق شراء حقيقي عبر `PurchaseGateway`؛ ترفض أي منتج ليس ضمن `ShopCatalog.products` (أي منتج قديم/legacy) قبل فتح Google Play. */
    /** تشتري رتبة التميمة الجديدة فقط بعد التحقق من وجود رتبة أعلى وسعر صالح. */
    private fun purchaseOrbRank(family: String, useGems: Boolean, price: Int) {
        val next = VolleyOrbCatalog.nextRank(family, game.progress.orbRank(family))
        if (next == null || price != (if (useGems) OrbShopPricing.gems(next.rank) else OrbShopPricing.coins(next.rank))) {
            message = "ORB RANK NO LONGER AVAILABLE"
            game.uiFeedback.emit(UiFeedbackKind.ERROR)
            return
        }
        if (game.progress.unlockedLevel < next.stageRequirement) {
            message = "REACH STAGE ${next.stageRequirement} TO UNLOCK THIS ORB"
            game.uiFeedback.emit(UiFeedbackKind.ERROR)
            return
        }
        if (game.progress.orbStashCount >= 150) {
            message = "ORB BAG FULL (150/150)"
            game.uiFeedback.emit(UiFeedbackKind.ERROR)
            return
        }
        val updated = game.progress.buyOrbRank(family) {
            game.economy.spendForOrbRank(family, next.rank, price, useGems)
        }
        message = if (updated) "$family UPGRADED TO RANK ${next.rank}"
            else if (useGems) "NOT ENOUGH GEMS" else "NOT ENOUGH COINS"
        game.uiFeedback.emit(if (updated) UiFeedbackKind.PURCHASE else UiFeedbackKind.ERROR)
    }

    private fun unlockBallWithGems(ball: BallSpriteDefinition, price: Int) {
        if (BallCollectionCatalog.offer(ball).gems != price) return
        if (game.cosmeticProgression.ownsBall(ball)) return
        if (!game.economy.spendGems(price)) {
            message = "NOT ENOUGH GEMS"
            game.uiFeedback.emit(UiFeedbackKind.ERROR)
            return
        }
        game.cosmeticOwnership.unlockBall(ball.id)
        message = "${ball.name.uppercase()} UNLOCKED • -$price GEMS"
        game.assets.cosmetics.ballRegion(ball)?.let { region ->
            rewardFx.trigger(
                kind = RewardFxSystem.Kind.COSMETIC,
                icons = listOf(region),
                centerX = 450f,
                centerY = 820f,
                targetX = if (isLandscape) visibleLeft + visibleWidth - 130f else 760f,
                targetY = visibleBottom + visibleHeight - 105f,
                accent = ForgeUiPalette.purple
            )
        }
        game.uiFeedback.emit(UiFeedbackKind.UNLOCK)
    }

    private fun unlockBallWithCoins(ball: BallSpriteDefinition, price: Int) {
        if (BallCollectionCatalog.offer(ball).coins != price) return
        if (game.cosmeticProgression.ownsBall(ball)) return
        if (!game.economy.spendCoins(price)) {
            message = "NOT ENOUGH COINS"
            game.uiFeedback.emit(UiFeedbackKind.ERROR)
            return
        }
        game.cosmeticOwnership.unlockBall(ball.id)
        message = "${ball.name.uppercase()} UNLOCKED • -$price COINS"
        game.assets.cosmetics.ballRegion(ball)?.let { region ->
            rewardFx.trigger(
                kind = RewardFxSystem.Kind.COSMETIC,
                icons = listOf(region),
                centerX = 450f,
                centerY = 820f,
                targetX = if (isLandscape) visibleLeft + visibleWidth - 130f else 760f,
                targetY = visibleBottom + visibleHeight - 105f,
                accent = ForgeUiPalette.success
            )
        }
        game.uiFeedback.emit(UiFeedbackKind.UNLOCK)
    }

    private fun purchase(product: ShopProduct) {
        if (purchaseBusy) return
        if (product !in ShopCatalog.purchasableProducts) {
            message = "This older product is no longer sold in the shop."
            return
        }
        if (game.developmentAccess.enabled) {
            grantPurchase(product, "dev:${product.storeId}:${System.nanoTime()}", "Development purchase complete")
            return
        }
        if (game.monetization.purchaseGateway.price(product) == null) {
            message = if (BuildConfig.DEBUG) "Play product unavailable (debug)." else "Store item unavailable."
            return
        }
        // The store also sells six permanent Epic ball entitlements.
        purchaseBusy = true
        message = "Opening Google Play…"
        game.monetization.purchaseGateway.purchase(product) { result ->
            purchaseBusy = false
            when (result) {
                is PurchaseResult.Success -> grantPurchase(product, result.transactionId, "Purchase complete")

                PurchaseResult.Pending -> message = "Payment pending — your reward arrives after Google Play confirms it."

                PurchaseResult.Cancelled -> {
                    game.ledger.clearPurchaseMode(product.storeId)
                    message = "Purchase cancelled."
                }

                is PurchaseResult.Failed -> {
                    game.ledger.clearPurchaseMode(product.storeId)
                    message = result.message
                }
            }
        }
    }

    private fun displayedPrice(product: ShopProduct): String? = if (game.developmentAccess.enabled) {
        DevelopmentAccess.TEST_PRICE_LABEL
    } else {
        game.monetization.purchaseGateway.price(product)
    }

    /** Credits the same receipt at most once, including after process death and Play reconciliation. */
    private fun grantPurchase(product: ShopProduct, transactionId: String, successPrefix: String) {
        if (game.ledger.hasProcessed(transactionId)) {
            message = "$successPrefix — already restored."
            return
        }
        if (product.ballSpriteName != null) {
            val ball = game.assets.cosmetics.balls.firstOrNull { it.spriteName == product.ballSpriteName }
            if (ball == null) {
                message = "Epic ball artwork is unavailable. Purchase will restore when assets are available."
                return
            }
            game.cosmeticOwnership.unlockBall(ball.id)
            game.ledger.markProcessed(transactionId)
            message = "$successPrefix — ${ball.name.uppercase()} UNLOCKED."
            game.uiFeedback.emit(UiFeedbackKind.UNLOCK)
            return
        }
        if (product.gemAmount > 0) {
            val credited = game.economy.creditPurchasedGems(transactionId, product.gemAmount)
            game.ledger.markProcessed(transactionId)
            message = if (credited) {
                "$successPrefix — +${product.gemAmount} GEMS."
            } else {
                "$successPrefix — Gems already credited."
            }
            if (credited) game.uiFeedback.emit(UiFeedbackKind.PURCHASE)
            return
        }
        // Legacy purchases can still be reconciled, but this screen never sells them.
        val grant = BoosterGrantFactory.createGrant(product, transactionId)
        if (grant.isEmpty()) {
            message = "No valid reward is configured for this product."
            return
        }
        shopInventory.addAll(grant)
        game.ledger.markProcessed(transactionId)
        game.ledger.clearPurchaseMode(product.storeId)
        message = "$successPrefix — +${grant.values.sum()} items."
        game.uiFeedback.emit(UiFeedbackKind.PURCHASE)
    }

    /** ملاحظة صيانة: الدالة `claimReward` هي المكان الوحيد الذي يُمنح فيه عنصر مجاني فعليًا، ويحدث ذلك فقط داخل رد نداء `RewardedAdResult.Earned` القادم من الـ SDK — لا تستدعِ منح المكافأة من أي مكان آخر. */
    private fun claimReward() {
        if (adRequestPending || !shopDailyRewards.canWatch(System.currentTimeMillis())) return
        adRequestPending = true
        message = ""
        game.monetization.rewardedTalismanAdGateway.show { result ->
            if (!adRequestPending) return@show
            adRequestPending = false
            when (result) {
                is RewardedAdResult.Earned -> {
                    // Credits one rolled Orb plus one point after SDK earn, with a durable
                    // receipt in ProgressStore so process death does not duplicate the reward.
                    val stashBefore = game.progress.orbStashCount
                    if (game.progress.creditEarnedOrbPoint(result.rewardToken)) {
                        shopDailyRewards.recordReward(System.currentTimeMillis())
                        game.ledger.markProcessed(result.rewardToken)
                        message = if (game.progress.orbStashCount > stashBefore)
                            "REWARDED: ORB + 1 UPGRADE POINT" else "REWARDED: +1 UPGRADE POINT (BAG FULL)"
                        game.uiFeedback.emit(UiFeedbackKind.PURCHASE)
                    } else {
                        message = "THIS REWARD HAS ALREADY BEEN CLAIMED"
                    }
                    adDialogOpen = false
                }

                RewardedAdResult.ClosedWithoutReward -> message = "Ad closed. No reward earned."

                is RewardedAdResult.Failed -> message = result.message
            }
        }
    }

    /** ملاحظة صيانة: الدالة `goBack` تقرر وجهة الرجوع الصحيحة حسب `returnDestination` (استئناف اللعبة الموقوفة مؤقتًا أو فتح القائمة الرئيسية). */
    private fun goBack() {
        if (returnDestination == ShopReturnDestination.PAUSED_GAME) game.resumePausedGame(showLoading = false) else game.openMenu()
    }
}
