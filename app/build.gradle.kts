plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.ras.mydiary"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ras.mydiary"
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation(libs.onesignal)
    implementation(libs.lottie)
    implementation(libs.androidx.recyclerview)
    implementation(libs.glide)
    implementation(libs.philjay.mpandroidchart)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.annotation)
    implementation(libs.androidx.activity)
    implementation(libs.firebase.database)
    implementation(libs.json)
    implementation(libs.volley)
    annotationProcessor (libs.compiler)
    implementation(libs.okhttp)
    annotationProcessor(libs.compiler)
    implementation(libs.firebase.firestore)
    implementation(libs.androidx.ui.text.android)
    implementation(libs.androidx.swiperefreshlayout)
    implementation(libs.androidx.media3.common.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}