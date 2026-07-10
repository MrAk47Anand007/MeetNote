plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.meetnote.android.background"
    compileSdk = 35

    defaultConfig {
        minSdk = 29
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
    implementation(project(":shared:export"))
    implementation(project(":android-capture"))
    implementation(project(":shared:ai-contracts"))
    implementation(project(":shared:storage"))
    implementation(project(":shared:domain"))
    implementation(project(":shared:core"))
    implementation(libs.androidx.work.runtime)
    implementation(libs.koin.android)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.junit4)
}
