# Brick Breaker Ball

<p align="center">
  <img src="logo.png" alt="شعار Brick Breaker Ball" width="350">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-Android-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/LibGDX-1.14.2-E74A45" alt="LibGDX 1.14.2">
  <img src="https://img.shields.io/badge/Android-minSdk%2024-3DDC84?logo=android&logoColor=white" alt="Android minSdk 24">
  <img src="https://img.shields.io/badge/Target%20SDK-36-3DDC84?logo=android&logoColor=white" alt="Target SDK 36">
  <img src="https://img.shields.io/badge/Gradle-Kotlin%20DSL-02303A?logo=gradle&logoColor=white" alt="Gradle Kotlin DSL">
  <img src="https://img.shields.io/badge/AdMob-Mobile%20Ads%2025.4.0-EA4335?logo=googleads&logoColor=white" alt="Google Mobile Ads 25.4.0">
  <img src="https://img.shields.io/badge/UMP-4.0.0-4285F4?logo=google&logoColor=white" alt="Google UMP 4.0.0">
  <img src="https://img.shields.io/badge/Google%20Play-Billing%209.1.0-414141?logo=googleplay&logoColor=white" alt="Google Play Billing 9.1.0">
  <img src="https://img.shields.io/badge/Media3-1.5.1-3F51B5" alt="AndroidX Media3 1.5.1">
  <img src="https://img.shields.io/badge/R8-Full%20Mode-5C2D91" alt="R8 Full Mode">
  <img src="https://img.shields.io/badge/Spotless-8.10.2-0F9D58" alt="Spotless 8.10.2">
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Release-1.0.1%20Alpha-0A84FF" alt="Release 1.0.1 Alpha">
  <img src="https://img.shields.io/badge/versionCode-2-0A84FF" alt="versionCode 2">
  <img src="https://img.shields.io/badge/Google%20Play-Closed%20Testing%20Published-34A853?logo=googleplay&logoColor=white" alt="Google Play closed testing published">
  <img src="https://img.shields.io/badge/16%20KB%20Page%20Size-Compatible-34A853" alt="16 KB page-size compatible">
</p>

## **ForgePulse Games**

[English](README.md) | **العربية**

Brick Breaker Ball لعبة كسر طوب مستقبلية على Android مكتوبة بـKotlin وتُرسم بواسطة LibGDX. هذه النسخة من README تمت إعادة بنائها بعد قراءة **الموديول الفعلي `app(4).zip`** وليست اعتمادًا على ملخص قديم للمشروع.

الموديول الحالي يحتوي **397 ملفًا**، منها **63 ملف Kotlin** و**266 PNG** و**14 فيديو MP4** للعوالم/Splash و**10 ملفات JSON**، إضافة إلى مكتبات libGDX الأصلية لثلاث معماريات وصوتيات وشيدرز وخطوط وموارد Android.

<p>

## <img src="yt-logo.png" alt="youtube logo" width="50"> ---> [مشاهدة فيديو Brick Breaker Ball](https://www.youtube.com/watch?v=xFAEdzZf7L4) <img src="yt-like.png" alt="youtube like" width="120"> <img src="yt-share.png" alt="youtube share" width="120">
</p> 

## حالة الإصدار الحالية

| الجزء | الحالة الحالية |
|---|---|
| Google Play | **الاختبار المغلق Alpha 1.0.1 منشور لمسار المختبرين**؛ النشر العام Production ما زال بانتظار إكمال متطلبات الاختبار المغلق/الوصول للإنتاج |
| الإصدار | `versionName 1.0.1` / `versionCode 2` |
| Application ID | خاصية الإنتاج افتراضيًا `com.forgepulse.brickbreakerball` |
| SDK | `minSdk 24` و`targetSdk 36` وإعداد compile SDK 36.1 |
| الحملة | **13 عالمًا / 90 مرحلة** |
| الكرات التجميلية | **105 كرة PNG فردية قابلة للجمع** |
| كتالوج البادل | **6 أوراق × 18 سبرايت = 108 سبرايت بادل = 36 ستايلًا بثلاث حالات** |
| الإعلانات | Google Mobile Ads **25.4.0** وبوابتان Rewarded (revive + talisman) وUMP **4.0.0** |
| المشتريات | Google Play Billing **9.1.0** لمنتجات Booster استهلاكية مع الاستعادة والاستهلاك |
| خلفيات الفيديو | Media3/ExoPlayer **1.5.1** مع Splash و13 فيديو للعوالم |
| معماريات Native | `arm64-v8a`, `armeabi-v7a`, `x86_64` |
| صفحات الذاكرة 16 KB | Play Console يعرضها **متوافقة** |
| تسجيل الدخول | لا يوجد نظام حساب/تسجيل دخول مستخدم داخل هذا الموديول |
| أذونات Manifest المصدر | `VIBRATE`, `INTERNET`, `ACCESS_NETWORK_STATE`؛ دمج Manifests الخاصة بالـSDKs يمكن أن يضيف أذونات في الباندل النهائي |

## أنظمة اللعب المؤكدة من الكود

- محاكاة Brick Breaker في LibGDX بتحديث ثابت ودعم Swept Collision.
- 13 عالم حملة بأسماء فعلية و90 مرحلة مولدة مع تدرج صعوبة وحفظ النجوم والنقاط.
- فتح العوالم والمراحل وتحديد المرحلة التالية عبر `ProgressStore` و`LevelRepository`.
- 105 كرة تجميلية تفتح عبر الحملة، و36 مجموعة بادل تحتوي كل واحدة Normal وWeapon وSticky.
- كتالوجات قدرات للكرات والبادلز تربط العناصر التجميلية بقوى لعب فعلية.
- Power-ups وتمائم ومخزون ومعززات وMultiball وحالات Fire/Piercing/Size والليزر والـSticky/Magnetic وDeath Rail وطوب خاص/ألغاز.
- شاشة نهاية المرحلة/النتائج تعرض المكافآت الجديدة بأنيميشن، بما فيها الكرات ومجموعات البادل المفتوحة.
- محرر مراحل مخصصة مع Validation وUndo/Redo وحفظ/تحميل/نسخ/حذف وتجربة المرحلة.
- حفظ جلسة Pause وحفظ المراحل المخصصة بصيغة JSON.
- فيديوهات العوالم تُشغّل تحت سطح LibGDX الشفاف بواسطة AndroidX Media3/ExoPlayer.

