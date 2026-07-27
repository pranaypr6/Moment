import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
    id("kotlin-kapt")
}

// Release signing: loaded from keystore.properties (gitignored, never committed).
// See android/keystore.properties.example for the expected format.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
val hasReleaseSigning = keystorePropertiesFile.exists()
if (hasReleaseSigning) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "com.pranayburra.moment"
    // NOTE: Google Play requires new app submissions to target API 36 (Android 16)
    // starting Aug 31, 2026 (extension available to Nov 1, 2026). Bumped from 34.
    // After bumping, run a full regression pass focused on notifications, background
    // work (WorkManager/FCM), and permission behavior changes introduced by API 35/36.
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pranayburra.moment"
        minSdk = 26
        targetSdk = 36
        // Versioning: bump versionCode by at least 1 on every Play Console upload
        // (it must strictly increase). versionName follows semver (major.minor.patch).
        versionCode = 1
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = file(keystoreProperties.getProperty("storeFile"))
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
                "proguard-rules.pro"
            )
            buildConfigField("String", "BASE_URL", "\"https://moment-production-b0e4.up.railway.app/\"")
            // Host suffix that pushed wallpaper image URLs must match before WallpaperWorker
            // will download/apply them (defense against a spoofed FCM data payload pointing
            // at an attacker-controlled URL - FCM messages are otherwise unauthenticated data
            // as far as the client is concerned). TODO: set this to your actual R2/CDN public
            // host (e.g. "r2.dev" or your custom CDN domain) once known - left blank for now
            // since the real value isn't available in this repo, which disables the allow-list
            // check but keeps the HTTPS-only enforcement active.
            // Exact host from Cloudflare:PublicUrl in backend/appsettings.Development.json.
            // That file is dev config, not necessarily what production actually uses - if
            // your deployed backend's Cloudflare__PublicUrl env var points at a different
            // host (e.g. a custom CDN domain instead of this raw r2.dev one), update this to
            // match. See Phase A of PLAYSTORE_LAUNCH_CHECKLIST.md.
            buildConfigField("String", "TRUSTED_IMAGE_HOST_SUFFIX", "\"pub-750b02dac3184d00822e32cc8511df79.r2.dev\"")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
            // If keystore.properties is missing, release builds fall back to no
            // explicit signingConfig (Gradle will fail assembleRelease/bundleRelease
            // rather than silently signing with the debug key — that's intentional).
        }
        debug {
            buildConfigField("String", "BASE_URL", "\"https://moment-production-b0e4.up.railway.app/\"")
            // Exact host from Cloudflare:PublicUrl in backend/appsettings.Development.json.
            // That file is dev config, not necessarily what production actually uses - if
            // your deployed backend's Cloudflare__PublicUrl env var points at a different
            // host (e.g. a custom CDN domain instead of this raw r2.dev one), update this to
            // match. See Phase A of PLAYSTORE_LAUNCH_CHECKLIST.md.
            buildConfigField("String", "TRUSTED_IMAGE_HOST_SUFFIX", "\"pub-750b02dac3184d00822e32cc8511df79.r2.dev\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.8"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation(platform("androidx.compose:compose-bom:2023.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.glance:glance-appwidget:1.0.0")
    implementation("com.google.android.material:material:1.11.0")
    
    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.6")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
    implementation("androidx.hilt:hilt-work:1.1.0")
    kapt("androidx.hilt:hilt-compiler:1.1.0")

    // Retrofit & OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Room
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Coil
    implementation("io.coil-kt:coil-compose:2.5.0")

    // Security
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Credential Manager
    implementation("androidx.credentials:credentials:1.2.2")
    implementation("androidx.credentials:credentials-play-services-auth:1.2.2")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.1.1")

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.1"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-analytics")

    // Play Install Referrer
    implementation("com.android.installreferrer:installreferrer:2.2")

    // CameraX
    val cameraVersion = "1.3.1"
    implementation("androidx.camera:camera-camera2:$cameraVersion")
    implementation("androidx.camera:camera-lifecycle:$cameraVersion")
    implementation("androidx.camera:camera-view:$cameraVersion")
    implementation("androidx.camera:camera-extensions:$cameraVersion")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.10.01"))
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
