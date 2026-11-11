plugins {
    alias(libs.plugins.android.application)
}

// An independent test host, so the runner does not rely on Kotlin classes removed from
// WearBack by R8. Not part of the personal APK; intended only for emulator testing.
android {
    namespace = "ru.kheynov.wearback.smoke"
    compileSdk {
        version = release(36) { minorApiLevel = 1 }
    }
    defaultConfig {
        applicationId = "ru.kheynov.wearback.smoke"
        minSdk = 30
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}
