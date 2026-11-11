plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.nuvex.vpn"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.nuvex.vpn"
        minSdk = 24
        targetSdk = 35
        versionCode = 3
        versionName = "0.5.0"

        // Public address of the Nuvex backend. Set with -PnuvexApiUrl=https://... or the
        // NUVEX_API_URL environment variable. Never put keys or secrets here.
        val apiUrl = (project.findProperty("nuvexApiUrl") as String?)
            ?: System.getenv("NUVEX_API_URL")
            ?: ""
        buildConfigField("String", "NUVEX_API_URL", "\"" + apiUrl.replace("\"", "") + "\"")
    }

    buildFeatures {
        buildConfig = true
    }

    // Build per-ABI APKs instead of shipping every native architecture in one file.
    // This is the main APK-size lever for WireGuard's native tunnel implementation.
    splits {
        abi {
            isEnable = true
            reset()
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = false
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation("com.wireguard.android:tunnel:1.0.20260102")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.0.3")
}
