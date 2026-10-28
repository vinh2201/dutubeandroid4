plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.vinh2201.youtubesmarttvandroid44"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.vinh2201.youtubesmarttvandroid44"
        minSdk = 19
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // ✅ Last AppCompat that still supports API 19
    implementation("androidx.appcompat:appcompat:1.6.1")
    // (pin the transitive resource module too, to avoid 1.7 sneaking in)
    implementation("androidx.appcompat:appcompat-resources:1.6.1")

    // Core KTX that still supports API 19
    implementation("androidx.core:core-ktx:1.12.0")
}

configurations.all {
    resolutionStrategy {
        force("androidx.appcompat:appcompat:1.6.1")
        force("androidx.appcompat:appcompat-resources:1.6.1")
    }
}