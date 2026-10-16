plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "dev.aezochka.m3ekeyboard"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.aezochka.m3ekeyboard"
        minSdk = 26
        targetSdk = 36
        versionCode = 13
        versionName = "1.8.0"
    }

    buildTypes {
        val ksPwd = System.getenv("KEYSTORE_PASSWORD")
        if (ksPwd != null) {
            signingConfigs.create("ci") {
                storeFile = file("m3e.jks")
                storePassword = ksPwd
                keyAlias = System.getenv("KEY_ALIAS") ?: "m3e"
                keyPassword = ksPwd
            }
        }
        release {
            isMinifyEnabled = false
            signingConfig = if (ksPwd != null) signingConfigs.getByName("ci") else signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures { compose = true }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    testOptions { unitTests { isIncludeAndroidResources = true } }
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
}