## المتجر وAdMob والموافقة وBilling

مسار الربح في إصدار الإنتاج منفذ فعليًا في الكود:

- `AdMobRewardedAdGateway` يستخدم Rewarded Ads من Google Mobile Ads ويتابع الحالات `LOADING`, `READY`, `SHOWING`, `UNAVAILABLE`, `DISABLED`.
- UMP يحدث حالة الموافقة قبل أن تبدأ البوابة الأساسية بطلب الإعلان، ويعرض Privacy Options عندما تكون مطلوبة.
- عند فشل تحميل Rewarded يعيد المحاولة تلقائيًا بعد **15 ثانية**؛ وإذا كانت الموافقة تمنع الطلب، يعيد فحص التحميل بعد **1.5 ثانية**.
- توجد بوابتان منفصلتان للإعلانات المكافأة: **revive** و**talisman/free reward**.
- Debug وQA يستخدمان Test IDs الرسمية من Google؛ Release يقرأ IDs الحقيقية من Gradle properties ويرفض البناء إذا وجد Test IDs.
- `PlayBillingPurchaseGateway` يستخدم BillingClient 9.1.0 مع Pending one-time purchases وإعادة اتصال تلقائية وأسعار `ProductDetails` المحلية وتحديثات الشراء والاستعادة والاستهلاك.
- المتجر الفعال يحتوي Spark Pack وPower Crate وFull Arsenal وForge Vault؛ وتبقى SKU القديمة معروفة لاستعادة/استهلاك المشتريات القديمة بأمان.
- ربط AdMob بالمتجر ومراجعة التطبيق يعتمدان على توفر اللعبة للعامة في متجر مدعوم؛ لذلك أثناء Closed Testing قد يبقى ملء الإعلانات الحقيقية محدودًا بينما AdMob يعرض أن المراجعة/التحقق معلقة.

## R8 وتقوية إصدار الإنتاج

R8 في هذا المشروع ليس مجرد خيار مفعّل؛ خط إصدار الإنتاج مبني حوله بشكل متعمد.

| الجزء | إعداد المشروع الحالي |
|---|---|
| Minify في Release | `isMinifyEnabled = true` |
| تقليص الموارد | `isShrinkResources = true` |
| قواعد Android المحسنة | `proguard-android-optimize.txt` |
| قواعد المشروع | `proguard-rules.pro` |
| مطابقة QA للإنتاج | نوع `qa` يرث `release` ويُبقي Minify وResource Shrinking مفعّلين |
| ملف Mapping | `app/build/outputs/mapping/release/mapping.txt` |
| رموز Native | `ndk.debugSymbolLevel = "SYMBOL_TABLE"` |
| Assets | ملفات `src/main/assets` لا يحذفها Android Resource Shrinking |

### ما الذي تحافظ عليه قواعد R8 المخصصة؟

- الإبقاء على `SourceFile` و`LineNumberTable` حتى يمكن لـPlay Console عمل ReTrace للـstack traces باستخدام `mapping.txt`، مع توحيد اسم المصدر عبر `-renamesourcefileattribute SourceFile`.
- إسكات تحذير `AndroidFragmentApplication` الاختياري في libGDX.
- الحفاظ على أسماء/تواقيع دوال JNI المطلوبة لمكتبات libGDX وFreeType الأصلية.
- الحفاظ على `StoredCustomLevel` و`LevelDefinition` مع السماح بالتحسين؛ لأن libGDX JSON يصل لحقولها بالـreflection وأسماء الحقول جزء من صيغة حفظ المراحل المخصصة.
- الحفاظ على constructor بدون معاملات لـ`androidx.work.impl.WorkDatabase_Impl` لأن مسار QA مع R8 احتاجه أثناء تهيئة WorkManager.

### فحوص Release الإجبارية

`assembleRelease` و`bundleRelease` يعتمدان على ثلاث مهام حماية:

1. **`verifyProductionMonetization`** — يتحقق من App ID الحقيقي لـAdMob ووحدتي Rewarded، يمنع Test IDs الرسمية في Release، ويتطلب `productionAdsAudienceReviewed=true`.
2. **`verifyProductionSourceSafety`** — يتأكد أن Developer Access معطل افتراضيًا في Release وأن مساراته في القائمة والمتجر مقيدة بـBuildConfig.
3. **`verifyReleaseSigning`** — يتحقق من application ID الدائم ورابط سياسة الخصوصية HTTPS وملف keystore وجميع بيانات التوقيع المطلوبة.

### آخر نتيجة وصلنا لها في Play Console

الباندل المرفوع لمسار Alpha ظهر في Play Console على أنه **R8 Full Mode** مع **94% تحسين** و**94% إخفاء/تشويش** و**94% تقليص للكود**، مع تقليص موارد محسن وإعادة حزم للفئات، وحجم DEX غير مضغوط **5.83 MB**. كما ظهر أن الباندل **متوافق مع صفحات ذاكرة 16 KB**.

> إعداد `SYMBOL_TABLE` يطلب من AGP تضمين رموز Native المتاحة. بعض مكتبات libGDX الجاهزة `.so` تكون stripped أصلًا، لذلك اكتمال Native Symbols يعتمد على ما تحتويه هذه المكتبات، وهذا منفصل عن ملف R8 `mapping.txt` الخاص بـJava/Kotlin.

## الصور والمعاينات

