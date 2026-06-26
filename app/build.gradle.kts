plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "io.github.haykh.zham"
    compileSdk = 35
    buildToolsVersion = "35.0.0" // match the version provided by the Nix SDK (store is read-only)

    defaultConfig {
        applicationId = "io.github.haykh.zham"
        minSdk = 26 // API 26+ gives us java.time with no desugaring
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    // App UI (Compose)
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("sh.calvin.reorderable:reorderable:2.4.3")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Home-screen widget (Glance = Compose for App Widgets)
    implementation("androidx.glance:glance-appwidget:1.1.1")
    implementation("androidx.glance:glance-material3:1.1.1")

    // Lifecycle and state management
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7") // collectAsStateWithLifecycle
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")

    // HSV color wheel for the custom accent picker
    implementation("com.github.skydoves:colorpicker-compose:1.1.2")

    // Navigation (bottom-nav tabs)
    implementation("androidx.navigation:navigation-compose:2.8.5")
}
