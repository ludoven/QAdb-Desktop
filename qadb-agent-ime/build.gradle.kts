plugins {
    alias(libs.plugins.androidApplication)
}

val releaseSigningReady = listOf(
    "QADB_HELPER_KEYSTORE",
    "QADB_HELPER_STORE_PASSWORD",
    "QADB_HELPER_KEY_PASSWORD",
    "QADB_HELPER_KEY_ALIAS"
).all { !System.getenv(it).isNullOrBlank() }

android {
    namespace = "com.ludoven.qadb.agentime"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.ludoven.qadb.agentime"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (releaseSigningReady) {
            create("release") {
                storeFile = file(requireNotNull(System.getenv("QADB_HELPER_KEYSTORE")))
                storePassword = System.getenv("QADB_HELPER_STORE_PASSWORD")
                keyPassword = System.getenv("QADB_HELPER_KEY_PASSWORD")
                keyAlias = System.getenv("QADB_HELPER_KEY_ALIAS")
            }
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (releaseSigningReady) signingConfig = signingConfigs.getByName("release")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
