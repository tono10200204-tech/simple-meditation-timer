import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

// Keystore path and passwords live in keystore.properties, which is gitignored
// and never has its values typed into a build script or a commit. The release
// build simply does not sign itself if that file is missing — a debug build
// stays possible without it, and CI never needs to know the passwords exist.
// The file sits in the repository root, one level above the Gradle root
// (android/), alongside the .gitignore rule that keeps it out of version
// control — not inside the Gradle project itself.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("../keystore.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

android {
    namespace = "com.shl.meditation"

    // Compile against the newest APIs the AndroidX libraries require, but stay
    // on targetSdk 36 — that is what Google Play asks for, and it avoids opting
    // into API 37 runtime behaviour changes that have not been tested here.
    compileSdk = 37
    compileSdkMinor = 1

    defaultConfig {
        applicationId = "com.shl.meditation"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (keystoreProperties.isNotEmpty()) {
            create("release") {
                // A leading ~ is a shell convention, not something java.io.File
                // understands on its own, so it is expanded by hand here.
                val rawPath = keystoreProperties.getProperty("storeFile")
                storeFile = if (rawPath.startsWith("~")) {
                    File(System.getProperty("user.home"), rawPath.removePrefix("~/"))
                } else {
                    rootProject.file("../$rawPath")
                }
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (keystoreProperties.isNotEmpty()) {
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.material3)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    debugImplementation(libs.androidx.ui.tooling)

    testImplementation(libs.junit)

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
