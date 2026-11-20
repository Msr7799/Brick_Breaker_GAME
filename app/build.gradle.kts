import com.google.firebase.crashlytics.buildtools.gradle.CrashlyticsExtension
import java.util.Properties

plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")

    // Kotlin formatter
    id("com.diffplug.spotless") version "8.10.2"
}

// QA has its own registered Firebase package in src/qa/google-services.json, so
// keep Google Services resource generation ENABLED for QA. Skipping it previously
// disabled Firebase initialization and Play Games / Firebase authentication in QA.
// Debug still needs a dedicated .debug Firebase app before its plugin can be enabled.
tasks.configureEach {
    if (name == "processDebugGoogleServices") {
        enabled = false
    }
}

val gdxVersion = "1.14.2"
val productionApplicationId =
    providers.gradleProperty("productionApplicationId").orElse("com.forgepulse.brickbreakerball").get()
// Play Games game-server Web OAuth client, linked to this game in Play Console.
// The Firebase-generated default_web_client_id belongs to a different project
// and must never be used as a fallback for requestServerSideAccess.
val playGamesWebClientId = providers.gradleProperty("playGamesWebClientId").orElse("").get().trim()
val expectedPlayGamesServerClientId =
    "6148685744-2gql3gj9l1q6d9167j47h4a4qtbvndnr.apps.googleusercontent.com"
val expectedGameFirebaseProjectId = "brick-breaker-ball-8e4f5"

val natives by configurations.creating

private val volleyWorldRoot = file("src/main/assets/volley-world")
private val volleyWorldDirectories = volleyWorldRoot
    .listFiles()
    .orEmpty()
    .filter { it.isDirectory && it.name.matches(Regex("\\d{2}_.+")) }
    .sortedBy { it.name }
private val volleyStageNumbers = volleyWorldDirectories.map { worldDirectory ->
    val stageNumbers = worldDirectory
        .listFiles()
        .orEmpty()
        .mapNotNull { file -> Regex("(\\d+)\\.(?:webp|jpg|png)", RegexOption.IGNORE_CASE).matchEntire(file.name)?.groupValues?.get(1)?.toIntOrNull() }
        .filter { it in 1..10 }
        .distinct()
        .sorted()
    require(stageNumbers.isNotEmpty()) { "Volley world ${worldDirectory.name} must contain at least one numbered image" }
    stageNumbers
}
private val volleyStageCounts = volleyStageNumbers.map(List<Int>::size)
private val volleyLoadingImageFiles = volleyWorldRoot.resolve("Loading_Screen")
    .listFiles()
    .orEmpty()
    .filter { it.isFile }
    .mapNotNull { file ->
        val number = Regex("loading-screen(\\d+)\\.(?:webp|jpg|png)", RegexOption.IGNORE_CASE)
            .matchEntire(file.name)?.groupValues?.get(1)?.toIntOrNull()
        number?.let { it to file.name }
    }
    .sortedWith(compareBy<Pair<Int, String>> { it.first }.thenBy { it.second })
    .map { it.second }
require(volleyLoadingImageFiles.isNotEmpty()) {
    "Volley loading artwork folder must contain at least one numbered loading-screen image"
}

val keystorePropertiesFile = rootProject.file("keystore.properties")

val keystoreProperties =
    Properties().apply {
        if (keystorePropertiesFile.exists()) {
            keystorePropertiesFile.inputStream().use(::load)
        }
    }

