import java.net.URI
import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("com.google.devtools.ksp")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
fun reHealthApiBaseUrl(): String {
    val configured = localProps.getProperty("rehealth.api.base.url")
        ?: System.getenv("REHEALTH_API_BASE_URL")
        ?: providers.gradleProperty("rehealth.api.base.url").orNull
        ?: "http://10.0.2.2:8080/jeecg-boot/"
    return configured.trim().trimEnd('/') + "/"
}
fun reHealthReleaseApiBaseUrl(): String {
    val configured = localProps.getProperty("rehealth.release.api.base.url")
        ?: System.getenv("REHEALTH_RELEASE_API_BASE_URL")
        ?: providers.gradleProperty("rehealth.release.api.base.url").orNull
        ?: "https://api.rehealth.invalid/"
    val normalized = configured.trim().trimEnd('/') + "/"
    require(normalized.startsWith("https://")) {
        "Release backend URL must use HTTPS (rehealth.release.api.base.url or REHEALTH_RELEASE_API_BASE_URL)"
    }
    return normalized
}

val configuredReleaseApiBaseUrl = localProps.getProperty("rehealth.release.api.base.url")
    ?: System.getenv("REHEALTH_RELEASE_API_BASE_URL")
    ?: providers.gradleProperty("rehealth.release.api.base.url").orNull

val releaseKeystorePath = System.getenv("REHEALTH_RELEASE_KEYSTORE")?.trim()?.takeIf { it.isNotEmpty() }
val releaseKeystorePassword =
    System.getenv("REHEALTH_RELEASE_KEYSTORE_PASSWORD")?.takeIf { it.isNotEmpty() }
