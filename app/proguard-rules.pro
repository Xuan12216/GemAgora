# ProGuard / R8 Rules for GemAgora

# 1. Google LiteRT-LM & MediaPipe GenAI
-keep class com.google.ai.edge.litertlm.** { *; }
-keep interface com.google.ai.edge.litertlm.** { *; }
-keep class com.google.ai.edge.litert.** { *; }
-keep class com.google.mediapipe.** { *; }
-keepclasseswithmembernames class * {
    native <methods>;
}

# 2. Room Database & Data Models
-keep class androidx.room.** { *; }
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class com.example.gemagora.data.local.** { *; }
-keep class com.example.gemagora.data.model.** { *; }
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }

# 3. DataStore & Coroutines
-keep class androidx.datastore.** { *; }
-keep class kotlinx.coroutines.** { *; }

# 4. Compose & Haze Frosting Library
-keep class androidx.compose.** { *; }
-keep class dev.chrisbanes.haze.** { *; }
