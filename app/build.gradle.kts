plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("kotlin-parcelize")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.FedUpGroup.fedup_foodwasteapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.FedUpGroup.fedup_foodwasteapp"
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

    implementation (libs.material.v1110) // Use latest stable version

    implementation (libs.androidx.biometric) // Latest version may vary


    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-analytics")

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
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.androidx.ui.desktop)
    kapt (libs.hilt.compiler)


    testImplementation (libs.kotlin.mockito.kotlin)
    testImplementation (libs.mockito.kotlin.v410)


    // Mockito for mocking
    testImplementation (libs.mockito.core.v390)
    testImplementation (libs.mockito.inline)

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


    // AndroidX Test - Core testing libraries
    testImplementation (libs.androidx.core.testing)
    testImplementation (libs.androidx.core)


    //implementation(libs.picasso)
    implementation(libs.retrofit)
    implementation(libs.converter.gson)

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.v121)
    androidTestImplementation(libs.androidx.espresso.core.v361)

    // Coroutines Testing
    testImplementation (libs.kotlinx.coroutines.test.v151)
}