<p align="center">
  <img src="BrickBreakerBall-ad.png" alt="الصورة الدعائية للعبة Brick Breaker Ball" width="750">
</p>

### لقطات اللعب

<p align="center">
  <img src="screenshot-1.png" alt="لقطة لعب 1" width="230">
  ---
  <img src="screenshot-2.png" alt="لقطة لعب 2" width="230">
  ---
  <img src="screenshot-3.png" alt="لقطة لعب 3" width="230">
</p>

### ورقة سبرايت اللعب الأساسية

<p align="center">
  <img src="spritesheet.png" alt="ورقة سبرايت اللعب" width="520">
</p>

نسخة التشغيل الأساسية هي `app/src/main/assets/sprites/spritesheet.png` وإحداثياتها في `sprite-map-detailed.json`.

### معاينات أوراق البادل

| المجموعة | المعاينة |
|---|---|
| 1 | ![Paddle group 1](app/src/main/assets/sprites/paddles-sprites/paddle-group1.png) |
| 2 | ![Paddle group 2](app/src/main/assets/sprites/paddles-sprites/paddles-group2.png) |
| 3 | ![Paddle group 3](app/src/main/assets/sprites/paddles-sprites/paddle-group3.png) |
| 4 | ![Paddle group 4](app/src/main/assets/sprites/paddles-sprites/paddles-group4.png) |
| 5 | ![Paddle group 5](app/src/main/assets/sprites/paddles-sprites/paddles-group5.png) |
| 6 | ![Paddle group 6](app/src/main/assets/sprites/paddles-sprites/paddles-group6.png) |

### ورقة الكرات التجميلية

<p align="center">
  <img src="app/src/main/assets/sprites/balls-sprites/balls-sheet-2.png" alt="ورقة الكرات التجميلية" width="700">
</p>

## البناء والتحقق

```powershell
.\gradlew.bat spotlessCheck
.\gradlew.bat test
.\gradlew.bat lint
.\gradlew.bat assembleQa
.\gradlew.bat clean bundleRelease
```

نوع `qa` مقارب للإنتاج في R8 وتقليص الموارد لكنه يستخدم توقيع Debug وTest Ads الرسمية من Google. ولا يكتمل `bundleRelease` إلا بعد نجاح فحوص الربح وسلامة المصدر والتوقيع.

### ملفات الناتج

```text
AAB:     app/build/outputs/bundle/release/app-release.aab
Mapping: app/build/outputs/mapping/release/mapping.txt
```

## مرجع ملفات Kotlin

يعرض القسم القابل للطي أدناه **ملفات Kotlin الخاصة بالكود والاختبارات فقط**. تم استبعاد الصور والـAssets وموارد Android والمكتبات الأصلية وJSON والصوتيات والفيديوهات والخطوط والـShaders والملفات المولدة عمدًا حتى يبقى المرجع مركزًا على كود البرنامج.

### ملفات Kotlin للتشغيل / الإنتاج (41)

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/AndroidMonetization.kt</code></summary>

تنفيذ Android الفعلي لـ AdMob وUMP وGoogle Play Billing. يدير تحميل/عرض/إعادة محاولة الإعلانات المكافَأة، خيارات الخصوصية، ProductDetails، الشراء، الاستعادة والاستهلاك.

**الأنواع/الكلاسات الرئيسية:** `AdMobRewardedAdGateway`, `PlayBillingPurchaseGateway`

**دوال بارزة:** `refreshConsent`, `preload`, `onAdLoaded`, `onAdFailedToLoad`, `show`, `onAdDismissedFullScreenContent`, `onAdFailedToShowFullScreenContent`, `showPrivacyOptions`, `startAdsIfAllowed`, `dispose` …

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/BallAbilities.kt</code></summary>

يعرّف أنواع وقدرات الكرات ويربط الكرات التجميلية الـ105 بملفات قوة فعلية ومستويات Tier وإحصاءات وأنماط قدرات.

**الأنواع/الكلاسات الرئيسية:** `BallAbilityKind`, `BallAbilityProfile`, `BallAbilityCatalog`

**دوال بارزة:** `shortStat`, `ballNumber`, `profileForBall`, `profileForSprite`, `profile`, `profileForArchetype`

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/BoosterStores.kt</code></summary>

تخزين مخزون المعززات وسجل المعاملات المعالجة وحدود/تبريد المكافآت اليومية من الإعلانات.

**الأنواع/الكلاسات الرئيسية:** `BoosterInventoryStore`, `TransactionLedger`, `DailyRewardStore`

**دوال بارزة:** `count`, `snapshot`, `add`, `addAll`, `consume`, `refund`, `clearForTests`, `total`, `hasProcessed`, `markProcessed` …

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/BrickBreakerGame.kt</code></summary>

منسق اللعبة الرئيسي في LibGDX؛ ينشئ الأصول والخدمات والتقدم، يبدّل الشاشات، يبدأ/يستأنف الجلسات، يطابق المشتريات ويطبق إعدادات الصوت.

**الأنواع/الكلاسات الرئيسية:** `BrickBreakerGame`

**دوال بارزة:** `create`, `setScreen`, `openMenu`, `applyAudioSettings`, `play`, `playCustom`, `resumePausedGame`, `startNewGame`, `reconcilePurchases`, `resume` …

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/Campaign.kt</code></summary>

تعريف العوالم والمراحل والتقدم. يحتوي 13 عالمًا و90 مرحلة حملة مولدة مع تدرج صعوبة ونجوم/نقاط وقواعد فتح وحفظ الإعدادات.

**الأنواع/الكلاسات الرئيسية:** `WorldDefinition`, `LevelDefinition`, `LevelRepository`, `ValidationResult`, `LevelValidator`, `GraphicsQuality`, `GameSettings`, `ProgressStore`

