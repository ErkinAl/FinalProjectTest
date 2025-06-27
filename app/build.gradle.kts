plugins {
    alias(libs.plugins.android.application)


}

android {
    namespace = "com.example.afinal"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.afinal"
        minSdk = 24
        targetSdk = 34
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    // CameraX
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-view:1.3.4")

    // ONNX Runtime
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.17.0")

    // Permissions
    implementation("androidx.activity:activity:1.9.3")

    // HTTP Client for API communication
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Supabase Android SDK for Authentication
    implementation("io.github.jan-tennert.supabase:postgrest-kt:2.6.0")
    implementation("io.github.jan-tennert.supabase:gotrue-kt:2.6.0")
    implementation("io.ktor:ktor-client-android:2.3.12")

    // Glide for GIF loading
    implementation("com.github.bumptech.glide:glide:4.16.0")


}