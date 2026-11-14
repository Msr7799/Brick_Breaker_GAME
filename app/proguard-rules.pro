# BrickBreakerBall R8 / ProGuard rules
#
# Release + QA use R8. Keep this file focused: broad keep rules would defeat
# shrinking and obfuscation.

# Keep useful source/line metadata so Play Console stack traces can be retraced
# accurately with app/build/outputs/mapping/release/mapping.txt.
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# libGDX official guidance: harmless when AndroidFragmentApplication is not used,
# and avoids optional-backend warnings during shrinking.
-dontwarn com.badlogic.gdx.backends.android.AndroidFragmentApplication

# Android's default optimized rules already preserve classes/method names that
# contain native methods for JNI. This explicit rule documents the dependency
# and protects libGDX/freetype JNI entry points if defaults change.
-keepclasseswithmembernames,includedescriptorclasses class * {
    native <methods>;
}

# libGDX Json serializes/deserializes these classes reflectively. Their field
# names are also part of the on-device custom-level JSON format, so renaming or
# removing members would break existing saved custom levels. Keeping only these
# two data-model classes is intentionally narrow.
-keep,allowoptimization class com.example.brick_breaker_ball.StoredCustomLevel { *; }
-keep,allowoptimization class com.example.brick_breaker_ball.LevelDefinition { *; }

# Room loads WorkManager's generated database by name and calls its no-arg
# constructor. The QA R8 build crashed during InitializationProvider startup
# while trying to create this class, before the game Activity could run.
-keep class androidx.work.impl.WorkDatabase_Impl { <init>(); }
