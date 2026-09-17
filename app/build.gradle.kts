import java.util.Properties
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

plugins {
    id("com.android.application")

    // Kotlin formatter
    id("com.diffplug.spotless") version "8.10.2"
}

val gdxVersion = "1.14.2"
val productionApplicationId =
    providers.gradleProperty("productionApplicationId").orElse("com.forgepulse.brickbreakerball").get()

val natives by configurations.creating
val texturePacker by configurations.creating

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
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        val privacyPolicyUrl = providers.gradleProperty("privacyPolicyUrl").orElse("").get()
        val escapedPrivacyPolicyUrl = privacyPolicyUrl.replace("\\", "\\\\").replace("\"", "\\\"")
        buildConfigField("String", "PRIVACY_POLICY_URL", "\"$escapedPrivacyPolicyUrl\"")
        buildConfigField("boolean", "DEVELOPER_ACCESS_ALLOWED", "false")

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

abstract class GenerateGameRulesAsset : DefaultTask() {
    @get:InputFile
    abstract val sourceFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun generate() {
        val destination = outputDirectory.get().asFile.resolve("rules/BrickBreakerBall_Game_Rules_EN.md")
        destination.parentFile.mkdirs()
        sourceFile.get().asFile.copyTo(destination, overwrite = true)
    }
}

val generateGameRulesAsset by tasks.registering(GenerateGameRulesAsset::class) {
    sourceFile.set(rootProject.layout.projectDirectory.file("app/src/main/BrickBreakerBall_Game_Rules_EN.md"))
    outputDirectory.set(layout.buildDirectory.dir("generated/game-rules/assets"))
}

androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(generateGameRulesAsset) { it.outputDirectory }
    }
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

    texturePacker(
        "com.badlogicgames.gdx:gdx-tools:$gdxVersion"
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
// LibGDX TexturePacker
// =============================================================================

fun registerAtlas(
    name: String
) = tasks.register<JavaExec>(
    "pack${
        name.replaceFirstChar {
            it.uppercase()
        }
    }Atlas"
) {
    classpath =
        texturePacker

    mainClass.set(
        "com.badlogic.gdx.tools.texturepacker.TexturePacker"
    )

    args(
        rootProject
            .file(
                "tools/assets_staging/selected/atlas_inputs/$name"
            ).absolutePath,

        project
            .file(
                "src/main/assets/atlases"
            ).absolutePath,

        name,

        rootProject
            .file(
                "tools/texturepacker-settings.json"
            ).absolutePath
    )
}

// Gameplay sprites are sliced at runtime from
// the canonical detailed JSON map.

val atlasTasks =
    listOf(
        "ui",
        "particles"
    ).map(
        ::registerAtlas
    )

val packGameAtlases =
    tasks.register(
        "packGameAtlases"
    ) {
        dependsOn(
            atlasTasks
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

val verifyProductionSourceSafety by tasks.registering {
    group = "verification"
    description = "Verifies that developer-mode bypasses are gated out of Release."

    doLast {
        val buildSource = project.file("build.gradle.kts").readText()
        check("buildConfigField(\"boolean\", \"DEVELOPER_ACCESS_ALLOWED\", \"false\")" in buildSource) {
            "Release must default to developer access disabled."
        }
        val developerSource = project.file("src/main/java/com/example/brick_breaker_ball/DevelopmentAccess.kt").readText()
        check("const val DEVELOPER_ACCESS = " in developerSource &&
            "private val available: Boolean = DEVELOPER_ACCESS && BuildConfig.DEVELOPER_ACCESS_ALLOWED" in developerSource &&
            "get() = available && prefs.getBoolean(ENABLED_KEY, false)" in developerSource &&
            "if (!available) return false" in developerSource) {
            "Developer access must be gated by the build variant."
        }

        val menuSource = project.file("src/main/java/com/example/brick_breaker_ball/UiScreens.kt").readText()
        check("val development = if (DevelopmentAccess.DEVELOPER_ACCESS && BuildConfig.DEVELOPER_ACCESS_ALLOWED) artButton(" in menuSource) {
            "Developer-mode menu button must be hidden in Play Release."
        }

        val shopSource = project.file("src/main/java/com/example/brick_breaker_ball/ShopScreen.kt").readText()
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

            val configuredStore = rootProject.file(keystoreProperties.getProperty("storeFile"))
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
