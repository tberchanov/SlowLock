import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// The upload key, loaded from keystore.properties (gitignored) or, for CI, from the matching
// environment variables. Absent on a fresh clone by design: an unsigned release build still
// succeeds so `bundleRelease` stays runnable, and `hasReleaseSigning` below is what decides
// whether a signing config gets attached at all.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun signingValue(key: String, env: String): String? =
    keystoreProperties.getProperty(key) ?: System.getenv(env)

val releaseStoreFile = signingValue("storeFile", "SLOWLOCK_STORE_FILE")
val releaseStorePassword = signingValue("storePassword", "SLOWLOCK_STORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "SLOWLOCK_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "SLOWLOCK_KEY_PASSWORD")

val hasReleaseSigning = listOf(
    releaseStoreFile,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() }

android {
    namespace = "com.slowlock"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.slowlock"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
                // Both signature schemes Play accepts for an upload key. v1 stays on for the
                // benefit of API 26-27 devices, which predate v2-only verification.
                enableV1Signing = true
                enableV2Signing = true
            }
        }
    }

    buildTypes {
        release {
            // R8, on. It shrinks, optimizes and obfuscates; keep rules come from
            // src/main/keepRules, which AGP passes through automatically.
            optimization {
                enable = true
            }
            isShrinkResources = true
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            // AppListViewModelTest needs to construct an Application. Nothing in the JVM suite
            // depends on real framework behaviour — the seams keep the platform out of reach —
            // so returning defaults is enough, and keeps Robolectric out of the build.
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    testImplementation(libs.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}