val releaseKeyAlias = System.getenv("REHEALTH_RELEASE_KEY_ALIAS")?.trim()?.takeIf { it.isNotEmpty() }
val releaseKeyPassword = System.getenv("REHEALTH_RELEASE_KEY_PASSWORD")?.takeIf { it.isNotEmpty() }
val hasCompleteReleaseSigningConfig = listOf(
    releaseKeystorePath,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { it != null }

fun validateReleaseApiBaseUrl(configured: String?): List<String> {
    if (configured.isNullOrBlank()) {
        return listOf(
            "Set rehealth.release.api.base.url or REHEALTH_RELEASE_API_BASE_URL to the production HTTPS endpoint.",
        )
    }
    val uri = runCatching { URI(configured.trim()) }.getOrNull()
        ?: return listOf("Release backend URL is not a valid URI.")
    val host = uri.host?.lowercase().orEmpty()
    val privateIpv4 = host.startsWith("10.") || host.startsWith("192.168.") ||
        host.startsWith("169.254.") || host.startsWith("100.64.") ||
        Regex("^172\\.(1[6-9]|2[0-9]|3[01])\\.").containsMatchIn(host)
    val privateIpv6 = host == "::1" || host.startsWith("fc") || host.startsWith("fd") ||
        host.startsWith("fe80:")
    val documentationHost = host == "example.com" || host.endsWith(".example.com") ||
        host == "example.org" || host.endsWith(".example.org") ||
        host == "example.net" || host.endsWith(".example.net")
    return buildList {
        if (uri.scheme?.lowercase() != "https") add("Release backend URL must use HTTPS.")
        if (uri.userInfo != null) add("Release backend URL must not contain credentials.")
        if (uri.query != null || uri.fragment != null) {
            add("Release backend URL must not contain a query or fragment.")
        }
        if (
            host.isBlank() || host == "localhost" || host == "0.0.0.0" || host == "127.0.0.1" ||
            host == "10.0.2.2" || host.endsWith(".local") || host.endsWith(".invalid") ||
            privateIpv4 || privateIpv6 || documentationHost
        ) {
            add("Release backend URL must resolve to a production host, not a local/private/placeholder host.")
        }
        if (host in setOf("rehealth.youngjimmy.store", "rehealth.47.80.30.228.sslip.io")) {
            add("The public development tunnel is not an approved production Release endpoint.")
        }
    }
}
// JeecgBoot request-signing secret for endpoints that require the `X-Sign` header
// (e.g. /sys/sms). Debug falls back to JeecgBoot's checked-in development default;
// release does not define or reference this BuildConfig field.
fun signSecret(): String =
    (localProps.getProperty("JEECG_SIGNATURE_SECRET") ?: System.getenv("JEECG_SIGNATURE_SECRET")
        ?: "dd05f1c54d63749eda95f9fa6d49v442a").trim()
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
// Debug-only ring simulation switches. Default off so real-device BLE QA is unaffected.
// Override per-run via -Prehealth.debug.use.fake.ring=true or local.properties /
// gradle.properties. SEED_FAKE_HEALTH_DATA only auto-activates on recognized emulators.
fun useFakeRing(): Boolean =
    (localProps.getProperty("rehealth.debug.use.fake.ring")
        ?: providers.gradleProperty("rehealth.debug.use.fake.ring").orNull
        ?: "false").toBooleanStrict()
fun seedFakeHealthData(): Boolean =
    (localProps.getProperty("rehealth.debug.seed.fake.health.data")
        ?: providers.gradleProperty("rehealth.debug.seed.fake.health.data").orNull
        ?: "false").toBooleanStrict()

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
    }

    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
    }

    val releaseSigningConfig = if (hasCompleteReleaseSigningConfig) {
        signingConfigs.create("release") {
            storeFile = file(releaseKeystorePath!!)
            storePassword = releaseKeystorePassword
            keyAlias = releaseKeyAlias
            keyPassword = releaseKeyPassword
        }
    } else {
        null
    }

    buildTypes {
        debug {
            buildConfigField("boolean", "USE_FAKE_RING", useFakeRing().toString())
            buildConfigField("boolean", "SEED_FAKE_HEALTH_DATA", seedFakeHealthData().toString())
            buildConfigField("boolean", "ALLOW_WEARABLE_PRODUCT_SWITCH", "true")
            buildConfigField("String", "DEBUG_WEARABLE_PRODUCT_CODE", "\"${debugWearableProductCode()}\"")
            buildConfigField("String", "REHEALTH_API_BASE_URL", "\"${reHealthApiBaseUrl()}\"")
            buildConfigField("String", "JEECG_SIGN_SECRET", "\"${signSecret()}\"")
            buildConfigField("String", "SMS_TEST_CODE", "\"123456\"")
            manifestPlaceholders["usesCleartextTraffic"] = "true"
        }
        release {
            buildConfigField("boolean", "ALLOW_WEARABLE_PRODUCT_SWITCH", "false")
            isMinifyEnabled = true
            signingConfig = releaseSigningConfig
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
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
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
    // VPOperateManager releases its JieLi watch-face stack from every disconnect callback.
    // These official runtime companions are therefore required even though ReHealth does not expose dials/OTA.
    implementation(files("libs/BmpConvert_V1.6.0_10604-release.aar"))
    implementation(files("libs/abpartool-release.aar"))
    // VPOperateManager initializes its Nordic OTA adapter from the BLE connection callback,
    // even though ReHealth does not expose OTA. These are mandatory runtime dependencies.
    implementation("no.nordicsemi.android:mcumgr-core:2.7.4")
    implementation("no.nordicsemi.android:mcumgr-ble:2.7.4")
    implementation("no.nordicsemi.android.support.v18:scanner:1.4.2")

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
    testImplementation("org.json:json:20250107")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.room:room-testing:2.7.1")
    ksp("androidx.room:room-compiler:2.7.1")
}

val validateReleaseConfiguration by tasks.registering {
    group = "verification"
    description = "Fails closed unless the production API and formal Android signing inputs are configured."
    doLast {
        val errors = validateReleaseApiBaseUrl(configuredReleaseApiBaseUrl).toMutableList()
        if (!hasCompleteReleaseSigningConfig) {
            errors += "Set REHEALTH_RELEASE_KEYSTORE, REHEALTH_RELEASE_KEYSTORE_PASSWORD, " +
                "REHEALTH_RELEASE_KEY_ALIAS, and REHEALTH_RELEASE_KEY_PASSWORD."
        } else if (!file(releaseKeystorePath!!).isFile) {
            errors += "REHEALTH_RELEASE_KEYSTORE does not point to an existing file."
        }
        if (errors.isNotEmpty()) {
            throw GradleException("Release configuration is incomplete:\n- " + errors.joinToString("\n- "))
        }
    }
}

tasks.configureEach {
    if (name in setOf("assembleRelease", "bundleRelease", "packageRelease")) {
        dependsOn(validateReleaseConfiguration)
    }
}