**دوال بارزة:** `level`, `worldLevelCount`, `firstLevel`, `lastLevel`, `worldForLevel`, `isBonusStage`, `createLevel`, `key`, `place`, `worldMaterials` …

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/CampaignBrickPalette.kt</code></summary>

يربط أنواع الطوب والعوالم بمعرّفات الصور وألوان العرض العادية ووضع عمى الألوان.

**دوال بارزة:** `spriteFor`, `tintFor`, `colorBlindTintFor`, `palette`

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/CharmsBagScreen.kt</code></summary>

واجهة حقيبة التمائم/المخزون مع تبويبات وتمرير وبطاقات ووصف واستخدام العناصر.

**الأنواع/الكلاسات الرئيسية:** `CharmsBagScreen`

**دوال بارزة:** `show`, `hide`, `render`, `drawSelectedCharm`, `drawActions`, `drawAction`, `drawTab`, `drawCharmCard`, `handleInput`, `handleMouseWheel` …

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/CosmeticDetailScreens.kt</code></summary>

شاشات تفاصيل قوة الكرة والبادل مع معاينات نظيفة وبيانات القدرة وحالات البادل الثلاث.

**الأنواع/الكلاسات الرئيسية:** `BallPowerDetailScreen`, `PaddlePowerDetailScreen`

**دوال بارزة:** `render`, `drawBallClean`, `smooth`, `drawPaddleFit`

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/CosmeticModels.kt</code></summary>

نماذج العناصر التجميلية والقيم الافتراضية وتخطيط حالات البادل وبناء مجموعات Normal/Weapon/Sticky.

**الأنواع/الكلاسات الرئيسية:** `BallSpriteDefinition`, `PaddleSpriteDefinition`, `CosmeticDefaults`, `CosmeticPaddleSelection`, `PaddleVisualSlot`, `PaddleVisualLayout`, `PaddleStyleSet`, `PaddleStyleCatalog`

**دوال بارزة:** `idForState`, `slots`, `contains`, `build`, `styleForPaddle`

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/CosmeticProgression.kt</code></summary>

ملكية وفتح الكرات والبادلز عبر تقدم الحملة، واسترجاع الفتح من المراحل المكتملة ومنع تجهيز العناصر غير المملوكة خارج وضع التطوير.

**الأنواع/الكلاسات الرئيسية:** `CosmeticUnlock`, `Ball`, `Paddle`, `CosmeticOwnershipStore`, `CosmeticProgressionService`

**دوال بارزة:** `ownsBall`, `ownsPaddleStyle`, `unlockBall`, `unlockPaddleStyle`, `reconcile`, `onCampaignResult`, `ownsPaddle`, `styleForPaddle`, `ownedBallCount`, `totalBallCount` …

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/CosmeticSpriteRepository.kt</code></summary>

تحميل صور وJSON الكرات والبادلز، التحقق من الإحداثيات، إنشاء TextureRegion، استعادة الاختيارات وحذف الموارد.

**الأنواع/الكلاسات الرئيسية:** `PaddleSheetAsset`, `CosmeticSpriteCatalogParser`, `CosmeticSpriteRepository`

**دوال بارزة:** `paddles`, `paddleName`, `paddleDisplayName`, `slug`, `paddleSheets`, `requireInBounds`, `ballDefinition`, `paddleDefinition`, `ballRegion`, `paddleRegion` …

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/CosmeticUnlockRevealScreen.kt</code></summary>

تدفق أنيميشن منفصل لكشف العناصر الجديدة مع معاينات الكرة/البادل وأزرار التجهيز والمتابعة والتخطي.

**الأنواع/الكلاسات الرئيسية:** `CosmeticRevealKind`, `CosmeticRevealFlow`, `CosmeticUnlockRevealScreen`

**دوال بارزة:** `advance`, `skip`, `render`, `revealPanel`, `drawBall`, `drawPaddleStyle`, `drawBallPreview`, `croppedBallPreviewRegion`, `drawEnergy`, `isBallEquipped` …

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/CustomizationScreen.kt</code></summary>

واجهة تخصيص الكرات والبادلز مع الملكية والتمرير والاختيار والمعاينة وفتح شاشة التفاصيل وتجهيز العنصر.

**الأنواع/الكلاسات الرئيسية:** `CustomizationTab`, `CustomizationScreen`

**دوال بارزة:** `visiblePaddleCard`, `visibleBallCard`, `show`, `hide`, `render`, `drawBalls`, `drawPaddles`, `orderedPaddles`, `selectedPaddleId`, `setSelectedPaddleId` …

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/DetailedSpriteSheet.kt</code></summary>

قارئ وغلاف تشغيل لخريطة السبرايت الأساسية المفصلة مع جلب المناطق والتنقل بين حالات الضرر.

**الأنواع/الكلاسات الرئيسية:** `SpriteId`, `SpriteMetadata`, `DetailedSpriteSheet`

**دوال بارزة:** `from`, `region`, `nextState`, `previousState`, `dispose`

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/DevelopmentAccess.kt</code></summary>

بوابة وضع المطور المقيدة بنوع البناء عبر BuildConfig وتتحكم بوصول الاختبار للعوالم والمراحل والعناصر والمشتريات التجريبية.

**الأنواع/الكلاسات الرئيسية:** `DevelopmentAccess`

**دوال بارزة:** `toggle`, `canSelectLevel`, `canSelectWorld`, `canUseCosmetic`, `canMakeTestPurchase`

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/ForgeUiPalette.kt</code></summary>

ألوان الواجهة المركزية لثيم Forge.

**الأنواع/الكلاسات الرئيسية:** `ForgeUiPalette`

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/ForgeUiRenderer.kt</code></summary>

أدوات رسم الأزرار واللوحات والحواف وأشرطة التقدم المتدرجة/المستديرة مع تخزين الخامات المولدة.

**الأنواع/الكلاسات الرئيسية:** `ForgeUiRenderer`, `GradientStyle`

