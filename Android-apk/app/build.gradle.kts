import java.util.Properties
import java.net.URI
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
// JeecgBoot request-signing secret for endpoints that require the `X-Sign` header
// (e.g. /sys/sms). It is intentionally local-only, including for Debug builds.
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

fun releaseVersionCode(): Int =
    (localProps.getProperty("rehealth.version.code")
        ?: System.getenv("REHEALTH_VERSION_CODE")
        ?: providers.gradleProperty("rehealth.version.code").orNull
        ?: "2").toInt()

fun releaseVersionName(): String =
    (localProps.getProperty("rehealth.version.name")
        ?: System.getenv("REHEALTH_VERSION_NAME")
        ?: providers.gradleProperty("rehealth.version.name").orNull
        ?: "1.0.1").trim()

fun releaseSigningValue(propertyName: String, environmentName: String): String? =
    (localProps.getProperty(propertyName)
        ?: System.getenv(environmentName)
        ?: providers.gradleProperty(propertyName).orNull)
        ?.trim()
        ?.takeIf(String::isNotEmpty)

val releaseStoreFile = releaseSigningValue("rehealth.release.store.file", "REHEALTH_RELEASE_STORE_FILE")
val releaseStorePassword = releaseSigningValue("rehealth.release.store.password", "REHEALTH_RELEASE_STORE_PASSWORD")
val releaseKeyAlias = releaseSigningValue("rehealth.release.key.alias", "REHEALTH_RELEASE_KEY_ALIAS")
val releaseKeyPassword = releaseSigningValue("rehealth.release.key.password", "REHEALTH_RELEASE_KEY_PASSWORD")
val releaseSigningValues = listOf(releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
require(releaseSigningValues.all { it == null } || releaseSigningValues.all { it != null }) {
    "Release signing requires store file, store password, key alias, and key password together."
}
val releaseSigningReady = releaseSigningValues.all { it != null }

android {
    namespace = "com.rehealth.genie"
    compileSdk = 36
    buildToolsVersion = "36.0.0"

    defaultConfig {
        applicationId = "com.rehealth.genie"
        minSdk = 26
        targetSdk = 36
        versionCode = releaseVersionCode()
        versionName = releaseVersionName()

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "REHEALTH_API_BASE_URL", "\"${reHealthReleaseApiBaseUrl()}\"")
        manifestPlaceholders["usesCleartextTraffic"] = "false"
        // Provider credentials and request-signing secrets must never enter a release APK.
        buildConfigField("String", "JEECG_SIGN_SECRET", "\"\"")
        buildConfigField("String", "SMS_TEST_CODE", "\"\"")
        // Main code may reference these fields, but only Debug is allowed to override them.
        buildConfigField("boolean", "USE_FAKE_RING", "false")
        buildConfigField("boolean", "SEED_FAKE_HEALTH_DATA", "false")
        buildConfigField("boolean", "ALLOW_WEARABLE_PRODUCT_SWITCH", "true")
    }

    signingConfigs {
        if (releaseSigningReady) {
            create("release") {
                storeFile = rootProject.file(requireNotNull(releaseStoreFile))
                storePassword = requireNotNull(releaseStorePassword)
                keyAlias = requireNotNull(releaseKeyAlias)
                keyPassword = requireNotNull(releaseKeyPassword)
            }
        }
    }

    sourceSets {
        getByName("androidTest").assets.srcDir("$projectDir/schemas")
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
            signingConfig = signingConfigs.findByName("release")
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

    lint {
        // AGP 8.10.1 can load the Compose detector without its generated helper class
        // after a clean build, crashing lint before it reports project findings.
        // Keep all other Release checks enabled until the toolchain is upgraded together.
        disable += "MutableCollectionMutableState"
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

val verifyReleaseConfiguration by tasks.registering {
    group = "verification"
    description = "Fails when the Release backend is not a real HTTPS endpoint."
    doLast {
        val releaseUrl = reHealthReleaseApiBaseUrl()
        val host = URI(releaseUrl).host.orEmpty().lowercase()
        require(host.isNotBlank() && !host.endsWith(".invalid")) {
            "Release backend URL must be an explicit non-placeholder HTTPS endpoint."
        }
    }
}

val verifyPublishConfiguration by tasks.registering {
    group = "verification"
    description = "Checks Release endpoint and signing inputs before publishing."
    dependsOn(verifyReleaseConfiguration)
    doLast {
        require(releaseSigningReady) {
            "Publishing requires external Release signing credentials; no keystore is committed to Git."
        }
        require(rootProject.file(requireNotNull(releaseStoreFile)).isFile) {
            "Configured Release keystore does not exist."
        }
    }
}

tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    dependsOn(verifyReleaseConfiguration)
}
