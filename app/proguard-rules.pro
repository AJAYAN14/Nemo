# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Room annotations
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Keep Supabase & Ktor model classes (for Serialization)
-keepattributes *Annotation*, Signature, Exception
-keep class kotlinx.serialization.** { *; }
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keep class com.jian.nemo.core.data.** { *; }

# WorkManager
-keep class androidx.work.Worker { *; }
-keep class * extends androidx.work.Worker { *; }

# Hilt & Dagger
-keep class dagger.hilt.** { *; }
-keep class com.jian.nemo.** {
    @javax.inject.Inject *;
    @dagger.hilt.android.AndroidEntryPoint *;
}

# Preserve stack traces
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# Remove Android Logging and StackTraces in Release build
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
    public static int wtf(...);
}

-assumenosideeffects class java.lang.Throwable {
    public void printStackTrace();
    public void printStackTrace(java.io.PrintStream);
    public void printStackTrace(java.io.PrintWriter);
}

# Keep Compose SharedTransition
-keep class androidx.compose.animation.** { *; }
-dontwarn androidx.compose.animation.**