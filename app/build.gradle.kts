plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.fedup_foodwasteapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.fedup_foodwasteapp"
        minSdk = 26
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    implementation(libs.logging.interceptor)
    // Coroutines
    implementation (libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.core.i18n)
    implementation(libs.protolite.well.known.types)

    implementation (libs.hilt.android)
    kapt (libs.hilt.compiler)


    testImplementation (libs.kotlin.mockito.kotlin)
    testImplementation (libs.mockito.kotlin.v410)

    //Firebase Auth
    implementation (libs.firebase.auth)
    implementation (platform(libs.firebase.bom))
    implementation (libs.play.services.auth)


    //Realtime database
    implementation (libs.firebase.database)
    implementation (libs.androidx.lifecycle.viewmodel.ktx.v261)

    // Unit testing dependencies
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.kotlinx.coroutines.test)

// AndroidX Test - JVM based tests
    androidTestImplementation(libs.androidx.junit.v113)
    androidTestImplementation(libs.androidx.espresso.core.v340)

    // Room components
    implementation(libs.androidx.room.runtime.v261)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.preference.ktx)
    implementation(libs.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    kapt("androidx.room:room-compiler:2.6.1")

    // Lifecycle components
    implementation(libs.androidx.lifecycle.viewmodel.ktx) // Update to latest
    implementation(libs.androidx.lifecycle.livedata.ktx)

    // UI Components
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.coordinatorlayout)
    implementation(libs.androidx.room.common)
    implementation (libs.androidx.core.splashscreen)


    //Recipe
    implementation (libs.picasso)


    //implementation(libs.picasso)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)
    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.v121)
    androidTestImplementation(libs.androidx.espresso.core.v361)
}
