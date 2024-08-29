plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    id("org.jetbrains.kotlin.kapt") // Kotlin annotation processing plugin
}

android {
    namespace = "com.example.fedup_foodwasteapp"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.fedup_foodwasteapp"
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
    // Room Database dependencies
    implementation(libs.androidx.room.ktx) // Room with Kotlin extensions
    implementation(libs.androidx.room.runtime) // Room runtime library
    kapt("androidx.room:room-compiler:2.5.1") // Room compiler for annotation processing

    // Navigation Component
    implementation(libs.androidx.navigation.fragment.ktx)

    // Preferences
    implementation(libs.androidx.preference)
    implementation(libs.androidx.preference.ktx)

    // Lifecycle components
    implementation(libs.androidx.lifecycle.viewmodel.ktx)

    // Core libraries
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)

    // Material Design components
    implementation(libs.material)

    // Activity KTX for ViewModel and lifecycle support
    implementation(libs.androidx.activity)

    //random comment
    // UI Components
    implementation(libs.androidx.constraintlayout) // ConstraintLayout for flexible UI
    implementation(libs.androidx.recyclerview) // RecyclerView for lists
    implementation(libs.androidx.cardview) // CardView for card layouts
    implementation(libs.androidx.coordinatorlayout) // CoordinatorLayout for handling complex UIs

    // Room common library (if needed for specific utilities)
    implementation(libs.androidx.room.common)

    // Testing dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