**دوال بارزة:** `drawGradientButton`, `drawRoundedGradientButton`, `drawCompactRoundedGradientButton`, `drawGradientPanel`, `drawGradientBorderPanel`, `drawGradientProgress`, `dispose`, `drawGradient`, `drawRoundedGradient`, `drawCompactRoundedGradient` …

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/ForgeUiTheme.kt</code></summary>

ثوابت المقاسات والأسلوب المشتركة لواجهة Forge.

**الأنواع/الكلاسات الرئيسية:** `ForgeUiTheme`

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/GameAssets.kt</code></summary>

مدير أصول LibGDX المركزي للخطوط والصوت وصور القوائم وموسيقى اللعب والأطالس والخلفيات والعناصر التجميلية.

**الأنواع/الكلاسات الرئيسية:** `GameAssets`

**دوال بارزة:** `gameplayMusicPathForWorld`, `font`, `play`, `worldFallbackBg`, `worldMapImage`, `startMenuTexture`, `pauseMenuTexture`, `settingsMenuTexture`, `startMenuMusic`, `stopMenuMusic` …

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/GameModels.kt</code></summary>

نماذج اللعب الأساسية: الحالات والكرة والبادل والطوب والعناصر الساقطة والليزر وأحداث التغذية الراجعة وحلقة التحديث الثابتة.

**الأنواع/الكلاسات الرئيسية:** `GamePhase`, `BallElement`, `BallCollisionMode`, `BallSize`, `PaddleMode`, `BrickType`, `DamageStage`, `Paddle`, `Ball`, `Brick`, `FallingPowerUp`, `LaserShot`

**دوال بارزة:** `smaller`, `larger`, `megaBoosted`, `fromRadius`, `animate`, `advance`

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/GameScreen.kt</code></summary>

شاشة اللعب الرئيسية: رسم العالم والملعب وHUD، إدارة Pause والمخزون وGame Over وإنهاء المرحلة والمتابعة بإعلان والتخطيط المتجاوب.

**الأنواع/الكلاسات الرئيسية:** `GameScreen`

**دوال بارزة:** `requiresGameplayBallSprite`, `render`, `drawLevelCompleteTransition`, `brick`, `paddle`, `ball`, `aimGuide`, `hud`, `activeEffectsHud`, `drawHudCell` …

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/GameSession.kt</code></summary>

محرك قواعد ومحاكاة اللعب: فيزياء الكرة، الاصطدامات، ضرر الطوب، القوى، الكبسولات، الليزر/قدرات البادل، النقاط والأرواح والتحديث.

**الأنواع/الكلاسات الرئيسية:** `BoosterUseResult`, `Applied`, `Rejected`, `GameSession`

**دوال بارزة:** `newLevel`, `serve`, `createBall`, `activeElement`, `applyCustomization`, `clampPaddleForActiveLayout`, `action`, `armPaddleAbility`, `paddleAbilityStatus`, `hasAttachedBalls` …

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/GameplayAtlas.kt</code></summary>

طبقة الوصول لسبرايتات اللعب الأساسية مثل الكرات والطوب والبادلز وأيقونات القوى.

**الأنواع/الكلاسات الرئيسية:** `PaddleSkin`, `GameplayAtlas`

**دوال بارزة:** `ball`, `brick`, `preview`, `intactBrickId`, `powerUpIcon`

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/GameplayTuning.kt</code></summary>

ثوابت موازنة اللعب للسرعات والأحجام والأزمنة والقيم المستخدمة في المحاكاة.

**الأنواع/الكلاسات الرئيسية:** `GameplayTuning`

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/LevelEditorModels.kt</code></summary>

نماذج محرر المراحل مع Undo/Redo وترميز الطوب ومستودع مراحل مخصصة محفوظة بصيغة JSON على الجهاز.

**الأنواع/الكلاسات الرئيسية:** `BrickCodec`, `EditorTool`, `PaletteSection`, `BrickInfo`, `BrickInfoRepository`, `EditorCell`, `LevelProperties`, `LevelEditorState`, `StoredCustomLevel`, `CustomLevelRepository`

**دوال بارزة:** `symbol`, `type`, `key`, `from`, `beginStroke`, `paint`, `endStroke`, `fill`, `clear`, `mutateProperties` …

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/LevelEditorScreen.kt</code></summary>

واجهة محرر المراحل للرسم على الشبكة والباليت والخصائص والحفظ/التحميل/النسخ/الحذف/التجربة والتحقق.

**الأنواع/الكلاسات الرئيسية:** `LevelEditorScreen`

**دوال بارزة:** `render`, `drawGrid`, `handleGridInput`, `gridAtTouch`, `drawPalette`, `handlePaletteInput`, `compactBrickDescription`, `drawProperties`, `handlePropertiesInput`, `hit` …

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/MainActivity.kt</code></summary>

Activity المضيف على Android؛ ينشئ عرض LibGDX وطبقة فيديو ExoPlayer وPlay Billing وبوابتي Rewarded AdMob ويربط دورة الحياة.

**الأنواع/الكلاسات الرئيسية:** `MainActivity`

**دوال بارزة:** `onCreate`, `playWorldVideo`, `stopWorldVideo`, `onPause`, `onResume`, `onDestroy`

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/MonetizationServices.kt</code></summary>

واجهات ونتائج الربح المحايدة عن المنصة وحاوية خدمات Play Billing والإعلانات المكافَأة.

**الأنواع/الكلاسات الرئيسية:** `PurchaseResult`, `Success`, `Pending`, `Cancelled`, `Failed`, `RecoveredPurchase`, `PurchaseGateway`, `AdState`, `RewardedAdResult`, `Earned`, `ClosedWithoutReward`, `RewardedAdGateway`

**دوال بارزة:** `purchase`, `price`, `refreshProducts`, `reconcile`, `dispose`, `show`, `preload`, `showPrivacyOptions`, `refreshConsent`, `earn` …

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/PaddleAbilities.kt</code></summary>

