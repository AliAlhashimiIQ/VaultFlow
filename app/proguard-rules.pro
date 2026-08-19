# ============================================================
# VaultFlow ProGuard/R8 Rules — Production Release
# ============================================================

# --- General Android & Kotlin ---
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Keep Kotlin Metadata for reflection
-keep class kotlin.Metadata { *; }
-dontwarn kotlin.**

# --- Room Database ---
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao class * { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}

# Keep Room entity constructors and fields
-keepclassmembers @androidx.room.Entity class * {
    <init>(...);
    <fields>;
}

# --- Compose ---
-dontwarn androidx.compose.**
-keep class androidx.compose.** { *; }

# --- Coroutines ---
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory { *; }
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler { *; }
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# --- AndroidX Lifecycle ---
-keepclassmembers class * extends androidx.lifecycle.ViewModel {
    <init>(...);
}
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    <init>(android.app.Application);
}

# --- JSON Parsing (org.json — standard Android, no rules needed) ---
# org.json is part of the Android framework and not subject to R8.

# --- FileProvider ---
-keep class androidx.core.content.FileProvider { *; }

# --- Prevent stripping of data classes used by Room ---
-keep class com.example.data.local.entity.** { *; }
-keep class com.example.data.local.dao.** { *; }
-keep class com.example.data.local.AppDatabase { *; }
-keep class com.example.data.repository.** { *; }

# --- Enums (keep for Room string matching and UI usage) ---
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
    public static ** entries;
}
