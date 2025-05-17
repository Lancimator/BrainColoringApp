plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.stopaddiction"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lancimator.stopaddiction"
        minSdk = 23
        targetSdk = 35
        versionCode = 1001
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }


    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.22")
    implementation("com.airbnb.android:lottie:6.3.0")
    implementation(libs.androidx.activity)
    implementation("com.android.billingclient:billing-ktx:7.1.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
}