تعريف قدرات البادل وربط معرّفات البادل العادي بقدرات لعب وTier.

**الأنواع/الكلاسات الرئيسية:** `PaddleAbilityKind`, `PaddleAbilityProfile`, `PaddleAbilityCatalog`

**دوال بارزة:** `profileForNormalPaddle`, `profile`, `knownNormalPaddleIds`, `profileForArchetype`

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/PausedSessionStore.kt</code></summary>

حفظ واستعادة جلسة الحملة المتوقفة ومسحها عند انتهاء الحاجة.

**الأنواع/الكلاسات الرئيسية:** `PausedSessionStore`

**دوال بارزة:** `hasPausedGame`, `save`, `restore`, `clear`

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/PowerUpInfoRepository.kt</code></summary>

الأسماء والأوصاف المقروءة لأنواع الـPower-ups مع توافق الأوصاف القديمة.

**الأنواع/الكلاسات الرئيسية:** `PowerUpInfo`, `PowerUpInfoRepository`

**دوال بارزة:** `info`, `legacyDescription`

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/PowerUps.kt</code></summary>

تعريفات القوى وفئاتها ومدير السقوط والتأثيرات النشطة وقواعد الحفظ والاستعادة.

**الأنواع/الكلاسات الرئيسية:** `PowerUpCategory`, `EffectScope`, `StackPolicy`, `CapsuleStyle`, `PowerUpType`, `PowerUpDefinition`, `PowerUpCatalog`, `PowerUpDropDirector`, `PowerUpManager`

**دوال بارزة:** `worldDropTypes`, `update`, `choose`, `activate`, `deactivate`, `clearLifeScoped`, `clear`, `persistentSnapshot`, `restorePersistent`, `activeEffects` …

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/PrivacyPolicyScreen.kt</code></summary>

عارض سياسة الخصوصية داخل اللعبة مع قص وتمرير وزر رجوع.

**الأنواع/الكلاسات الرئيسية:** `PrivacyPolicyScreen`

**دوال بارزة:** `render`, `beginTextClip`, `endTextClip`, `handleScroll`, `drawScrollTrack`, `goBack`

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/Replay.kt</code></summary>

هياكل بيانات الإدخال/الإعادة ومسجل بسيط لإدخالات اللعب.

**الأنواع/الكلاسات الرئيسية:** `ReplayInput`, `ReplayData`, `ReplayRecorder`

**دوال بارزة:** `record`, `snapshot`

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/RewardRevealScreen.kt</code></summary>

حفظ وعرض المكافآت المعلقة بين انتقالات الشاشات والجلسات.

**الأنواع/الكلاسات الرئيسية:** `PendingReward`, `PendingRewardRevealStore`, `RewardRevealScreen`

**دوال بارزة:** `save`, `load`, `clear`, `render`

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/RulesScreen.kt</code></summary>

عارض قواعد اللعبة داخل التطبيق مع عناوين وفقرات وتمييزات وصور وتمرير.

**الأنواع/الكلاسات الرئيسية:** `Heading`, `Paragraph`, `Image`, `Spacer`, `RulesScreen`

**دوال بارزة:** `scrolled`, `render`, `show`, `hide`, `updateSmoothScroll`, `drawRules`, `withPanelClip`, `drawHeading`, `drawParagraph`, `parseInlineTokens` …

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/ShopModels.kt</code></summary>

كتالوج المتجر ومنطق المنح؛ أربع حزم استهلاكية فعالة وSKU قديمة للاستعادة وحالة السعر المحلي ومنح حتمية للمعززات.

**الأنواع/الكلاسات الرئيسية:** `ShopProductId`, `BoosterGrant`, `RandomTotal`, `EachType`, `EachTypePlusRandom`, `ShopProduct`, `ShopProductUiState`, `ShopCatalog`, `BoosterGrantFactory`, `DailyRewardState`

**دوال بارزة:** `shopProductUiState`, `findByStoreId`, `createGrant`, `randomGrant`, `reward`, `dayKey`

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/ShopScreen.kt</code></summary>

واجهة المتجر للتبويبات والحزم والبادلز والكرات والعناصر والمكافآت المجانية؛ تربط أسعار/مشتريات Play Billing وإعلانات Rewarded.

**الأنواع/الكلاسات الرئيسية:** `ShopReturnDestination`, `ShopScreen`

**دوال بارزة:** `show`, `hide`, `render`, `beginStore`, `drawHeader`, `drawTabs`, `drawSectionTitle`, `scrollOffset`, `maxScrollOffset`, `updateScrollDrag` …

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/SweptCollision.kt</code></summary>

خوارزمية اصطدام دائرة متحركة مع AABB لتقليل عبور الكرة للطوب عند السرعات العالية.

**الأنواع/الكلاسات الرئيسية:** `CollisionResult`, `SweptCollision`

**دوال بارزة:** `circleVsAabb`

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/UiScreens.kt</code></summary>

شاشات وتدفقات LibGDX المشتركة: Splash والقائمة والعوالم والمراحل والنتائج والاختبار المخصص والإعدادات، وتشمل مكافآت نهاية المرحلة المتحركة.

**الأنواع/الكلاسات الرئيسية:** `ForgeScreen`, `SplashScreen`, `MainMenuScreen`, `WorldMapScreen`, `LevelSelectScreen`, `ResultsScreen`, `CustomTestResultScreen`, `SettingsScreen`

**دوال بارزة:** `installMouseWheelHandler`, `scrolled`, `removeMouseWheelHandler`, `drawFullscreen`, `drawFullscreenPanel`, `begin`, `title`, `button`, `drawLockBadge`, `fittedText` …

</details>

<details>
<summary><code>src/main/java/com/example/brick_breaker_ball/WorldVideoBackgrounds.kt</code></summary>

