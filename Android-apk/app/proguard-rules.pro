# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep all methods in PhmService implementations
-keep class com.rehealth.genie.phm.** { *; }
-keepclassmembers class com.rehealth.genie.phm.** { *; }

# Keep Mock methods specifically
-keep class com.rehealth.genie.phm.RemotePhmService {
    private *** generateMockRiskResult(...);
    private *** generateMockAttributionResult(...);
}

# Keep HealthChatService and its methods
-keep class com.rehealth.genie.network.HealthChatService { *; }
-keepclassmembers class com.rehealth.genie.network.HealthChatService { *; }

# Keep all network services
-keep class com.rehealth.genie.network.** { *; }

# Keep Retrofit interfaces
-keepattributes Signature
-keepattributes *Annotation*
-keep class retrofit2.** { *; }
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}

# Keep Gson classes
-keep class com.google.gson.** { *; }
-keep class * implements com.google.gson.TypeAdapterFactory
-keep class * implements com.google.gson.JsonSerializer
-keep class * implements com.google.gson.JsonDeserializer

# Keep all DTO classes
-keep class com.rehealth.genie.network.dto.** { *; }
-keepclassmembers class com.rehealth.genie.network.dto.** { *; }

# Keep all model classes
-keep class com.rehealth.genie.phm.PhmModels** { *; }
-keep class com.rehealth.genie.ring.RingModels** { *; }

# MRD SDK uses runtime model lookup and BLE callbacks that are not visible to R8.
-keep class com.manridy.** { *; }

# Release builds must not emit identifiers, raw BLE frames, or health values via logcat.
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
    public static *** e(...);
    public static *** wtf(...);
}

# OkHttp platform used only on JVM and when Conscrypt dependency is available.
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