android {
    namespace = "com.example.brick_breaker_ball"

    compileSdk {
        version =
            release(36) {
                minorApiLevel = 1
            }
    }

    defaultConfig {
        applicationId = productionApplicationId
        minSdk = 24
        targetSdk = 36
        versionCode = 35
        versionName = "3.0.8"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val privacyPolicyUrl = providers.gradleProperty("privacyPolicyUrl").orElse("").get()
        val escapedPrivacyPolicyUrl = privacyPolicyUrl.replace("\\", "\\\\").replace("\"", "\\\"")
        buildConfigField("String", "PRIVACY_POLICY_URL", "\"$escapedPrivacyPolicyUrl\"")
        // Vercel backend HTTPS base URL, configured per environment (no secrets in the APK).
        val rewardsUrl = providers.gradleProperty("rewardsApiBaseUrl").orElse("").get().trim().trimEnd('/')
        require(rewardsUrl.isEmpty() || rewardsUrl.matches(Regex("https://[A-Za-z0-9.-]+(?::[0-9]+)?"))) {
            "rewardsApiBaseUrl must be an HTTPS origin without a path"
        }
        val escapedRewardsUrl = rewardsUrl.replace("\\", "\\\\").replace("\"", "\\\"")
        buildConfigField("String", "REWARDS_API_BASE_URL", "\"$escapedRewardsUrl\"")
        buildConfigField("boolean", "DEVELOPER_ACCESS_ALLOWED", "false")
        buildConfigField("boolean", "QA_DIAGNOSTICS", "false")
        val escapedPlayGamesWebClient = playGamesWebClientId.replace("\\", "\\\\").replace("\"", "\\\"")
        buildConfigField("String", "PLAY_GAMES_WEB_CLIENT_ID", "\"$escapedPlayGamesWebClient\"")
        buildConfigField("String", "VOLLEY_STAGE_COUNTS", "\"${volleyStageCounts.joinToString(",")}\"")
        buildConfigField("String", "VOLLEY_STAGE_IMAGES", "\"${volleyStageNumbers.joinToString(";") { it.joinToString(",") }}\"")
        buildConfigField("String", "VOLLEY_LOADING_IMAGES", "\"${volleyLoadingImageFiles.joinToString(";")}\"")
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile =
                    rootProject.file(
                        keystoreProperties.getProperty("storeFile")
                    )

                storePassword =
                    keystoreProperties.getProperty(
                        "storePassword"
                    )

                keyAlias =
                    keystoreProperties.getProperty(
                        "keyAlias"
                    )

                keyPassword =
                    keystoreProperties.getProperty(
                        "keyPassword"
                    )
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            buildConfigField("boolean", "DEVELOPER_ACCESS_ALLOWED", "true")
            buildConfigField("boolean", "QA_DIAGNOSTICS", "true")

            manifestPlaceholders["admobAppId"] =
                "ca-app-pub-3940256099942544~3347511713"

            manifestPlaceholders["adsProviderEnabled"] =
                "true"

            buildConfigField(
                "String",
                "REWARDED_REVIVE_AD_UNIT",
                "\"ca-app-pub-3940256099942544/5224354917\""
            )
            buildConfigField(
                "String",
                "REWARDED_TALISMAN_AD_UNIT",
                "\"ca-app-pub-3940256099942544/5224354917\""
            )
        }

        release {
            buildConfigField("boolean", "DEVELOPER_ACCESS_ALLOWED", "false")

            // معرّفات الإنتاج لا تُختلق.
            // يبقى الإعلان معطلاً حتى يهيئه الناشر.

            val adsApp =
                providers
                    .gradleProperty(
                        "productionAdmobAppId"
                    ).orElse("")
                    .get()

            val reviveAdsUnit =
                providers
                    .gradleProperty(
                        "productionRewardedReviveAdUnitId"
                    ).orElse("")
                    .get()

            val talismanAdsUnit =
                providers
                    .gradleProperty(
                        "productionRewardedTalismanAdUnitId"
                    ).orElse("")
                    .get()

            require(
                !adsApp.contains(
                    "3940256099942544"
                ) &&
                    !reviveAdsUnit.contains(
                        "3940256099942544"
                    ) &&
                    !talismanAdsUnit.contains(
                        "3940256099942544"
                    )
            ) {
                "Google sample ads are forbidden in Release"
            }

            val adsEnabled =
                adsApp.matches(
                    Regex(
                        "ca-app-pub-[0-9]{16}~[0-9]{10}"
                    )
                ) &&
                    reviveAdsUnit.matches(
                        Regex(
                            "ca-app-pub-[0-9]{16}/[0-9]{10}"
                        )
                    ) &&
                    talismanAdsUnit.matches(
                        Regex(
                            "ca-app-pub-[0-9]{16}/[0-9]{10}"
                        )
                    ) &&
                    providers
                        .gradleProperty(
                            "productionAdsAudienceReviewed"
                        ).orElse(
                            "false"
                        ).get() == "true"

            manifestPlaceholders["admobAppId"] =
                if (adsEnabled) {
                    adsApp
                } else {
                    ""
                }

            manifestPlaceholders["adsProviderEnabled"] =
                adsEnabled.toString()

            buildConfigField(
                "String",
                "REWARDED_REVIVE_AD_UNIT",
                "\"${if (adsEnabled) reviveAdsUnit else ""}\""
            )
            buildConfigField(
                "String",
                "REWARDED_TALISMAN_AD_UNIT",
                "\"${if (adsEnabled) talismanAdsUnit else ""}\""
            )

            // Production optimization: shrink/optimize/obfuscate bytecode with R8
            // and remove unused Android resources. Assets under src/main/assets are unaffected.
            isMinifyEnabled = true
            isShrinkResources = true

            // Ask AGP to package any native symbol table metadata that is actually
            // available. Prebuilt third-party .so files may already be stripped.
            ndk {
                debugSymbolLevel = "SYMBOL_TABLE"
            }
            configure<CrashlyticsExtension> {
                nativeSymbolUploadEnabled = true
            }

            if (keystorePropertiesFile.exists()) {
                signingConfig =
                    signingConfigs.getByName(
                        "release"
                    )
            }

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
                "proguard-rules.pro"
            )
        }

        // Production-like testing build.
        create("qa") {
            initWith(
                getByName(
                    "release"
                )
            )

            applicationIdSuffix = ".qa"
            versionNameSuffix = "-qa"
            buildConfigField("boolean", "DEVELOPER_ACCESS_ALLOWED", "true")
            buildConfigField("boolean", "QA_DIAGNOSTICS", "true")

            signingConfig =
                signingConfigs.getByName(
                    "debug"
                )

            manifestPlaceholders["admobAppId"] =
                "ca-app-pub-3940256099942544~3347511713"

            manifestPlaceholders["adsProviderEnabled"] =
                "true"

            buildConfigField(
                "String",
                "REWARDED_REVIVE_AD_UNIT",
                "\"ca-app-pub-3940256099942544/5224354917\""
            )
            buildConfigField(
                "String",
                "REWARDED_TALISMAN_AD_UNIT",
                "\"ca-app-pub-3940256099942544/5224354917\""
            )

            matchingFallbacks +=
                listOf(
                    "debug",
                    "release"
                )

            // QA intentionally mirrors Release R8/resource shrinking so runtime
            // issues are caught before the Play bundle is uploaded.
            isMinifyEnabled = true
            isShrinkResources = true
            configure<CrashlyticsExtension> {
                nativeSymbolUploadEnabled = true
            }
        }
    }

    compileOptions {
        sourceCompatibility =
            JavaVersion.VERSION_11

        targetCompatibility =
            JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
        viewBinding = false
    }

    sourceSets["main"]
        .jniLibs
        .directories += "libs"
}

