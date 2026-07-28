import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun reHealthApiBaseUrl(): String =
    (localProps.getProperty("rehealth.api.base.url") ?: System.getenv("REHEALTH_API_BASE_URL")
        ?: "http://10.0.2.2:8080/jeecg-boot/").trim().trimEnd('/') + "/"
fun reHealthReleaseApiBaseUrl(): String {
    val configured = localProps.getProperty("rehealth.release.api.base.url")
        ?: System.getenv("REHEALTH_RELEASE_API_BASE_URL")
        ?: "https://api.rehealth.invalid/"
    val normalized = configured.trim().trimEnd('/') + "/"
    require(normalized.startsWith("https://")) {
        "Release backend URL must use HTTPS (rehealth.release.api.base.url or REHEALTH_RELEASE_API_BASE_URL)"
    }
    return normalized
}
// JeecgBoot request-signing secret for endpoints that require the `X-Sign` header
// (e.g. /sys/sms). It must be supplied by local.properties or the environment.
fun signSecret(): String =
    (localProps.getProperty("JEECG_SIGNATURE_SECRET") ?: System.getenv("JEECG_SIGNATURE_SECRET")
        ?: "").trim()
fun debugWearableProductCode(): String {
    val normalizedProductCode = (
        providers.gradleProperty("rehealth.debug.wearable.product.code")
            .orNull
            ?: localProps.getProperty("rehealth.debug.wearable.product.code")
            ?: "RH-MRD-S01"
        ).trim()
    require(normalizedProductCode in setOf("RH-MRD-S01", "RH-RW-P01", "RH-HB-E01")) {
        "rehealth.debug.wearable.product.code must be RH-MRD-S01, RH-RW-P01, or RH-HB-E01"
    }
    return normalizedProductCode
}

android {
    namespace = "com.rehealth.genie"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.rehealth.genie"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "REHEALTH_API_BASE_URL", "\"${reHealthReleaseApiBaseUrl()}\"")
        manifestPlaceholders["usesCleartextTraffic"] = "false"
        // Provider credentials and request-signing secrets must never enter a release APK.
        buildConfigField("String", "JEECG_SIGN_SECRET", "\"\"")
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "USE_FAKE_RING", "false")
            buildConfigField("boolean", "SEED_FAKE_HEALTH_DATA", "false")
            buildConfigField("boolean", "ALLOW_WEARABLE_PRODUCT_SWITCH", "true")
            buildConfigField("String", "DEBUG_WEARABLE_PRODUCT_CODE", "\"${debugWearableProductCode()}\"")
            buildConfigField("String", "REHEALTH_API_BASE_URL", "\"${reHealthApiBaseUrl()}\"")
            buildConfigField("String", "JEECG_SIGN_SECRET", "\"${signSecret()}\"")
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        release {
            buildConfigField("boolean", "ALLOW_WEARABLE_PRODUCT_SWITCH", "false")
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(files("libs/sdk_mrd2026_1.3.0.aar"))
    implementation(files("libs/blesdk-rwfit-release_v2_260724.aar"))
    implementation(files("libs/vpbluetooth-1.20.aar"))
    implementation(files("libs/vpprotocol-2.3.73.15.aar"))
    // Required by VPOperateManager/JLOTAManager and Bluetooth authentication class signatures.
    // JL_Watch supplies WatchOpImpl during manager initialization; ReHealth does not expose OTA/dial APIs.
    implementation(files("libs/jl_bt_ota_V1.10.0_10931-release.aar"))
    implementation(files("libs/jl_rcsp_V0.7.2_527-release.aar"))
    implementation(files("libs/JL_Watch_V1.13.1_11214-release.aar"))

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.navigation:navigation-compose:2.8.5")

    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended:1.7.6")
    debugImplementation("androidx.compose.ui:ui-tooling")

    implementation("androidx.room:room-runtime:2.7.1")
    implementation("androidx.room:room-ktx:2.7.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    // Retrofit + Moshi: typed E1 mobile API client for /features/evaluate and risk/intervention retrieval.
    // Why: replace ad-hoc OkHttp/Gson string parsing with a typed DTO client aligned to MOBILE_API.md / API_CONTRACT.md.
    // Alternative: keep extending ReHealthBackendClient manually; rejected because feature-evaluate DTOs are complex (nested featureQuality map, snake_case) and error-prone to (de)serialize by hand.
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.moshi:moshi:1.15.1")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    // WorkManager: reserved for E2 durable telemetry upload queue. D1 does NOT wire it to a production telemetry worker; it is added now so that D1's lightweight feature-evaluate retry helper can reuse the same dependency surface later.
    implementation("androidx.work:work-runtime-ktx:2.10.0")
    testImplementation(kotlin("test"))
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    ksp("androidx.room:room-compiler:2.7.1")
}
