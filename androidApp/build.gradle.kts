plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.compose.compiler)
}

// Push needs a Firebase project: drop its google-services.json here
// (androidApp/) and rebuild.  Without it the app builds and runs, push off.
if (file("google-services.json").exists()) {
    apply(plugin = libs.plugins.google.services.get().pluginId)
}

android {
    namespace = "com.felipechaux.jarvis.android"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.felipechaux.jarvis"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }
    buildFeatures {
        compose = true
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.koin.android)
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.messaging)
}