// Check only the three font files instantiated by GameAssets.
val verifyReleaseFonts = tasks.register("verifyReleaseFonts") {
    group = "verification"
    description = "Ensure the supplied game fonts are available for release."
    doLast {
        val fontNames = listOf(
            "GOTHAM-BOLD.TTF",
            "TANHEADLINE-Regular.ttf",
            "Insignia.ttf"
        )
        fontNames.forEach { name ->
            val fontFile = file("src/main/assets/fonts/$name")
            require(fontFile.isFile && fontFile.length() > 1_000L) {
                "Missing game font: app/src/main/assets/fonts/$name"
            }
        }
    }
}
tasks.matching { it.name == "assembleRelease" || it.name == "bundleRelease" }.configureEach {
    dependsOn(verifyReleaseFonts)
}


// =============================================================================
// Spotless + ktlint
// =============================================================================
//
// هدف الإعداد هنا:
// Formatter مثل Prettier.
//
// لذلك نعطّل قواعد ktlint التي:
// - لا يستطيع إصلاحها تلقائياً
// - أو لا تناسب المشروع الحالي
// - أو تسبب looping / lint failure
//
// spotlessApply سيستمر في تنسيق:
// spacing
// indentation
// braces
// wrapping
// trailing commas
// blank lines
// وغيرها.
// =============================================================================

