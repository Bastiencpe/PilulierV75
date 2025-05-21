plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("kotlin-kapt")
}

android {
    namespace = "com.example.pilulier"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.pilulier"
        minSdk = 26
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Bibliothèques Android de base
    implementation("com.google.android.material:material:1.12.0")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // Graphiques (charts)
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Reconnaissance de texte (MLKit)
    implementation("com.google.mlkit:text-recognition:16.0.0")

    // Base de données locale (Room)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kaptTest("androidx.room:room-compiler:2.6.1")
    kaptAndroidTest("androidx.room:room-compiler:2.6.1")

    // Filament pour rendu 3D
    implementation("com.google.android.filament:filament-android:1.40.0")
    implementation("com.google.android.filament:filament-utils-android:1.40.0")

    // OpenCV (module local, à définir dans settings.gradle si ce n'est pas déjà fait)
    implementation(project(":opencv"))
}
