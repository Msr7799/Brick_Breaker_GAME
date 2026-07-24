import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val gdxVersion = "1.14.2"
val natives by configurations.creating
val texturePacker by configurations.creating
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) keystorePropertiesFile.inputStream().use(::load)
}

android {
    namespace = "com.example.brick_breaker_ball"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.brick_breaker_ball"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            if (keystorePropertiesFile.exists()) signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        viewBinding = false
    }
    sourceSets["main"].jniLibs.srcDir("libs")
}

dependencies {
    implementation("androidx.media3:media3-exoplayer:1.5.1")
    implementation("androidx.media3:media3-ui:1.5.1")
    implementation("com.badlogicgames.gdx:gdx:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-backend-android:$gdxVersion")
    implementation("com.badlogicgames.gdx:gdx-freetype:$gdxVersion")
    texturePacker("com.badlogicgames.gdx:gdx-tools:$gdxVersion")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-x86_64")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-freetype-platform:$gdxVersion:natives-x86_64")
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}

val copyAndroidNatives by tasks.registering {
    doLast {
        natives.files.forEach { jar ->
            val abi = when {
                jar.name.contains("arm64-v8a") -> "arm64-v8a"
                jar.name.contains("armeabi-v7a") -> "armeabi-v7a"
                jar.name.contains("x86_64") -> "x86_64"
                else -> return@forEach
            }
            copy {
                from(zipTree(jar)) { include("*.so") }
                into(layout.projectDirectory.dir("libs/$abi"))
            }
        }
    }
}

tasks.named("preBuild").configure { dependsOn(copyAndroidNatives) }

fun registerAtlas(name: String) = tasks.register<JavaExec>("pack${name.replaceFirstChar { it.uppercase() }}Atlas") {
    classpath = texturePacker
    mainClass.set("com.badlogic.gdx.tools.texturepacker.TexturePacker")
    args(
        rootProject.file("tools/assets_staging/selected/atlas_inputs/$name").absolutePath,
        project.file("src/main/assets/atlases").absolutePath,
        name,
        rootProject.file("tools/texturepacker-settings.json").absolutePath,
    )
}

// Gameplay sprites are sliced at runtime from the canonical detailed JSON map.
val atlasTasks = listOf("ui", "particles", "backgrounds").map(::registerAtlas)
val packGameAtlases = tasks.register("packGameAtlases") { dependsOn(atlasTasks) }