spotless {
    kotlin {
        target(
            "src/**/*.kt"
        )

        targetExclude(
            "**/build/**",
            "**/generated/**"
        )

        ktlint(
            "1.8.0"
        ).editorConfigOverride(
            mapOf(
                // Android Studio formatting style
                "ktlint_code_style" to
                    "android_studio",

                // -------------------------------------------------------------
                // Legacy project package:
                // com.example.brick_breaker_ball
                // -------------------------------------------------------------
                "ktlint_standard_package-name" to
                    "disabled",

                // -------------------------------------------------------------
                // wildcard imports موجودة في بعض الاختبارات.
                // ktlint لا يستطيع دائماً إصلاحها تلقائياً.
                // -------------------------------------------------------------
                "ktlint_standard_no-wildcard-imports" to
                    "disabled",

                // -------------------------------------------------------------
                // هذا كان من أسباب Campaign.kt / الأسطر الطويلة.
                // لا نريد formatter يفشل بسبب طول السطر.
                // -------------------------------------------------------------
                "ktlint_standard_max-line-length" to
                    "disabled",

                // -------------------------------------------------------------
                // المشروع يحتوي KDoc وتعليقات legacy
                // بأماكن ktlint يعتبرها غير قياسية.
                // لا نريد تغيير محتوى التوثيق تلقائياً.
                // -------------------------------------------------------------
                "ktlint_standard_kdoc" to
                    "disabled",

                // -------------------------------------------------------------
                // بعض ملفات المشروع فيها block comments وسط السطر.
                // نتركها كما هي بدل فشل formatter بالكامل.
                // -------------------------------------------------------------
                "ktlint_standard_comment-wrapping" to
                    "disabled"
            )
        )

        trimTrailingWhitespace()
        endWithNewline()
    }

    kotlinGradle {
        target(
            "*.gradle.kts"
        )

        targetExclude(
            "**/build/**",
            "**/generated/**"
        )

        ktlint(
            "1.8.0"
        ).editorConfigOverride(
            mapOf(
                "ktlint_code_style" to
                    "android_studio",

                // لا نخلي build.gradle يفشل بسبب
                // أسطر Dependencies أو Regex طويلة.
                "ktlint_standard_max-line-length" to
                    "disabled",

                "ktlint_standard_comment-wrapping" to
                    "disabled"
            )
        )

        trimTrailingWhitespace()
        endWithNewline()
    }
}

// =============================================================================
// Dependencies
// =============================================================================

