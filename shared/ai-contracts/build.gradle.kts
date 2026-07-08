plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    androidTarget()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":shared:domain"))
        }
    }
}

android {
    namespace = "com.meetnote.shared.ai"
    compileSdk = 35

    defaultConfig {
        minSdk = 29
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
