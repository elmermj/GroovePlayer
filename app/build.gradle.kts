plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
    id("kotlin-kapt")
}

// Crashlytics / Google Services — only when Elmer drops app/google-services.json.
val googleServicesJson = file("google-services.json")
val hasGoogleServices = googleServicesJson.exists()
if (hasGoogleServices) {
    apply(plugin = "com.google.gms.google-services")
    apply(plugin = "com.google.firebase.crashlytics")
}


// Parse local.properties without java.util.Properties (Gradle script classpath quirk).
val localPropMap: Map<String, String> = buildMap {
    val f = rootProject.file("local.properties")
    if (f.exists()) {
        f.readLines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#") || "=" !in trimmed) return@forEach
            val idx = trimmed.indexOf('=')
            put(trimmed.substring(0, idx).trim(), trimmed.substring(idx + 1).trim())
        }
    }
}

fun escapeBuildConfig(raw: String): String =
    raw.replace("\\", "\\\\").replace("\"", "\\\"")

fun localProp(key: String, default: String): String =
    escapeBuildConfig(localPropMap[key] ?: default)

/** Prefer CI env, then local.properties, then default. Never commit secrets. */
fun secretProp(key: String, default: String): String {
    val fromEnv = System.getenv(key)?.trim()?.takeIf { it.isNotEmpty() }
    val fromLocal = localPropMap[key]?.trim()?.takeIf { it.isNotEmpty() }
    return escapeBuildConfig(fromEnv ?: fromLocal ?: default)
}

/** Raw (unescaped) secret: CI env first, then local.properties. Empty if unset. */
fun rawSecret(key: String): String {
    val fromEnv = System.getenv(key)?.trim()?.takeIf { it.isNotEmpty() }
    val fromLocal = localPropMap[key]?.trim()?.takeIf { it.isNotEmpty() }
    return fromEnv ?: fromLocal ?: ""
}

val PROD_API_DEFAULT = "https://grooveplayer-backend.fly.dev"

val releaseStoreFile = rawSecret("RELEASE_STORE_FILE")
val releaseStorePassword = rawSecret("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = rawSecret("RELEASE_KEY_ALIAS")
val releaseKeyPassword = rawSecret("RELEASE_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword
).all { it.isNotEmpty() }

val prodAdmobAppId = rawSecret("ADMOB_APP_ID")
val prodAdmobBanner = rawSecret("ADMOB_BANNER_UNIT_ID")
val prodAdmobInterstitial = rawSecret("ADMOB_INTERSTITIAL_UNIT_ID")
val hasProdAdMob = listOf(prodAdmobAppId, prodAdmobBanner, prodAdmobInterstitial).all { it.isNotEmpty() }


// Google sample / test AdMob IDs (safe for debug / local compile).
val ADMOB_TEST_APP_ID = "ca-app-pub-3940256099942544~3347511713"
val ADMOB_TEST_BANNER_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
val ADMOB_TEST_INTERSTITIAL_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"

/** Command-line -P, then VERSION_* env, then local defaults. App Tester reads the APK. */
fun projectProp(name: String): String? =
    findProperty(name)?.toString()?.trim()?.takeIf { it.isNotEmpty() }

val resolvedVersionCode: Int =
    projectProp("versionCode")?.toIntOrNull()
        ?: System.getenv("VERSION_CODE")?.trim()?.takeIf { it.isNotEmpty() }?.toIntOrNull()
        ?: 1

val resolvedVersionName: String =
    projectProp("versionName")
        ?: System.getenv("VERSION_NAME")?.trim()?.takeIf { it.isNotEmpty() }
        ?: "1.0"

android {
    namespace = "com.aethelsoft.grooveplayer"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aethelsoft.grooveplayer"
        minSdk = 24
        targetSdk = 36
        versionCode = resolvedVersionCode
        versionName = resolvedVersionName
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }

        // Web client ID is required as serverClientId when requesting Google ID tokens.
        // Shared across flavors (same applicationId / SHA-1 registration).
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"${localProp(
                "GOOGLE_WEB_CLIENT_ID",
                "356328665268-h7p2ct3o1k8358itiecp3h6o4r1tjjgo.apps.googleusercontent.com"
            )}\""
        )
        buildConfigField(
            "String",
            "GOOGLE_ANDROID_CLIENT_ID",
            "\"${localProp(
                "GOOGLE_ANDROID_CLIENT_ID",
                "356328665268-1t80fc2j091cei383co7tncernl4p00c.apps.googleusercontent.com"
            )}\""
        )
    }

    // environment: day-to-day `dev` vs store-oriented `prod`.
    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            // Android Studio / matchingFallbacks default day-to-day variant
            isDefault = true

            // Local / LAN API — override with API_BASE_URL in local.properties
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${localProp("API_BASE_URL", "http://10.0.2.2:8080")}\""
            )
            // Always Google sample/test AdMob IDs (never prod units in dev)
            buildConfigField("String", "ADMOB_BANNER_UNIT_ID", "\"$ADMOB_TEST_BANNER_UNIT_ID\"")
            buildConfigField(
                "String",
                "ADMOB_INTERSTITIAL_UNIT_ID",
                "\"$ADMOB_TEST_INTERSTITIAL_UNIT_ID\""
            )
            manifestPlaceholders["admobAppId"] = ADMOB_TEST_APP_ID
            buildConfigField("boolean", "ALLOW_CLEARTEXT", "true")
            resValue("string", "flavor_environment", "dev")
        }
        create("prod") {
            dimension = "environment"

            // Fly.io production API — override with PROD_API_BASE_URL in local.properties / CI
            buildConfigField(
                "String",
                "API_BASE_URL",
                "\"${secretProp("PROD_API_BASE_URL", PROD_API_DEFAULT)}\""
            )
            // Real AdMob only — never fall back to Google sample IDs (prodRelease fails if unset).
            buildConfigField(
                "String",
                "ADMOB_BANNER_UNIT_ID",
                "\"${escapeBuildConfig(prodAdmobBanner)}\""
            )
            buildConfigField(
                "String",
                "ADMOB_INTERSTITIAL_UNIT_ID",
                "\"${escapeBuildConfig(prodAdmobInterstitial)}\""
            )
            // Empty placeholder keeps Android Studio sync working; prodRelease still fails closed.
            manifestPlaceholders["admobAppId"] =
                escapeBuildConfig(prodAdmobAppId.ifEmpty { "ca-app-pub-0000000000000000~0000000000" })
            buildConfigField("boolean", "ALLOW_CLEARTEXT", "false")
            resValue("string", "flavor_environment", "prod")
        }
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                // Path is relative to the project root (same folder as local.properties).
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            // default debug signing
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Play upload signing from local.properties / CI.
            // Never fall back to the debug keystore for release.
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

