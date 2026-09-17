package com.example.brick_breaker_ball

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.g2d.TextureRegion
import com.badlogic.gdx.math.Rectangle
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.utils.Align
import com.badlogic.gdx.scenes.scene2d.utils.ScissorStack
import kotlin.math.ceil

private enum class ShopTab(val label: String) {
    FEATURED("FEATURED"),
    PADDLES("PADDLES"),
    BALLS("BALLS"),
    ITEMS("ITEMS"),
    FREE("FREE")
}

enum class ShopReturnDestination { MAIN_MENU, PAUSED_GAME }

/**
 * Production-facing store.
 *
 * Design rules:
 * - one clear visual hierarchy instead of a dense prototype grid;
 * - four clearly described paid Item bundles in the visible catalog;
 * - collection previews connect purchases to the actual progression loop;
 * - rewarded ads live in their own Free tab and grant only from the SDK reward callback;
 * - checkout uses Google Play's localized price and stays disabled without ProductDetails.
 *
 * ملاحظات صيانة عامة على هذا التحديث:
 * - المتجر يستخدم Midnight Forge palette المركزية وGradients مولّدة مباشرة، بدون Tint فوق Texture ملونة.
 * - أقسام PADDLES و BALLS و ITEMS أصبحت قابلة للتمرير (سحب باللمس أو بالماوس) وتعرض كل
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
class ShopScreen(game: BrickBreakerGame, private val returnDestination: ShopReturnDestination) : ForgeScreen(game) {

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

        private val FEATURED_PREVIEW_TYPES = listOf(
            PowerUpType.LASER_AUTO_CHARGE,
            PowerUpType.EXTRA_LIFE,
            PowerUpType.EXPAND_PADDLE,
            PowerUpType.FIRE_BALL,
            PowerUpType.MULTIBALL_PLUS_4,
        )

        // أعلى نقطة مشتركة لكل مناطق التمرير الثلاث (PADDLES / BALLS / ITEMS)، بدل قيمة top مختلفة
        // لكل تبويب. الهيدر أعلاه أصبح أقصر تحديدًا ليتيح هذه القيمة مساحة كافية دون تمرير الشاشة.
        private const val PANEL_TOP = 1156f
    }

    private var activeTab = ShopTab.FEATURED
    private var message = ""
    private var purchaseBusy = false

    // Unified Midnight Forge palette. Accent colors are rendered as generated gradients,
    // never multiplied into the pre-colored UI atlas textures.
    private val muted = ForgeUiPalette.muted
    private val primary = ForgeUiPalette.primary
    private val primaryLight = ForgeUiPalette.primaryLight
    private val crimson = ForgeUiPalette.crimson
    private val success = ForgeUiPalette.success
    private val danger = ForgeUiPalette.danger

    // حالة التمرير الخاصة بكل تبويب قابل للتمرير (PADDLES / BALLS / ITEMS)، محفوظة بشكل مستقل
    // بحيث لا يفقد كل قسم موضع تمريره عند التنقل بين التبويبات.
    private val scrollOffsets = mutableMapOf<ShopTab, Float>()
    private var scrollDragActive = false
    private var lastTouchWorldY = 0f
    private val touchWorld = Vector2()
    private var scissorPushed = false

    /**
     * ملاحظة صيانة: الدالة `show` تُستدعى عند فتح شاشة المتجر. تتحقق أولاً من وجود مكافأة معلّقة لم
     * تُعرض بعد (مثلاً بعد رجوع اللاعب من الخلفية أثناء عرض إعلان) قبل تحميل إعلان المكافأة التالي.
     */
    override fun show() {
        installMouseWheelHandler(::handleMouseWheel)
        game.pendingRewards.load()?.let {
            game.setScreen(RewardRevealScreen(game, it, returnDestination))
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
        beginStore()
        drawHeader()
        val tabs = drawTabs()
        val actions = mutableListOf<Pair<Rectangle, () -> Unit>>()

        when (activeTab) {
            ShopTab.FEATURED -> drawFeatured(actions)
            ShopTab.PADDLES -> drawPaddles(actions)
            ShopTab.BALLS -> drawBalls(actions)
            ShopTab.ITEMS -> drawItems(actions)
            ShopTab.FREE -> drawFree(actions)
        }

        if (message.isNotBlank()) drawMessage()
        val back = shopButton("BACK", 250f, 64f, 400f, 92f, primary, .94f)
        end()

        if (Gdx.input.isKeyJustPressed(Input.Keys.BACK)) {
            goBack()
            return
        }

        val tappedTab = tabs.firstOrNull { tapped(it.first) }?.second
        if (tappedTab != null) {
            activeTab = tappedTab
            message = ""
            return
        }
        actions.firstOrNull { tapped(it.first) }?.second?.invoke()
        if (tapped(back)) goBack()
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
        drawFullscreen(game.assets.shopScreenBackground)
        batch.color = Color(.003f, .006f, .012f, .46f)
        drawFullscreenPanel(game.assets.ui.findRegion("panel"))
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
        fittedText(game.assets.titleFont, "SHOP", 55f, 1560f, 790f, .92f)
        game.assets.titleFont.color = Color.WHITE

        // ثلاث خانات متساوية بدل سطر واحد مضغوط (BALLS x/y • PADDLES x/y • ITEMS n).
        val chipGap = SPACE_SM
        val chipHeight = 68f
        val chipY = 1388f
        val chipWidth = (800f - chipGap * 2f) / 3f
        val stats = listOf(
            "BALLS" to "${game.cosmeticProgression.ownedBallCount()}/${game.cosmeticProgression.totalBallCount()}",
            "PADDLES" to "${game.cosmeticProgression.ownedPaddleCount()}/${game.cosmeticProgression.totalPaddleCount()}",
            "ITEMS" to "${game.boosterInventory.total()}",
        )
        stats.forEachIndexed { index, (label, value) ->
            val chipX = 50f + index * (chipWidth + chipGap)
            game.assets.uiRenderer.drawGradientPanel(
                batch,
                Rectangle(chipX, chipY, chipWidth, chipHeight),
                ForgeUiRenderer.GradientStyle.NEUTRAL,
            )
            game.assets.smallFont.color = muted
            fittedText(game.assets.smallFont, label, chipX + SPACE_SM, chipY + chipHeight - 16f, chipWidth - SPACE_SM * 2f, SCALE_CAPTION)
            fittedText(game.assets.hudLabelFont, value, chipX + SPACE_SM, chipY + 28f, chipWidth - SPACE_SM * 2f, SCALE_ITEM_TITLE)
            game.assets.smallFont.color = Color.WHITE
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
        val x0 = 28f
        val width = (844f - gap * 4f) / 5f
        return ShopTab.entries.mapIndexed { index, tab ->
            val x = x0 + index * (width + gap)
            val rect = Rectangle(x, y, width, height)
            game.assets.uiRenderer.drawGradientButton(
                batch,
                rect,
                if (activeTab == tab) ForgeUiRenderer.GradientStyle.PRIMARY else ForgeUiRenderer.GradientStyle.NEUTRAL,
            )
            game.assets.hudLabelFont.color = if (activeTab == tab) ForgeUiPalette.textPrimary else ForgeUiPalette.textSecondary
            // Padding أكبر يسارًا/يمينًا (SPACE_SM لكل جانب) حتى لا تلامس الكلمات حواف الزر.
            fittedText(game.assets.hudLabelFont, tab.label, rect.x + SPACE_SM, rect.y + 63f, rect.width - SPACE_SM * 2f, .76f)
            game.assets.hudLabelFont.color = Color.WHITE
            rect to tab
        }
    }

    /**
     * ملاحظة صيانة: الدالة `drawSectionTitle` ترسم عنوان القسم وسطره الفرعي بنفس الموضع الثابت أعلى
     * كل تبويب. عدّل الإحداثيات هنا فقط إن أردت تغيير مكان كل العناوين دفعة واحدة في كل الأقسام.
     */
    private fun drawSectionTitle(title: String, subtitle: String) {
        game.assets.hudLabelFont.color = primaryLight
        fittedText(game.assets.hudLabelFont, title, 55f, 1240f, 790f, SCALE_SECTION_TITLE)
        game.assets.hudLabelFont.color = Color.WHITE
        game.assets.smallFont.color = muted
        fittedText(game.assets.smallFont, subtitle, 55f, 1200f, 790f, SCALE_BODY)
        game.assets.smallFont.color = Color.WHITE
    }

    // ==================================================================================
    // أدوات التمرير (Scroll) العامة — تُستخدم من قبل PADDLES و BALLS و ITEMS فقط، لأن هذه
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
    private fun maxScrollOffset(contentHeight: Float, panelBounds: Rectangle): Float =
        (contentHeight - panelBounds.height).coerceAtLeast(0f)

    /**
     * ملاحظة صيانة: الدالة `updateScrollDrag` تتابع لمسة المستخدم (لمس شاشة حقيقي أو سحب بالماوس على
     * سطح المكتب) وتُحدّث إزاحة تمرير التبويب المعطى تبعًا لحركة الإصبع/المؤشر. يجب استدعاؤها مرة واحدة
     * فقط في كل إطار، قبل رسم بطاقات القسم مباشرة. تُحوَّل إحداثيات اللمس من شاشة الجهاز إلى عالم
     * اللعبة عبر `viewport.unproject` حتى تعمل بشكل صحيح مهما كان حجم الشاشة الفعلي أو نسبة أبعادها.
     */
    private fun updateScrollDrag(tab: ShopTab, panelBounds: Rectangle, contentHeight: Float) {
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
        if (amountY == 0f || activeTab !in setOf(ShopTab.PADDLES, ShopTab.BALLS, ShopTab.ITEMS)) return false
        touchWorld.set(Gdx.input.x.toFloat(), Gdx.input.y.toFloat())
        viewport.unproject(touchWorld)
        val panelBounds = Rectangle(55f, 325f, 790f, PANEL_TOP - 325f)
        if (!panelBounds.contains(touchWorld.x, touchWorld.y)) return false

        val contentHeight = when (activeTab) {
            ShopTab.PADDLES -> {
                val rows = (game.cosmeticProgression.paddleStyles.size + 1) / 2
                12f + rows * (164f + 24f) - 24f + 12f
            }
            ShopTab.BALLS -> {
                val rows = (game.assets.cosmetics.balls.size + 1) / 2
                12f + rows * (255f + 24f) - 24f + 12f
            }
            ShopTab.ITEMS -> {
                val rows = (SHOP_ELIGIBLE_TYPES.size + 1) / 2
                12f + rows * (218f + 24f) - 24f + 12f
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
            scissors,
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
            ForgeUiRenderer.GradientStyle.NEUTRAL,
        )
        val thumbHeight = (panelBounds.height * (panelBounds.height / (panelBounds.height + maxOffset)))
            .coerceIn(60f, panelBounds.height)
        val progress = (offset / maxOffset).coerceIn(0f, 1f)
        val thumbY = panelBounds.y + (panelBounds.height - thumbHeight) * (1f - progress)
        game.assets.uiRenderer.drawGradientProgress(
            batch,
            Rectangle(trackX, thumbY, 6f, thumbHeight),
            ForgeUiRenderer.GradientStyle.PRIMARY,
        )
    }

    /** Four offers fit in the featured area with a larger hero icon and clear reward text. */
    private fun drawFeatured(actions: MutableList<Pair<Rectangle, () -> Unit>>) {
        drawSectionTitle("FEATURED ITEMS", "Random pulls: each of 14 positive Items has a 1/14 chance; repeats possible.")

        val products = ShopCatalog.products
        products.forEachIndexed { index, product ->
            val y = 940f - index * 224f
            val accent = when (index) {
                0 -> primary
                1 -> crimson
                2 -> primary
                else -> success
            }
            val buy = drawWideProductCard(product, y, accent)
            if (game.developmentAccess.canMakeTestPurchase(
                    game.monetization.purchaseGateway.price(product) != null, purchaseBusy
                )) {
                actions += buy to { purchase(product) }
            }
        }
    }

    /**
     * ملاحظة صيانة: الدالة `drawPaddles` ترسم تبويب "أطقم شكل المضرب" الكامل داخل منطقة قابلة
     * للتمرير، بدل الاقتصار على أول 4 أطقم فقط كما كان سابقًا. البطاقات أكبر الآن (icons ونصوص) لتحسين
     * الوضوح، وزر "عرض كل الأطقم" يبقى ثابتًا أسفل الشاشة لفتح شاشة التخصيص الكاملة (تبديل التجهيز).
     */
    private fun drawPaddles(actions: MutableList<Pair<Rectangle, () -> Unit>>) {
        drawSectionTitle("PADDLE STYLE SETS", "One style equips matching normal, weapon, and sticky forms.")

        val styles = game.cosmeticProgression.paddleStyles
        val cardHeight = 258f
        val cardGap = SPACE_LG
        val rowSpacing = cardHeight + cardGap
        val clipInset = SPACE_SM
        val contentHeight = if (styles.isEmpty()) 0f else styles.size * rowSpacing - cardGap + clipInset * 2f
        val panelBounds = Rectangle(55f, 325f, 790f, PANEL_TOP - 325f)

        updateScrollDrag(ShopTab.PADDLES, panelBounds, contentHeight)
        val offset = scrollOffset(ShopTab.PADDLES)

        beginClippedRegion(panelBounds)
        styles.forEachIndexed { index, style ->
            // clipInset يمنع أول/آخر بطاقة من الظهور "مقصوصة" على حافة منطقة التمرير مباشرة.
            val y = panelBounds.y + panelBounds.height - clipInset - cardHeight - index * rowSpacing + offset
            if (y + cardHeight < panelBounds.y || y > panelBounds.y + panelBounds.height) return@forEachIndexed

            val rect = Rectangle(panelBounds.x, y, panelBounds.width - 20f, cardHeight)
            val owned = game.cosmeticProgression.ownsPaddle(style)
            val equipped = game.progress.settings.selectedPaddleId == style.normal.id
            framedPanel(
                rect,
                when {
                    equipped -> success
                    owned -> primary
                    else -> ForgeUiPalette.neutralBorder
                }
            )

            val definitions = listOf(style.normal, style.weapon, style.sticky)
            definitions.forEachIndexed { formIndex, definition ->
                val px = rect.x + 24f + formIndex * 261f
                batch.color = Color.WHITE
                game.assets.cosmetics.paddleRegion(definition)?.let { drawFit(it, px, rect.y + 98f, 220f, 104f) }
                if (!owned) drawLockBadge(px + 175f, rect.y + 148f, 42f)
                batch.color = Color.WHITE
                game.assets.smallFont.color = muted
                fittedText(game.assets.smallFont, listOf("NORMAL", "WEAPON", "STICKY")[formIndex], px, rect.y + 76f, 220f, SCALE_CAPTION)
                game.assets.smallFont.color = Color.WHITE
            }

            fittedText(game.assets.hudLabelFont, style.displayName.uppercase(), rect.x + 25f, rect.y + 230f, 485f, SCALE_ITEM_TITLE)
            val ability = PaddleAbilityCatalog.profileForNormalPaddle(style.normal.id)
            game.assets.smallFont.color = primaryLight
            fittedText(game.assets.smallFont, ability.title, rect.x + 25f, rect.y + 202f, 485f, SCALE_CAPTION)
            game.assets.smallFont.color = Color.WHITE
            val state = when {
                equipped -> "EQUIPPED"
                owned -> "OWNED"
                else -> game.cosmeticProgression.paddleRequirement(style)
            }
            game.assets.smallFont.color = when {
                equipped -> success
                owned -> success
                else -> primaryLight
            }
            fittedText(game.assets.smallFont, state, rect.x + 520f, rect.y + 222f, 235f, SCALE_BODY)
            game.assets.smallFont.color = Color.WHITE
        }
        endClippedRegion()
        drawScrollTrack(panelBounds, offset, maxScrollOffset(contentHeight, panelBounds))

        val open = shopButton("VIEW ALL ${styles.size} STYLE SETS", 185f, 205f, 530f, 96f, primary, .88f)
        actions += open to { openCustomization() }
    }

    /** Draws every ball as an individual unlock; there are no ball themes/groups anymore. */
    private fun drawBalls(actions: MutableList<Pair<Rectangle, () -> Unit>>) {
        val balls = game.assets.cosmetics.balls
        val ownedCount = game.cosmeticProgression.ownedBallCount()
        val nextUnlock = game.cosmeticProgression.nextLockedBall()
        val nextText = nextUnlock?.let { "Next unlock: Stage ${it.second}" } ?: "All balls unlocked"
        drawSectionTitle("BALL COLLECTION", "$ownedCount/${balls.size} balls unlocked. $nextText.")

        val cardWidth = 375f
        val cardHeight = 255f
        val colGap = SPACE_MD
        val rowGap = SPACE_LG
        val colOffset = cardWidth + colGap
        val rows = if (balls.isEmpty()) 0 else ceil(balls.size / 2.0).toInt()
        val rowSpacing = cardHeight + rowGap
        val clipInset = SPACE_SM
        val contentHeight = if (rows == 0) 0f else rows * rowSpacing - rowGap + clipInset * 2f
        val panelBounds = Rectangle(55f, 325f, 790f, PANEL_TOP - 325f)

        updateScrollDrag(ShopTab.BALLS, panelBounds, contentHeight)
        val offset = scrollOffset(ShopTab.BALLS)

        beginClippedRegion(panelBounds)
        balls.forEachIndexed { index, definition ->
            val col = index % 2
            val row = index / 2
            val y = panelBounds.y + panelBounds.height - clipInset - cardHeight - row * rowSpacing + offset
            if (y + cardHeight < panelBounds.y || y > panelBounds.y + panelBounds.height) return@forEachIndexed

            val rect = Rectangle(panelBounds.x + col * colOffset, y, cardWidth, cardHeight)
            val owned = game.developmentAccess.canUseCosmetic(game.cosmeticProgression.ownsBall(definition))
            val equipped = owned && game.progress.settings.selectedBallGroupName == definition.groupName &&
                game.progress.settings.selectedBallSpriteName == definition.spriteName
            framedPanel(rect, if (equipped) primary else ForgeUiPalette.neutralBorder)

            fittedText(
                game.assets.hudLabelFont,
                definition.name.uppercase(),
                rect.x + 18f,
                rect.y + 225f,
                rect.width - 36f,
                SCALE_ITEM_TITLE
            )

            val previewX = rect.x + (rect.width - 112f) / 2f
            game.assets.cosmetics.ballRegion(definition)?.let {
                drawFit(it, previewX, rect.y + 82f, 112f, 112f)
            }
            if (!owned) drawLockBadge(previewX + 66f, rect.y + 148f, 46f)
            game.assets.smallFont.color = when {
                equipped -> success
                owned -> muted
                else -> primaryLight
            }
            fittedText(
                game.assets.smallFont,
                when {
                    equipped -> "EQUIPPED"
                    owned -> "UNLOCKED"
                    else -> game.cosmeticProgression.ballRequirement(definition)
                },
                rect.x + 18f,
                rect.y + 53f,
                rect.width - 36f,
                SCALE_BODY
            )
            game.assets.smallFont.color = Color.WHITE
        }
        endClippedRegion()
        drawScrollTrack(panelBounds, offset, maxScrollOffset(contentHeight, panelBounds))

        val open = shopButton("OPEN BALL COLLECTION", 185f, 205f, 530f, 96f, primary, .88f)
        actions += open to { openCustomization() }
    }

    /**
     * ملاحظة صيانة: الدالة `drawItems` ترسم كل عناصر المتجر الإيجابية داخل منطقة قابلة للتمرير بعمودين،
     * بدل عرض أول 6 فقط كما في النسخة القديمة. زرا الشراء الرئيسيان (FULL ARSENAL / FORGE VAULT) ثابتان
     * أسفل منطقة التمرير ولا يتحركان معها.
     */
    private fun drawItems(actions: MutableList<Pair<Rectangle, () -> Unit>>) {
        val types = SHOP_ELIGIBLE_TYPES
        drawSectionTitle("POSITIVE ITEMS", "${types.size} player items. Hazards and special drops are never sold here.")

        val cardWidth = 375f
        // كانت 230؛ +20% تقريبًا حتى يتنفس الوصف بدل الانحشار في سطرين.
        val cardHeight = 278f
        val colGap = SPACE_LG
        val rowGap = SPACE_LG
        val colOffset = cardWidth + colGap
        val rows = if (types.isEmpty()) 0 else ceil(types.size / 2.0).toInt()
        val rowSpacing = cardHeight + rowGap
        val clipInset = SPACE_SM
        val contentHeight = if (rows == 0) 0f else rows * rowSpacing - rowGap + clipInset * 2f
        val panelBounds = Rectangle(55f, 345f, 790f, PANEL_TOP - 345f)

        updateScrollDrag(ShopTab.ITEMS, panelBounds, contentHeight)
        val offset = scrollOffset(ShopTab.ITEMS)

        beginClippedRegion(panelBounds)
        types.forEachIndexed { index, type ->
            val col = index % 2
            val row = index / 2
            val y = panelBounds.y + panelBounds.height - clipInset - cardHeight - row * rowSpacing + offset
            if (y + cardHeight < panelBounds.y || y > panelBounds.y + panelBounds.height) return@forEachIndexed

            val rect = Rectangle(panelBounds.x + col * colOffset, y, cardWidth, cardHeight)
            framedPanel(rect, ForgeUiPalette.neutralBorder)

            // قالب ثابت لكل بطاقة: Icon container 64×64 → Title → Badge OWNED بجانبه مباشرة → Description
            // بعرض كامل البطاقة. نفس الـpadding (SPACE_MD) في كل بطاقة، بدل محاذاة مختلفة لكل عنصر.
            val iconBox = 64f
            val iconX = rect.x + SPACE_MD
            val iconY = rect.y + rect.height - SPACE_MD - iconBox
            game.assets.uiRenderer.drawGradientPanel(
                batch,
                Rectangle(iconX, iconY, iconBox, iconBox),
                ForgeUiRenderer.GradientStyle.NEUTRAL,
            )
            // container ثابت الحجم + drawFit بدل batch.draw المباشر: هذا وحده يوحّد الحجم البصري
            // للأيقونات حتى لو كانت نسبة العرض للارتفاع مختلفة بين ملفات الصور الأصلية.
            drawFit(game.assets.gameplayAtlas.powerUpIcon(type), iconX + 6f, iconY + 6f, iconBox - 12f, iconBox - 12f)

            val info = PowerUpInfoRepository.info(type)
            val owned = game.boosterInventory.count(type)

            val badgeWidth = 92f
            val badgeHeight = 34f
            val badgeRect = Rectangle(rect.x + rect.width - SPACE_MD - badgeWidth, iconY + iconBox - badgeHeight, badgeWidth, badgeHeight)
            game.assets.uiRenderer.drawGradientPanel(
                batch,
                badgeRect,
                if (owned > 0) ForgeUiRenderer.GradientStyle.SUCCESS else ForgeUiRenderer.GradientStyle.NEUTRAL,
            )
            game.assets.smallFont.color = if (owned > 0) Color.WHITE else muted
            fittedText(game.assets.smallFont, "OWNED x$owned", badgeRect.x + SPACE_XS, badgeRect.y + badgeHeight - 10f, badgeWidth - SPACE_XS * 2f, SCALE_CAPTION)
            game.assets.smallFont.color = Color.WHITE

            val titleX = iconX + iconBox + SPACE_MD
            val titleWidth = (badgeRect.x - SPACE_SM - titleX).coerceAtLeast(60f)
            fittedText(game.assets.hudLabelFont, info.shortName, titleX, iconY + iconBox - 14f, titleWidth, SCALE_ITEM_TITLE)

            game.assets.smallFont.color = muted
            wrappedText(
                game.assets.smallFont,
                info.description,
                rect.x + SPACE_MD,
                iconY - SPACE_SM,
                rect.width - SPACE_MD * 2f,
                SCALE_BODY,
            )
            game.assets.smallFont.color = Color.WHITE
        }
        endClippedRegion()
        drawScrollTrack(panelBounds, offset, maxScrollOffset(contentHeight, panelBounds))

        game.assets.smallFont.color = muted
        fittedText(
            game.assets.smallFont,
            "Tip: the in-game inventory panel shows the full item guide with large scrollable cards.",
            70f,
            305f,
            760f,
            SCALE_CAPTION,
        )
        game.assets.smallFont.color = Color.WHITE

        val first = ShopCatalog.products.firstOrNull { it.id == ShopProductId.FIVE_OF_EACH }
        val second = ShopCatalog.products.firstOrNull { it.id == ShopProductId.MIXED_150 }
        if (first != null) {
            val r = compactProductButton(first, 75f, 180f, 355f, crimson)
            if (game.developmentAccess.canMakeTestPurchase(
                    game.monetization.purchaseGateway.price(first) != null, purchaseBusy
                )) actions += r to { purchase(first) }
        }
        if (second != null) {
            val r = compactProductButton(second, 470f, 180f, 355f, primary)
            if (game.developmentAccess.canMakeTestPurchase(
                    game.monetization.purchaseGateway.price(second) != null, purchaseBusy
                )) actions += r to { purchase(second) }
        }
    }

    /**
     * ملاحظة صيانة: الدالة `drawFree` ترسم تبويب "مجاني" (الإعلان المكافئ اليومي) وشرح آلية عمله
     * أسفله؛ لا يحتاج تمريرًا لأن المحتوى ثابت الحجم دائمًا (بطاقة واحدة + فقرة شرح واحدة).
     */
    private fun drawFree(actions: MutableList<Pair<Rectangle, () -> Unit>>) {
        drawSectionTitle("FREE REWARDED ITEM", "Watch one short video to earn a reward.")

        // كل المحتوى هنا ثابت الحجم دائمًا (بطاقة + شرح)، لكن الشاشة أطول منه بكثير. بدل تثبيت البطاقة
        // أعلى الشاشة وترك فراغ ضخم واحد قبل BACK (المشكلة الأصلية)، نحسب الفراغ الكلي المتاح ونوزّعه
        // بالتساوي أعلى وأسفل الكتلة، فتصبح المسافات متوازنة بدل فراغ مقصود بمكان واحد.
        val rewardHeight = 430f
        val explainerHeight = 220f
        val blockGap = SPACE_LG
        val topBound = 1200f - SPACE_XL
        val bottomBound = 156f + SPACE_XL
        val contentHeight = rewardHeight + blockGap + explainerHeight
        val extraSpace = (topBound - bottomBound - contentHeight).coerceAtLeast(0f)
        val rewardTop = topBound - extraSpace / 2f
        val rewardY = rewardTop - rewardHeight
        val explainerY = rewardY - blockGap - explainerHeight

        val button = drawFreeRewardCard(55f, rewardY, 790f, rewardHeight)
        if (game.dailyRewards.canWatch(System.currentTimeMillis()) &&
            game.monetization.rewardedTalismanAdGateway.state in setOf(AdState.READY, AdState.UNAVAILABLE)
        ) {
            actions += button to { handleRewardTap() }
        }

        val explainer = Rectangle(80f, explainerY, 740f, explainerHeight)
        framedPanel(explainer, ForgeUiPalette.neutralBorder)
        fittedText(game.assets.hudLabelFont, "HOW IT WORKS", explainer.x + SPACE_LG, explainer.y + explainer.height - 42f, 680f, SCALE_ITEM_TITLE)

        // ثلاث خطوات قصيرة بحالة أحرف عادية بدل خمسة أسطر Uppercase تقنية.
        val steps = listOf("1. Watch the ad", "2. Finish the video", "3. Get your reward")
        game.assets.smallFont.color = muted
        steps.forEachIndexed { index, step ->
            fittedText(
                game.assets.smallFont,
                step,
                explainer.x + SPACE_LG,
                explainer.y + explainer.height - 92f - index * 40f,
                680f,
                SCALE_BODY,
            )
        }
        game.assets.smallFont.color = Color.WHITE

        // تفاصيل الـcallback التقنية لا تخص اللاعب؛ تظهر فقط في بناء تصحيح الأخطاء، وأسفل اللوحة تمامًا
        // حتى لا تغيّر ارتفاع اللوحة نفسها بين بناء الإنتاج وبناء التصحيح.
        if (BuildConfig.DEBUG) {
            game.assets.smallFont.color = danger
            fittedText(
                game.assets.smallFont,
                "Debug: reward unit = ${BuildConfig.REWARDED_TALISMAN_AD_UNIT.ifBlank { "none" }}",
                explainer.x + SPACE_LG,
                explainer.y - SPACE_MD,
                680f,
                SCALE_CAPTION,
            )
            game.assets.smallFont.color = Color.WHITE
        }
    }

    /**
     * ملاحظة صيانة: الدالة `drawCollectionProgress` ترسم شريط تقدّم عام لمجموع الكرات + المضارب
     * المملوكة. غير مستدعاة حاليًا من `render` — إن أردت تفعيلها اربطها من أحد التبويبات، أو احذفها
     * لاحقًا إن لم تعد مطلوبة لتفادي الاحتفاظ بكود غير مستخدم.
     */
    private fun drawCollectionProgress(x: Float, y: Float, width: Float) {
        val total = game.cosmeticProgression.totalBallCount() + game.cosmeticProgression.totalPaddleCount()
        val owned = game.cosmeticProgression.ownedBallCount() + game.cosmeticProgression.ownedPaddleCount()
        val progress = if (total == 0) 0f else owned.toFloat() / total
        game.assets.uiRenderer.drawGradientProgress(
            batch,
            Rectangle(x, y, width, 28f),
            ForgeUiRenderer.GradientStyle.NEUTRAL,
        )
        game.assets.uiRenderer.drawGradientProgress(
            batch,
            Rectangle(x + 3f, y + 3f, (width - 6f) * progress, 22f),
            ForgeUiRenderer.GradientStyle.SUCCESS,
        )
        fittedText(game.assets.smallFont, "$owned / $total COSMETICS", x, y + 52f, width, .56f)
    }

    /** Draws the actual Item art and Play purchase state for one featured offer. */
    private fun drawWideProductCard(product: ShopProduct, y: Float, accent: Color): Rectangle {
        val rect = Rectangle(55f, y, 790f, 205f)
        framedPanel(rect, accent)

        val heroType = when (product.id) {
            ShopProductId.RANDOM_20 -> PowerUpType.RANDOM_GOOD
            ShopProductId.RANDOM_50 -> PowerUpType.MULTIBALL_15
            ShopProductId.FIVE_OF_EACH -> PowerUpType.EXTRA_LIFE
            ShopProductId.MIXED_150 -> PowerUpType.FIRE_BALL
            else -> PowerUpType.RANDOM_GOOD
        }
        val hero = Rectangle(rect.x + 20f, rect.y + 43f, 122f, 122f)
        game.assets.uiRenderer.drawGradientPanel(batch, hero, ForgeUiRenderer.GradientStyle.NEUTRAL)
        drawFit(game.assets.gameplayAtlas.powerUpIcon(heroType), hero.x + 7f, hero.y + 7f, 108f, 108f)

        val detailX = rect.x + 162f
        fittedText(game.assets.hudLabelFont, product.title, detailX, rect.y + 170f, 392f, .92f)
        game.assets.smallFont.color = muted
        fittedText(game.assets.smallFont, product.subtitle, detailX, rect.y + 140f, 392f, .73f)
        game.assets.smallFont.color = Color.WHITE

        FEATURED_PREVIEW_TYPES.forEachIndexed { index, type ->
            val iconX = detailX + index * 72f
            val iconY = rect.y + 55f
            game.assets.uiRenderer.drawGradientPanel(
                batch, Rectangle(iconX, iconY, 66f, 66f), ForgeUiRenderer.GradientStyle.NEUTRAL,
            )
            drawFit(game.assets.gameplayAtlas.powerUpIcon(type), iconX + 3f, iconY + 3f, 60f, 60f)
        }
        game.assets.smallFont.color = primaryLight
        val detail = when (product.grant) {
            is BoosterGrant.RandomTotal -> "RANDOM: 1/14 PER TYPE • REPEATS POSSIBLE"
            is BoosterGrant.EachType -> "ALL 14 POSITIVE TYPES GUARANTEED"
            is BoosterGrant.EachTypePlusRandom -> "10 EACH + 10 RANDOM: 1/14 PER TYPE"
        }
        fittedText(game.assets.smallFont, detail, detailX, rect.y + 34f, 390f, .62f)
        game.assets.smallFont.color = Color.WHITE

        val localizedPrice = displayedPrice(product)
        val state = shopProductUiState(localizedPrice, purchaseBusy)
        game.assets.smallFont.color = if (localizedPrice == null) muted else primaryLight
        val priceNote = when {
            game.developmentAccess.enabled -> "TEST GRANT"
            localizedPrice == null -> "TARGET USD ${product.targetUsdPrice}"
            else -> "GOOGLE PLAY PRICE"
        }
        fittedText(game.assets.smallFont, priceNote, rect.x + 570f, rect.y + 155f, 193f, .64f)
        game.assets.smallFont.color = Color.WHITE
        return shopButton(
            if (localizedPrice == null) "UNAVAILABLE" else state.buttonLabel,
            rect.x + 570f,
            rect.y + 50f,
            193f,
            83f,
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
        val title = if (price != null) "${product.title} • $price" else "${product.title} • UNAVAILABLE"
        return shopButton(title, x, y, w, 94f, if (price != null) accent else ForgeUiPalette.disabled, .66f)
    }

    /**
     * ملاحظة صيانة: الدالة `drawFreeRewardCard` ترسم بطاقة الإعلان المكافئ اليومي وتعيد مستطيل الزر
     * فقط؛ حالة الزر (نصه وتفعيله) تعتمد بالكامل على `DailyRewardStore` و`RewardedAdGateway.state` ولا
     * تُمنح أي مكافأة من هذه الدالة مباشرة — المنح يحدث فقط داخل `claimReward`.
     */
    private fun drawFreeRewardCard(x: Float, y: Float, w: Float, h: Float): Rectangle {
        val now = System.currentTimeMillis()
        val daily = game.dailyRewards.currentState(now)
        val cooldown = game.dailyRewards.remainingCooldownMillis(now)
        val adState = game.monetization.rewardedTalismanAdGateway.state
        framedPanel(Rectangle(x, y, w, h), primary)

        fittedText(game.assets.titleFont, "WATCH • EARN • KEEP PLAYING", x + 35f, y + h - 52f, w - 70f, .70f)
        game.assets.smallFont.color = muted
        fittedText(
            game.assets.smallFont,
            "Complete one rewarded video to receive one positive talisman.",
            x + 38f,
            y + h - 105f,
            w - 76f,
            SCALE_BODY,
        )
        game.assets.smallFont.color = Color.WHITE

        SHOP_ELIGIBLE_TYPES.take(7).forEachIndexed { index, type ->
            batch.color = Color.WHITE
            batch.draw(game.assets.gameplayAtlas.powerUpIcon(type), x + 90f + index * 94f, y + 200f, 70f, 70f)
        }

        // "لا يوجد إعلانات متاحة" معلومة تهم اللاعب فتظهر دائمًا؛ أما تفاصيل test-unit/production فهي
        // شأن تصحيح أخطاء فقط، ولا تظهر إطلاقًا خارج بناء DEBUG.
        when {
            BuildConfig.REWARDED_TALISMAN_AD_UNIT.isBlank() -> {
                game.assets.smallFont.color = danger
                fittedText(game.assets.smallFont, "Rewarded videos are currently unavailable.", x + 45f, y + 175f, w - 90f, SCALE_CAPTION)
                game.assets.smallFont.color = Color.WHITE
            }

            BuildConfig.DEBUG -> {
                val adMode = if (BuildConfig.REWARDED_TALISMAN_AD_UNIT.contains("3940256099942544")) {
                    "Debug build — Google test ad unit"
                } else {
                    "Debug build — production ad unit"
                }
                game.assets.smallFont.color = primaryLight
                fittedText(game.assets.smallFont, adMode, x + 45f, y + 175f, w - 90f, SCALE_CAPTION)
                game.assets.smallFont.color = Color.WHITE
            }
        }
        val label = when {
            daily.claimedToday >= daily.dailyLimit -> "DAILY COMPLETE"

            cooldown > 0L -> {
                val seconds = ceil(cooldown / 1000.0).toLong()
                "NEXT %02d:%02d".format(seconds / 60, seconds % 60)
            }

            adState == AdState.READY -> "WATCH AD  •  GET +1"

            adState == AdState.LOADING -> "LOADING GOOGLE AD…"

            adState == AdState.SHOWING -> "AD PLAYING…"

            adState == AdState.UNAVAILABLE -> "RETRY GOOGLE AD"

            else -> "CURRENTLY UNAVAILABLE"
        }
        val enabled = daily.claimedToday < daily.dailyLimit && cooldown <= 0L &&
            adState in setOf(AdState.READY, AdState.UNAVAILABLE)

        // زر الإعلان له سطران: الحالة/الإجراء في الأعلى، وعداد المكافآت اليومية تحته داخل الزر نفسه.
        val buttonX = x + 150f
        val buttonY = y + 42f
        val buttonW = w - 300f
        val buttonH = 92f
        val buttonRect = Rectangle(buttonX, buttonY, buttonW, buttonH)
        val buttonTint = if (enabled) primary else ForgeUiPalette.disabled

        game.assets.uiRenderer.drawGradientButton(batch, buttonRect, gradientStyleFor(buttonTint))

        game.assets.buttonFont.color = ForgeUiPalette.textPrimary
        fittedText(
            game.assets.buttonFont,
            label,
            buttonX + 10f,
            buttonY + 68f,
            buttonW - 20f,
            .64f,
        )
        game.assets.buttonFont.color = Color.WHITE

        game.assets.smallFont.color = if (enabled) ForgeUiPalette.textSecondary else muted
        fittedText(
            game.assets.smallFont,
            "${daily.claimedToday}/${daily.dailyLimit} FREE REWARDS TODAY",
            buttonX + 12f,
            buttonY + 28f,
            buttonW - 24f,
            .50f,
        )
        game.assets.smallFont.color = Color.WHITE

        return buttonRect
    }

    /**
     * ملاحظة صيانة: الدالة `framedPanel` ترسم الإطار القياسي المستخدم في كل بطاقات المتجر (توهج خفيف
     * من الخلف + لون بارز من الخارج + سطح داكن من الداخل). أي تعديل هنا ينعكس فورًا على كل بطاقات
     * الشوب دفعة واحدة، فكن حذرًا عند تغييره.
     */
    private fun framedPanel(rect: Rectangle, accent: Color) {
        game.assets.uiRenderer.drawGradientBorderPanel(batch, rect, gradientStyleFor(accent), 5f)
    }

    /**
     * ملاحظة صيانة: الدالة `drawMessage` ترسم شريط الرسالة السفلي (نتائج الشراء أو الإعلان)؛ تُستدعى
     * فقط من `render` عندما تكون `message` غير فارغة.
     */
    private fun drawMessage() {
        batch.color = Color(.025f, .055f, .09f, .98f)
        batch.draw(game.assets.ui.findRegion("panel"), 70f, 175f, 760f, 70f)
        batch.color = Color.WHITE
        fittedText(game.assets.smallFont, message, 90f, 219f, 720f, .68f)
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

    /**
     * ملاحظة صيانة: الدالة `shopButton` ترسم زرًا موحّد الشكل مع نص يتكيّف مع عرضه، وتعيد مستطيله
     * ليُفحص لاحقًا عند اللمس؛ لا تحتوي على أي منطق نقر بحد ذاتها.
     */
    private fun shopButton(text: String, x: Float, y: Float, w: Float, h: Float, tint: Color, scale: Float = .82f): Rectangle {
        val rect = Rectangle(x, y, w, h)
        game.assets.uiRenderer.drawGradientButton(batch, rect, gradientStyleFor(tint))
        game.assets.buttonFont.color = ForgeUiPalette.textPrimary
        fittedText(game.assets.buttonFont, text, x + 8f, y + h * .67f, w - 16f, scale)
        game.assets.buttonFont.color = Color.WHITE
        return rect
    }

    private fun gradientStyleFor(color: Color): ForgeUiRenderer.GradientStyle = when {
        color === ForgeUiPalette.disabled || color.a < .72f -> ForgeUiRenderer.GradientStyle.DISABLED
        color == success || color == ForgeUiPalette.successLight || color == ForgeUiPalette.successDark -> ForgeUiRenderer.GradientStyle.SUCCESS
        color == crimson || color == ForgeUiPalette.crimsonLight || color == ForgeUiPalette.crimsonDark || color == danger -> ForgeUiRenderer.GradientStyle.CRIMSON
        color == primary || color == primaryLight || color == ForgeUiPalette.primaryDark -> ForgeUiRenderer.GradientStyle.PRIMARY
        else -> ForgeUiRenderer.GradientStyle.NEUTRAL
    }

    private fun wrappedText(
        font: com.badlogic.gdx.graphics.g2d.BitmapFont,
        text: String,
        x: Float,
        y: Float,
        width: Float,
        scale: Float,
    ) {
        val oldX = font.data.scaleX
        val oldY = font.data.scaleY
        font.data.setScale(scale)
        font.draw(batch, text, x, y, width, Align.left, true)
        font.data.setScale(oldX, oldY)
    }

    /** ملاحظة صيانة: الدالة `openCustomization` تفتح شاشة التخصيص الكاملة، وتُخبرها إن كانت اللعبة متوقفة مؤقتًا حتى تعرف طريق الرجوع الصحيح. */
    private fun openCustomization() {
        game.setScreen(CustomizationScreen(game, returnToPausedGame = returnDestination == ShopReturnDestination.PAUSED_GAME))
    }

    /** ملاحظة صيانة: الدالة `handleRewardTap` تُستدعى فقط عند لمس زر الإعلان المكافئ؛ تختار الإجراء المناسب (عرض الإعلان/إعادة تحميله/رسالة انتظار) حسب حالة `RewardedAdGateway` الحالية دون منح أي مكافأة هنا. */
    private fun handleRewardTap() {
        val now = System.currentTimeMillis()
        if (!game.dailyRewards.canWatch(now)) return
        when (game.monetization.rewardedTalismanAdGateway.state) {
            AdState.READY -> claimReward()

            AdState.UNAVAILABLE -> {
                message = "Retrying the rewarded ad…"
                game.monetization.rewardedTalismanAdGateway.preload()
            }

            AdState.LOADING -> message = "Rewarded video is loading…"

            AdState.SHOWING -> message = "Finish the video to earn your talisman."

            AdState.DISABLED -> message = if (BuildConfig.DEBUG) "Rewarded ad unavailable (debug)." else "Rewarded ads are currently unavailable."
        }
    }

    /** ملاحظة صيانة: الدالة `purchase` تبدأ تدفق شراء حقيقي عبر `PurchaseGateway`؛ ترفض أي منتج ليس ضمن `ShopCatalog.products` (أي منتج قديم/legacy) قبل فتح Google Play. */
    private fun purchase(product: ShopProduct) {
        if (purchaseBusy) return
        if (product !in ShopCatalog.products) {
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
        purchaseBusy = true
        message = "Opening Google Play…"
        game.monetization.purchaseGateway.purchase(product) { result ->
            purchaseBusy = false
            when (result) {
                is PurchaseResult.Success -> grantPurchase(product, result.transactionId, "Purchase complete")
                PurchaseResult.Pending -> message = "Payment pending — your reward arrives after Google Play confirms it."
                PurchaseResult.Cancelled -> message = "Purchase cancelled."
                is PurchaseResult.Failed -> message = result.message
            }
        }
    }

    private fun displayedPrice(product: ShopProduct): String? =
        if (game.developmentAccess.enabled) DevelopmentAccess.TEST_PRICE_LABEL
        else game.monetization.purchaseGateway.price(product)

    /** ملاحظة صيانة: الدالة `grantPurchase` تمنح مكونات الشراء فعليًا عبر `BoosterInventoryStore`، وتتحقق أولاً من `TransactionLedger.hasProcessed` لمنع منح نفس عملية الشراء مرتين (مهم عند استعادة عمليات شراء سابقة). */
    private fun grantPurchase(product: ShopProduct, transactionId: String, successPrefix: String) {
        if (game.ledger.hasProcessed(transactionId)) {
            message = "$successPrefix — already restored."
            return
        }
        val grant = BoosterGrantFactory.createGrant(product, transactionId)
        game.boosterInventory.addAll(grant)
        game.ledger.markProcessed(transactionId)
        message = "$successPrefix — +${grant.values.sum()} items."
    }

    /** ملاحظة صيانة: الدالة `claimReward` هي المكان الوحيد الذي يُمنح فيه عنصر مجاني فعليًا، ويحدث ذلك فقط داخل رد نداء `RewardedAdResult.Earned` القادم من الـ SDK — لا تستدعِ منح المكافأة من أي مكان آخر. */
    private fun claimReward() {
        message = ""
        game.monetization.rewardedTalismanAdGateway.show { result ->
            when (result) {
                is RewardedAdResult.Earned -> {
                    // الإعلان المجاني يمنح فقط العناصر الإيجابية المعروضة في SHOP_ELIGIBLE_TYPES.
                    // نستخدم rewardToken لاختيار عنصر بشكل ثابت، مع إبقاء Ledger مسؤولاً عن منع التكرار.
                    if (SHOP_ELIGIBLE_TYPES.isEmpty()) {
                        message = "No positive rewarded items are configured."
                        return@show
                    }
                    val rewardIndex = (result.rewardToken.hashCode() and Int.MAX_VALUE) % SHOP_ELIGIBLE_TYPES.size
                    val type = SHOP_ELIGIBLE_TYPES[rewardIndex]
                    if (!game.ledger.hasProcessed(result.rewardToken)) {
                        game.boosterInventory.add(type, 1)
                        game.dailyRewards.recordReward(System.currentTimeMillis())
                        game.ledger.markProcessed(result.rewardToken)
                    }
                    val pending = PendingReward(result.rewardToken, type, game.boosterInventory.count(type))
                    game.pendingRewards.save(pending)
                    game.setScreen(RewardRevealScreen(game, pending, returnDestination))
                }

                RewardedAdResult.ClosedWithoutReward -> message = "Finish the video to receive the talisman."

                is RewardedAdResult.Failed -> message = result.message
            }
        }
    }

    /** ملاحظة صيانة: الدالة `goBack` تقرر وجهة الرجوع الصحيحة حسب `returnDestination` (استئناف اللعبة الموقوفة مؤقتًا أو فتح القائمة الرئيسية). */
    private fun goBack() {
        if (returnDestination == ShopReturnDestination.PAUSED_GAME) game.resumePausedGame() else game.openMenu()
    }
}