جسر بين شاشات LibGDX وExoPlayer على Android؛ يربط 13 عالمًا وفيديو Splash بملفات MP4.

**الأنواع/الكلاسات الرئيسية:** `WorldVideoBackgrounds`

**دوال بارزة:** `assetForWorld`, `isVideoVisible`, `showAsset`

</details>

### ملفات Kotlin لاختبارات Unit (21)

<details>
<summary><code>src/test/java/com/example/brick_breaker_ball/BallAbilityTest.kt</code></summary>

يتحقق من كتالوج قدرات الكرات وربط القوة.

**الأنواع/الكلاسات الرئيسية:** `BallAbilityTest`

**دوال بارزة:** `allOneHundredFiveNumberedBallsHaveAValidPowerProfile`, `fiveProgressionTiersCoverTwentyOneBallsEach`, `sevenAbilityArchetypesRepeatInStableOrder`, `highNumberBallSpritesUseThreeDigitCatalogNumbers`

</details>

<details>
<summary><code>src/test/java/com/example/brick_breaker_ball/CampaignBrickPaletteTest.kt</code></summary>

يتحقق من ربط صور/ألوان طوب الحملة.

**الأنواع/الكلاسات الرئيسية:** `CampaignBrickPaletteTest`

</details>

<details>
<summary><code>src/test/java/com/example/brick_breaker_ball/CampaignProgressionTest.kt</code></summary>

يتحقق من فتح العوالم والمراحل وقواعد إكمال الحملة.

**الأنواع/الكلاسات الرئيسية:** `CampaignProgressionTest`

</details>

<details>
<summary><code>src/test/java/com/example/brick_breaker_ball/CosmeticCustomizationTest.kt</code></summary>

يتحقق من كتالوج الكرات/البادلات والمعرّفات والاختيار والحفظ.

**الأنواع/الكلاسات الرئيسية:** `CosmeticCustomizationTest`

**دوال بارزة:** `parsePaddles`

</details>

<details>
<summary><code>src/test/java/com/example/brick_breaker_ball/CosmeticSetProgressionTest.kt</code></summary>

يتحقق من مراحل فتح العناصر ومجموعات حالات البادل.

**الأنواع/الكلاسات الرئيسية:** `CosmeticSetProgressionTest`

**دوال بارزة:** `ball`, `style`, `paddle`, `fixture`

</details>

<details>
<summary><code>src/test/java/com/example/brick_breaker_ball/DetailedSpriteManifestTest.kt</code></summary>

يتحقق من بنية خريطة السبرايت الأساسية والمناطق المعرفة.

**الأنواع/الكلاسات الرئيسية:** `DetailedSpriteManifestTest`

</details>

<details>
<summary><code>src/test/java/com/example/brick_breaker_ball/DevelopmentAccessTest.kt</code></summary>

يتحقق من تقييد وضع المطور وتعطيله في الإنتاج.

**الأنواع/الكلاسات الرئيسية:** `DevelopmentAccessTest`

**دوال بارزة:** `debugModeBypassesSelectionLocksWithoutChangingProgress`, `releaseModeIgnoresPreviouslyEnabledPreference`

</details>

<details>
<summary><code>src/test/java/com/example/brick_breaker_ball/GameLogicTest.kt</code></summary>

اختبارات عامة لمنطق اللعب والجلسة.

**الأنواع/الكلاسات الرئيسية:** `GameLogicTest`

**دوال بارزة:** `playingSession`, `collisionSession`, `hitBrick`

</details>

<details>
<summary><code>src/test/java/com/example/brick_breaker_ball/GameplayRulesRegressionTest.kt</code></summary>

اختبارات Regression لقواعد اللعب التي يجب ألا تتغير بدون قصد.

**الأنواع/الكلاسات الرئيسية:** `GameplayRulesRegressionTest`

**دوال بارزة:** `levelWith`, `stageOneHasNoExplosiveTeachingTrap`, `megaBallIsVisiblyMega`, `piercingBallCanDestroySteelAndContinue`, `laserShotsInheritFireAndPiercingEffects`, `losingALifeClearsTemporaryEffectsAndFallingTalismans`

</details>

<details>
<summary><code>src/test/java/com/example/brick_breaker_ball/LevelEditorTest.kt</code></summary>

يتحقق من حالة المحرر وترميز الطوب وUndo/Redo والمراحل المخصصة.

**الأنواع/الكلاسات الرئيسية:** `LevelEditorTest`

**دوال بارزة:** `level`

</details>

<details>
<summary><code>src/test/java/com/example/brick_breaker_ball/NewSpriteGameplayTest.kt</code></summary>

اختبارات Regression لسلوك اللعب مع خط الأصول/السبرايتات الجديد.

**الأنواع/الكلاسات الرئيسية:** `NewSpriteGameplayTest`

**دوال بارزة:** `playing`, `sentinel`, `collisionGame`, `hitUp`, `advance`

</details>

<details>
<summary><code>src/test/java/com/example/brick_breaker_ball/PaddleAbilityTest.kt</code></summary>

يتحقق من ملفات قدرات البادل وسلوكها.

**الأنواع/الكلاسات الرئيسية:** `PaddleAbilityTest`

**دوال بارزة:** `everyPaddleStyleHasAStableAbility`, `firstFourStylesMatchDesignedProgression`, `hyperGlideMovesPaddleFasterThanStarter`, `impactBoostTapArmsShortPerfectWindow`, `pausedSessionPreservesAbilityStateAndFireCharge`

</details>

<details>
<summary><code>src/test/java/com/example/brick_breaker_ball/Phase1PowerUpTest.kt</code></summary>

يتحقق من المجموعة الأولى من قوى الـPower-ups وتفاعلاتها.

**الأنواع/الكلاسات الرئيسية:** `Phase1PowerUpTest`

**دوال بارزة:** `configured`, `playing`, `sentinel`, `collisionGame`, `hitUp`, `advance`