kapt {
    correctErrorTypes = true
}

dependencies {

    // Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.foundation.layout)
    implementation(libs.androidx.compose.ui.unit)
    implementation(libs.androidx.ui.text.google.fonts)

    // Navigation
    implementation(libs.navigation.compose)

    // Media
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.datasource)
    implementation(libs.androidx.media3.database)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    implementation(libs.lottie.compose)

    // Hilt
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.text)
    implementation(libs.ui)
    implementation(libs.androidx.compose.runtime)
    implementation(libs.gms.play.services.nearby)
    implementation(libs.androidx.compose.ui.ui)
    kapt(libs.hilt.compiler)

    // Room (KSP OK)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.common.jvm)
    ksp(libs.androidx.room.compiler)

    // Coil
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)

    // Paging
    implementation(libs.androidx.paging.runtime)
    implementation(libs.androidx.paging.compose)

    // Audio tag editing (MP3, M4A, FLAC, OGG, etc.)
    implementation(libs.jaudiotagger)

    // Nearby Connections (P2P transfer without internet)
    implementation(libs.play.services.nearby)

    // Google Sign-In (Credential Manager + Google ID)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    // Auth API client
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.androidx.security.crypto)

    // AdMob (tier-gated; free tier only)
    implementation(libs.play.services.ads)
    // UMP (EEA/UK consent) — request before MobileAds.initialize
    implementation(libs.user.messaging.platform)

    // Google Play Billing (subscriptions + storage add-ons)
    implementation(libs.billing.ktx)

    // Firebase Crashlytics — only when app/google-services.json exists.
    if (hasGoogleServices) {
        implementation(platform(libs.firebase.bom))
        implementation(libs.firebase.crashlytics)
        implementation(libs.firebase.analytics)
    }

    // Misc
    implementation(libs.icons.lucide)
    implementation(libs.javax.inject)
    implementation(libs.androidx.palette.ktx)
    implementation(libs.androidx.constraintlayout)

    // Tests
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}


// Fail closed for Play-bound builds: no sample AdMob IDs, no debug-signed release.
gradle.taskGraph.whenReady {
    val runningProdRelease = allTasks.any { task ->
        val n = task.name
        n.contains("ProdRelease", ignoreCase = true) ||
            (n.contains("prod", ignoreCase = true) && n.contains("Release", ignoreCase = true) &&
                (n.startsWith("assemble") || n.startsWith("bundle") || n.startsWith("publish")))
    }
    if (!runningProdRelease) return@whenReady
    if (!hasProdAdMob) {
        throw GradleException(
            "prodRelease requires real AdMob IDs. Set ADMOB_APP_ID, ADMOB_BANNER_UNIT_ID, " +
                "and ADMOB_INTERSTITIAL_UNIT_ID in local.properties (or CI env). " +
                "Google sample IDs are not allowed for prod."
        )
    }
    if (!hasReleaseSigning) {
        throw GradleException(
            "prodRelease requires a Play upload keystore. Set RELEASE_STORE_FILE, " +
                "RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, and RELEASE_KEY_PASSWORD in " +
                "local.properties (or CI env). Do not use the debug keystore."
        )
    }
}