dependencies {
    implementation(platform("com.google.firebase:firebase-bom:34.19.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-crashlytics-ndk")
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.android.gms:play-services-games-v2:22.1.0")
    implementation("com.google.android.play:app-update:2.1.0")

    implementation(
        "com.google.android.gms:play-services-ads:25.4.0"
    )

    implementation(
        "com.google.android.ump:user-messaging-platform:4.0.0"
    )

    implementation(
        "com.android.billingclient:billing:9.1.0"
    )

    implementation(
        "androidx.media3:media3-exoplayer:1.5.1"
    )

    implementation(
        "androidx.media3:media3-ui:1.5.1"
    )

    implementation(
        "com.badlogicgames.gdx:gdx:$gdxVersion"
    )

    implementation(
        "com.badlogicgames.gdx:gdx-backend-android:$gdxVersion"
    )

    implementation(
        "com.badlogicgames.gdx:gdx-freetype:$gdxVersion"
    )


    natives(
        "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a"
    )

    natives(
        "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a"
    )

    natives(
        "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64"
    )

    natives(
        "com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-arm64-v8a"
    )

    natives(
        "com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-armeabi-v7a"
    )

    natives(
        "com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-x86_64"
    )

    testImplementation(
        "junit:junit:4.13.2"
    )

    androidTestImplementation(
        "androidx.test.espresso:espresso-core:3.7.0"
    )

    androidTestImplementation(
        "androidx.test.ext:junit:1.3.0"
    )
}

// =============================================================================
// Android native libraries
// =============================================================================

val copyAndroidNatives by
    tasks.registering {
        doLast {
            natives.files.forEach { jar ->

                val abi =
                    when {
                        jar.name.contains(
                            "arm64-v8a"
                        ) ->
                            "arm64-v8a"

                        jar.name.contains(
                            "armeabi-v7a"
                        ) ->
                            "armeabi-v7a"

                        jar.name.contains(
                            "x86_64"
                        ) ->
                            "x86_64"

                        else ->
                            return@forEach
                    }

                copy {
                    from(
                        zipTree(
                            jar
                        )
                    ) {
                        include(
                            "*.so"
                        )
                    }

                    into(
                        layout.projectDirectory.dir(
                            "libs/$abi"
                        )
                    )
                }
            }
        }
    }

tasks
    .named(
        "preBuild"
    ).configure {
        dependsOn(
            copyAndroidNatives
        )
    }


// =============================================================================
// Production monetization verification
// =============================================================================
//
// Play upload guard:
//
// Release bundles must never silently ship with
// rewarded ads disabled.
//
// Debug / QA continue using Google's official
// sample IDs.
// =============================================================================

val verifyProductionMonetization by
    tasks.registering {

        group =
            "verification"

        description =
            "Checks publisher AdMob configuration before creating a Play release bundle."

        doLast {
            val appId =
                providers
                    .gradleProperty(
                        "productionAdmobAppId"
                    ).orNull
                    .orEmpty()

            val rewardedReviveId =
                providers
                    .gradleProperty(
                        "productionRewardedReviveAdUnitId"
                    ).orNull
                    .orEmpty()

            val rewardedTalismanId =
                providers
                    .gradleProperty(
                        "productionRewardedTalismanAdUnitId"
                    ).orNull
                    .orEmpty()

            val audienceReviewed =
                providers
                    .gradleProperty(
                        "productionAdsAudienceReviewed"
                    ).orNull ==
                    "true"

            check(
                appId.matches(
                    Regex(
                        "ca-app-pub-[0-9]{16}~[0-9]{10}"
                    )
                )
            ) {
                "Missing/invalid productionAdmobAppId. " +
                    "Configure your own AdMob App ID " +
                    "before Play release."
            }

            check(
                rewardedReviveId.matches(
                    Regex(
                        "ca-app-pub-[0-9]{16}/[0-9]{10}"
                    )
                )
            ) {
                "Missing/invalid productionRewardedReviveAdUnitId. " +
                    "Configure the Rewarded Revive Ad Unit before Play release."
            }

            check(
                rewardedTalismanId.matches(
                    Regex(
                        "ca-app-pub-[0-9]{16}/[0-9]{10}"
                    )
                )
            ) {
                "Missing/invalid productionRewardedTalismanAdUnitId. " +
                    "Configure the Rewarded Talisman Ad Unit before Play release."
            }

            check(
                !appId.contains(
                    "3940256099942544"
                ) &&
                    !rewardedReviveId.contains(
                        "3940256099942544"
                    ) &&
                    !rewardedTalismanId.contains(
                        "3940256099942544"
                    )
            ) {
                "Google sample/test AdMob IDs " +
                    "are forbidden in Release."
            }

            check(
                audienceReviewed
            ) {
                "Set productionAdsAudienceReviewed=true " +
                    "only after target-audience/consent " +
                    "configuration has been reviewed."
            }
        }
    }

tasks
    .matching {
        it.name == "bundleRelease" || it.name == "assembleRelease"
    }.configureEach {

        dependsOn(
            verifyProductionMonetization
        )
    }

// =============================================================================
// Production source-safety verification
// =============================================================================

// Resolve paths during configuration; Gradle 10 disallows Task.project from task actions.
val appVerificationDir = layout.projectDirectory.asFile
val rootVerificationDir = rootProject.layout.projectDirectory.asFile

val verifyProductionSourceSafety by tasks.registering {
    group = "verification"
    description = "Verifies that developer-mode bypasses are gated out of Release."

    doLast {
        val buildSource = appVerificationDir.resolve("build.gradle.kts").readText()
        check("buildConfigField(\"boolean\", \"DEVELOPER_ACCESS_ALLOWED\", \"false\")" in buildSource) {
            "Release must default to developer access disabled."
        }
        val developerSource = appVerificationDir.resolve("src/main/java/com/example/brick_breaker_ball/app/DevelopmentAccess.kt").readText()
        check(
            "BuildConfig.BUILD_TYPE.equals(\"qa\", ignoreCase = true)" in developerSource &&
                "BuildConfig.DEVELOPER_ACCESS_ALLOWED" in developerSource &&
                "private val available: Boolean = DEVELOPER_ACCESS && BuildConfig.DEVELOPER_ACCESS_ALLOWED" in developerSource &&
                "get() = available && prefs.getBoolean(ENABLED_KEY, false)" in developerSource &&
                "if (!available) return false" in developerSource
        ) {
            "Developer access must be gated by the build variant."
        }
        val buildTypesSource = buildSource.substringAfter("buildTypes {", "")
        val releaseBuildType = buildTypesSource.substringAfter("release {", "").substringBefore("// Production-like testing build.")
        check("buildConfigField(\"boolean\", \"DEVELOPER_ACCESS_ALLOWED\", \"false\")" in releaseBuildType) {
            "Release must explicitly disable developer access."
        }

        val qaBuildType = buildTypesSource.substringAfter("create(\"qa\") {", "").substringBefore("signingConfig")
        check("buildConfigField(\"boolean\", \"DEVELOPER_ACCESS_ALLOWED\", \"true\")" in qaBuildType) {
            "QA must explicitly enable developer access."
        }

        val menuSource = appVerificationDir.resolve("src/main/java/com/example/brick_breaker_ball/ui/UiScreens.kt").readText()
        val legacyVariantGatedButton =
            "val development = if (DevelopmentAccess.DEVELOPER_ACCESS && BuildConfig.DEVELOPER_ACCESS_ALLOWED) artButton(" in menuSource
        val explicitlyDisabledResponsiveButton =
            "development = null" in menuSource &&
                !Regex("""development\s*=\s*artButton\(""").containsMatchIn(menuSource)
        val guardedResponsiveButton =
            "val development = drawDeveloperAccessButton(" in menuSource &&
                "if (!DevelopmentAccess.DEVELOPER_ACCESS || !BuildConfig.DEVELOPER_ACCESS_ALLOWED) return null" in menuSource
        check(legacyVariantGatedButton || explicitlyDisabledResponsiveButton || guardedResponsiveButton) {
            "Developer-mode menu button must be hidden in Play Release."
        }

        val shopSource = appVerificationDir.resolve("src/main/java/com/example/brick_breaker_ball/ui/ShopScreen.kt").readText()
        check("if (game.developmentAccess.enabled) {" in shopSource) {
            "Developer test grants must require enabled developer access."
        }
    }
}

tasks
    .matching { it.name == "assembleRelease" || it.name == "bundleRelease" }
    .configureEach {
        dependsOn(verifyProductionSourceSafety)
    }

// =============================================================================
// Release signing verification
// =============================================================================

val verifyReleaseSigning by
    tasks.registering {
        group = "verification"
        description = "Fails production release tasks when the upload signing configuration is missing or incomplete."

        doLast {
            check(productionApplicationId.matches(Regex("[a-zA-Z][a-zA-Z0-9_]*(\\.[a-zA-Z][a-zA-Z0-9_]*){2,}"))) {
                "productionApplicationId must be a valid reverse-domain Android application ID."
            }
            check(!productionApplicationId.startsWith("com.example")) {
                "Set productionApplicationId to your permanent Play package ID before Release (for example com.yourbrand.brickbreakerball)."
            }
            val privacyUrl = providers.gradleProperty("privacyPolicyUrl").orNull.orEmpty()
            check(privacyUrl.startsWith("https://") && privacyUrl.length > "https://".length) {
                "privacyPolicyUrl must be a public HTTPS URL before Release."
            }
            check(keystorePropertiesFile.exists()) {
                "Missing keystore.properties. Configure the Play upload signing key before building Release."
            }

            val requiredKeys = listOf("storeFile", "storePassword", "keyAlias", "keyPassword")
            val missingKeys = requiredKeys.filter { keystoreProperties.getProperty(it).isNullOrBlank() }
            check(missingKeys.isEmpty()) {
                "keystore.properties is incomplete. Missing required signing fields: ${missingKeys.joinToString()}."
            }

            val configuredStore = rootVerificationDir.resolve(keystoreProperties.getProperty("storeFile"))
            check(configuredStore.isFile) {
                "The configured Release keystore file does not exist."
            }
        }
    }

tasks
    .matching { it.name == "assembleRelease" || it.name == "bundleRelease" }
    .configureEach {
        dependsOn(verifyReleaseSigning)
    }

// =============================================================================
// Formatting commands
// =============================================================================
//
// مباشر:
// .\gradlew.bat spotlessApply
//
// أو alias:
// .\gradlew.bat format
//
// فحص فقط:
// .\gradlew.bat spotlessCheck
// .\gradlew.bat formatCheck
// =============================================================================

tasks.register(
    "format"
) {
    group =
        "formatting"

    description =
        "Formats Kotlin and Gradle Kotlin DSL files using Spotless + ktlint."

    dependsOn(
        "spotlessApply"
    )
}

tasks.register(
    "formatCheck"
) {
    group =
        "verification"

    description =
        "Checks Kotlin formatting without modifying source files."

    dependsOn(
        "spotlessCheck"
    )
}

// Google Play Games v2 login + Firebase Auth MUST have a Web OAuth Client ID.
// This is a verification gate, not a substitute for console publishing, OAuth
// consent/provider setup, SHA-1 or signed-device validation.
val verifyPlayGamesFirebaseRelease by tasks.registering {
    group = "verification"
    description = "Checks Firebase Android app, Web OAuth setup, and Play Games resource before Release."
    doLast {
        val config = appVerificationDir.resolve("google-services.json")
        check(config.isFile) { "Missing app/google-services.json for Release." }
        val firebaseConfig = groovy.json.JsonSlurper().parse(config) as? Map<*, *>
            ?: error("Invalid Firebase configuration JSON")
        val firebaseProject = firebaseConfig["project_info"] as? Map<*, *>
        check(firebaseProject?.get("project_id") == expectedGameFirebaseProjectId) {
            "Release Firebase project must be $expectedGameFirebaseProjectId to accept the server's custom token."
        }
        val firebaseClients = (firebaseConfig["client"] as? List<*>).orEmpty()
            .mapNotNull { it as? Map<*, *> }
        val registeredPackages = firebaseClients.mapNotNull { client ->
            val info = client["client_info"] as? Map<*, *>
            (info?.get("android_client_info") as? Map<*, *>)?.get("package_name") as? String
        }.toSet()
        check(productionApplicationId in registeredPackages) {
            "Firebase google-services.json has no Android client for $productionApplicationId"
        }
        check(playGamesWebClientId == expectedPlayGamesServerClientId) {
            "Release must use the Play Games game-server Web OAuth client from Play Console, " +
                "not Firebase's default_web_client_id."
        }
        check(providers.gradleProperty("rewardsApiBaseUrl").orElse("").get().isNotBlank()) {
            "Release daily rewards require rewardsApiBaseUrl."
        }
        val pgsResource = appVerificationDir.resolve("src/main/res/values/strings.xml").readText()
        check(Regex("<string name=\"game_services_project_id\"[^>]*>6148685744</string>").containsMatchIn(pgsResource)) {
            "Play Games Services App ID must be 6148685744."
        }
    }
}

tasks.matching { it.name == "bundleRelease" || it.name == "assembleRelease" }.configureEach {
    dependsOn(verifyPlayGamesFirebaseRelease)
}

// QA must use its own registered Firebase applicationId; unlike Debug, QA
// now runs processQaGoogleServices and can initialize the Firebase SDK.
val verifyQaFirebaseConfig by tasks.registering {
    group = "verification"
    description = "Confirms the QA Firebase application is registered."
    doLast {
        val qaConfig = appVerificationDir.resolve("src/qa/google-services.json")
        check(qaConfig.isFile) { "Missing app/src/qa/google-services.json" }
        val expectedPackage = "$productionApplicationId.qa"
        val qaJson = groovy.json.JsonSlurper().parse(qaConfig) as? Map<*, *>
        val qaProject = qaJson?.get("project_info") as? Map<*, *>
        check(qaProject?.get("project_id") == expectedGameFirebaseProjectId) {
            "QA Firebase project must be $expectedGameFirebaseProjectId to accept the server's custom token."
        }
        val found = (qaJson?.get("client") as? List<*>).orEmpty().any { client ->
            val info = (client as? Map<*, *>)?.get("client_info") as? Map<*, *>
            (info?.get("android_client_info") as? Map<*, *>)?.get("package_name") == expectedPackage
        }
        check(found) { "Register $expectedPackage in Firebase and download its QA google-services.json" }
        check(playGamesWebClientId == expectedPlayGamesServerClientId) {
            "QA must request the Play Games game-server Web OAuth client; Firebase's Web client is not valid here."
        }
        check(providers.gradleProperty("rewardsApiBaseUrl").orElse("").get().isNotBlank()) {
            "QA daily rewards require rewardsApiBaseUrl."
        }
    }
}

tasks.matching { it.name == "preQaBuild" }.configureEach {
    dependsOn(verifyQaFirebaseConfig)
}

// =============================================================================
// Production UX / asset readiness guard
// =============================================================================

val verifyProductionReadiness by tasks.registering {
    group = "verification"
    description = "Fails QA/Release pre-build when critical production UX assets are missing or empty."

    doLast {
        val assetRoot = appVerificationDir.resolve("src/main/assets")
        val criticalAssets = buildList {
            add("backgrounds/start-screen/main-screen/main-screen-volley.png")
            add("backgrounds/start-screen/main-screen/main-screen-volley.mp4")
            add("backgrounds/start-screen/main-screen/main-screen-FINAIL.json")
            add("backgrounds/start-screen/main-screen/sound-off-btn-final.png")
            add("backgrounds/start-screen/main-screen/new-game-btn-final.png")
            add("backgrounds/start-screen/setting/sound-btn.png")
            add("backgrounds/start-screen/setting/sound-btn-off.png")
            add("backgrounds/worlds-map-images/star-on.png")
            add("backgrounds/worlds-map-images/star-off.png")
            add("backgrounds/splash.mp4")
            add("hazard/electric_floor_wire.png")
            add("atlases/particles.atlas")
            add("atlases/balls.atlas")
            add("atlases/volley.atlas")
            add("atlases/particles.png")
            add("atlases/balls.png")
            add("atlases/volley.png")
            add("ui/boards.json")
            add("ui/boards1.png")
            add("ui/boards2.json")
            add("ui/boards2.png")
            add("atlases/assets_sheet_atlas.atlas")
            add("atlases/assets_sheet_atlas.png")
            add("volley-orbs/orb-atlas.json")
            add("volley-orbs/orb-atlas.png")
            add("ui/volley_editor_atlas.json")
            add("ui/volley_editor_atlas.png")
            add("ui/volley_editor_controls.json")
            add("ui/volley_editor_controls.png")
            add("ui/ball_collection_buttons.json")
            add("ui/ball_collection_buttons.png")
            // Every literal OGG reference in the game's canonical audio catalog must exist.
            val soundCatalog = appVerificationDir.resolve("src/main/java/com/example/brick_breaker_ball/assets/GameAssets.kt").readText()
            Regex("audio/[A-Za-z0-9_-]+\\.ogg").findAll(soundCatalog).forEach { add(it.value) }
        }

        val missing = criticalAssets.filter { relative ->
            val file = assetRoot.resolve(relative)
            !file.isFile || file.length() <= 0L
        }
        check(missing.isEmpty()) {
            "Critical production assets are missing/empty:\n" + missing.joinToString("\n") { " - $it" }
        }

        val menuJson = assetRoot.resolve("backgrounds/start-screen/main-screen/main-screen-FINAIL.json").readText()
        listOf("info", "the-dimon-value", "the-dimon-pius-btn", "the-coins-value", "the-coins-pluse-btn", "music-btn", "settings-btn", "charms-bag", "shop", "continue&new-game-btn", "worlds-map", "balls", "level-editor-btn", "youtube", "share-my-app-btn", "developer-btn").forEach { key ->
            check(menuJson.contains("\"$key\"")) { "portrait main-screen-FINAIL.json is missing required frame '$key'" }
        }
    }
}

tasks.matching { it.name == "preQaBuild" || it.name == "preReleaseBuild" }.configureEach {
    dependsOn(verifyProductionReadiness)
}

// A Volley playtest changes its work when the selected stages, seeds, or descent scale change.
// Register those environment values so Gradle never reuses an old test result for a new run.
tasks.matching { it.name == "testDebugUnitTest" }.configureEach {
    listOf(
        "VOLLEY_PLAYTEST_ENABLED",
        "VOLLEY_PLAYTEST_FIRST",
        "VOLLEY_PLAYTEST_LAST",
        "VOLLEY_PLAYTEST_LEVELS",
        "VOLLEY_PLAYTEST_TRIALS",
        "VOLLEY_PLAYTEST_DESCENT_SCALE",
        "VOLLEY_PLAYTEST_OUTPUT"
    ).forEach { name ->
        inputs.property(name, providers.environmentVariable(name).orElse(""))
    }
}
