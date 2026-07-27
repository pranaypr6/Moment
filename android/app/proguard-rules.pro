# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Strip all Log statements in Release builds
-assumenosideeffects class android.util.Log {
    public static boolean isLoggable(java.lang.String, int);
    public static int v(...);
    public static int i(...);
    public static int w(...);
    public static int d(...);
    public static int e(...);
}

# Retrofit/OkHttp/Gson reflection safety net. Most Retrofit DTOs in this project are
# already annotated @Keep, but generic response wrappers (e.g. PaginatedResponse<T>) rely
# on generic type signatures surviving obfuscation for Gson's TypeToken-based
# deserialization to work correctly in release builds. Without these, generic responses can
# silently deserialize wrong or throw only in release, never in debug.
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes Exceptions
-keepattributes EnclosingMethod
-keepattributes InnerClasses

# Room entities/DAOs use reflection-adjacent codegen; keep entity fields intact.
-keep class com.pranayburra.moment.data.local.** { *; }

# Prevent R8 from stripping generic signatures of Kotlin Continuations,
# which causes "java.lang.Class cannot be cast to java.lang.reflect.ParameterizedType"
# when Retrofit parses suspend functions.
-keep class kotlin.coroutines.Continuation { *; }

-keep class retrofit2.** { *; }

# Keep Retrofit API interfaces and DTOs intact
-keep class com.pranayburra.moment.data.remote.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