</details>

<details>
<summary><code>src/test/java/com/example/brick_breaker_ball/ProductionEnhancementsTest.kt</code></summary>

يتحقق من تحسينات وحواجز الأمان الخاصة بجاهزية الإنتاج.

**الأنواع/الكلاسات الرئيسية:** `ProductionEnhancementsTest`

**دوال بارزة:** `everyWorldExposesTheCompleteClassicTalismanPool`, `everyCampaignStageHasAGuaranteedTalismanCarrier`, `forcedCarrierStillDropsWhenThreeTalismansAreAlreadyFalling`, `rewardedContinueRestoresThreeLivesAndStopsAfterThreeUses`, `gameOverPauseRestoreKeepsRewardedContinueUsage`, `expandTalismanNowCreatesALargerPaddleStep`, `magneticTalismanUsesTheStrengthenedField`

</details>

<details>
<summary><code>src/test/java/com/example/brick_breaker_ball/PuzzleBrickBehaviorTest.kt</code></summary>

يتحقق من الطوب الخاص/الألغاز وقواعد مجموعاته.

**الأنواع/الكلاسات الرئيسية:** `PuzzleBrickBehaviorTest`

**دوال بارزة:** `session`, `definition`, `hit`

</details>

<details>
<summary><code>src/test/java/com/example/brick_breaker_ball/ShopAvailabilityTest.kt</code></summary>

يتحقق من توافر منتجات المتجر وحالات السعر.

**الأنواع/الكلاسات الرئيسية:** `ShopAvailabilityTest`

</details>

<details>
<summary><code>src/test/java/com/example/brick_breaker_ball/StartMenuAssetsTest.kt</code></summary>

يتحقق من وجود وتسميات أصول القائمة الرئيسية.

**الأنواع/الكلاسات الرئيسية:** `StartMenuAssetsTest`

**دوال بارزة:** `assertButtonTexture`

</details>

<details>
<summary><code>src/test/java/com/example/brick_breaker_ball/TestPreferences.kt</code></summary>

بديل Preferences للاختبارات بدون تخزين حقيقي.

**الأنواع/الكلاسات الرئيسية:** `TestPreferences`

**دوال بارزة:** `putBoolean`, `putInteger`, `putLong`, `putFloat`, `putString`, `put`, `getBoolean`, `getInteger`, `getLong`, `getFloat` …

</details>

<details>
<summary><code>src/test/java/com/example/brick_breaker_ball/WorldBalanceRegressionTest.kt</code></summary>

يتحقق من موازنة/صعوبة العوالم ضد التراجع.

**الأنواع/الكلاسات الرئيسية:** `WorldBalanceRegressionTest`

**دوال بارزة:** `effort`, `everyGeneratedCampaignLevelValidates`, `averageHitPointWorkRisesWorldByWorld`, `averageStartingSpeedRisesWorldByWorld`, `annotatedBreakableSpecialBricksUseCanonicalOneHitContract`, `plusFourAndFifteenBallTalismansAreInstantSpawns`, `everyWorldHasAVisualMaterialPalette`

</details>

<details>
<summary><code>src/test/java/com/example/brick_breaker_ball/WorldMapAssetsTest.kt</code></summary>

يتحقق من صور خريطة العوالم والأصول المرتبطة.

**الأنواع/الكلاسات الرئيسية:** `WorldMapAssetsTest`

</details>

<details>
<summary><code>src/test/java/com/example/brick_breaker_ball/WorldVideoBackgroundsTest.kt</code></summary>

يتحقق من ربط العالم بالفيديو وحالات إظهار/إخفاء الخلفية.

**الأنواع/الكلاسات الرئيسية:** `WorldVideoBackgroundsTest`

</details>

### ملف Kotlin لاختبار Android Instrumentation (1)

<details>
<summary><code>src/androidTest/java/com/example/brick_breaker_ball/ExampleInstrumentedTest.kt</code></summary>

اختبار Instrumentation أساسي لسياق التطبيق والحزمة.

**الأنواع/الكلاسات الرئيسية:** `ExampleInstrumentedTest`

**دوال بارزة:** `useAppContext`

</details>

## أمان المستودع

لا ترفع `local.properties` أو `keystore.properties` أو ملفات keystore أو كلمات مرور التوقيع أو IDs/أسرار الإنتاج الخاصة إلى GitHub. Build الإنتاج نفسه يتحقق من application ID غير `com.example` ورابط سياسة خصوصية HTTPS وإعداد keystore مكتمل قبل إنشاء باندل Google Play.

## الخاتمة

<p align="center">
  <img src="app-icon.png" alt="أيقونة Brick Breaker Ball" width="150">
</p>

<p align="center">
  <strong>Brick Breaker Ball</strong><br>
  لعبة كسر طوب حديثة مبنية بـ Kotlin وLibGDX لنظام Android، وتضم حملة متدرجة، كرات وبادلز قابلة للجمع، إعلانات AdMob مكافئة، Google Play Billing، عوالم فيديو عبر Media3، ومسار Release محمي بـ R8 وفحوص إنتاج قبل إنشاء الباندل.
</p>

<p align="center">
  اللعبة منشورة حاليًا على مسار <strong>Google Play Closed Testing / Alpha</strong> وتتابع مرحلة الاختبار تمهيدًا للانتقال إلى الإصدار العام Production.
</p>

<p align="center">
  <code>#BrickBreakerBall</code> <code>#Kotlin</code> <code>#LibGDX</code> <code>#AndroidGame</code> <code>#GooglePlay</code> <code>#AdMob</code> <code>#GooglePlayBilling</code> <code>#R8</code> <code>#IndieGame</code> <code>#ArcadeGame</code>
</p>

<p align="center">
  <sub>تطوير <strong>ForgePulse Games</strong> • مع التركيز على الأداء، قابلية الصيانة، وأمان إصدار الإنتاج.</sub>
</p>
