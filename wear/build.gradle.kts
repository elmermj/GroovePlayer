plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val localPropMap: Map<String, String> = buildMap {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.readLines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#") || "=" !in trimmed) return@forEach
            val idx = trimmed.indexOf('=')
            put(trimmed.substring(0, idx).trim(), trimmed.substring(idx + 1).trim())
        }
    }
}

fun projectProp(name: String): String? =
    findProperty(name)?.toString()?.trim()?.takeIf { it.isNotEmpty() }

fun rawSecret(key: String): String {
    val fromEnv = System.getenv(key)?.trim()?.takeIf { it.isNotEmpty() }
    val fromLocal = localPropMap[key]?.trim()?.takeIf { it.isNotEmpty() }
    return fromEnv ?: fromLocal ?: ""
}

/** Same rules as :app, so a dev build with GROOVE_ENV=staging gets the .staging id. */
fun grooveEnv(): String {
    val explicit = projectProp("GROOVE_ENV")
        ?: System.getenv("GROOVE_ENV")?.trim()?.takeIf { it.isNotEmpty() }
    if (explicit != null) return explicit.lowercase()
    if (System.getenv("GITHUB_ACTIONS") == "true") return ""
    return localPropMap["GROOVE_ENV"]?.trim()?.lowercase().orEmpty()
}

fun versionFromBuild(propertyName: String, envAndFileKey: String): String? {
    projectProp(propertyName)?.let { return it }
    System.getenv(envAndFileKey)?.trim()?.takeIf { it.isNotEmpty() }?.let { return it }
    if (System.getenv("GITHUB_ACTIONS") == "true") return null
    return localPropMap[envAndFileKey]?.trim()?.takeIf { it.isNotEmpty() }
}

val releaseStoreFile = rawSecret("RELEASE_STORE_FILE")
val releaseStorePassword = rawSecret("RELEASE_STORE_PASSWORD")
val releaseKeyAlias = rawSecret("RELEASE_KEY_ALIAS")
val releaseKeyPassword = rawSecret("RELEASE_KEY_PASSWORD")
val hasReleaseSigning = listOf(
    releaseStoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword,
).all { it.isNotEmpty() }

android {
    namespace = "com.aethelsoft.grooveplayer"
    compileSdk = 36

    defaultConfig {
        // Must match :app for the same variant. The Data Layer pairs on applicationId + signature.
        applicationId = "com.aethelsoft.grooveplayer"
        minSdk = 30
        targetSdk = 36
        versionCode = versionFromBuild("versionCode", "VERSION_CODE")?.toIntOrNull() ?: 1
        versionName = versionFromBuild("versionName", "VERSION_NAME") ?: "1.0"
        vectorDrawables { useSupportLibrary = true }
    }

    flavorDimensions += "environment"
    productFlavors {
        create("dev") {
            dimension = "environment"
            isDefault = true
            if (grooveEnv() == "staging") {
                applicationIdSuffix = ".staging"
                resValue("string", "app_name", "GroovePlayer Staging")
            }
        }
        create("staging") {
            dimension = "environment"
            applicationIdSuffix = ".staging"
        }
        create("prod") {
            dimension = "environment"
        }
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            // Default debug keystore, the same one :app uses (~/.android/debug.keystore).
            signingConfig = signingConfigs.getByName("debug")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
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
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            excludes += "META-INF/DEPENDENCIES"
        }
    }

    sourceSets.named("main") {
        kotlin.srcDir(rootProject.file("wear-contract/src/main/kotlin"))
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.material3)
    implementation(libs.wear.compose.ui.tooling)
    implementation(libs.wear.tooling.preview)
    implementation(libs.play.services.wearable)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)
}
