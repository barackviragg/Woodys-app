plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android) // This will use the Kotlin version specified in the project-level file
    id("com.google.gms.google-services")
}


android {
    namespace = "com.woodys.woodysburger"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.woodys.woodysburger"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
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
        jvmTarget = "11" // Align with Java compilation target
    }

    buildFeatures {
        compose = true
        viewBinding = true
        dataBinding = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.3" // Ensure this version is compatible with your Compose version
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx.v1101)
    implementation(libs.androidx.activity.compose.v172) // Updated to latest stable version
    implementation(libs.material)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.play.services.auth)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom.v20230300)) // Add the BOM
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)

    implementation("com.google.zxing:core:3.4.1")


    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.analytics)
    implementation (libs.firebase.firestore.ktx) // Firestore dependency

    //webrequests
    implementation (libs.zxing.android.embedded)
    implementation (libs.firebase.firestore.ktx.v2460)
    implementation (libs.firebase.auth.ktx)
    implementation (libs.okhttp)

    implementation ("org.jsoup:jsoup:1.15.3")


    implementation("com.google.mlkit:barcode-scanning:16.2.0")
    implementation("androidx.camera:camera-core:1.2.0")
    implementation("androidx.camera:camera-camera2:1.2.0")
    implementation("androidx.camera:camera-lifecycle:1.2.0")
    implementation("androidx.camera:camera-view:1.0.0-alpha32")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core.v351)
    androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00")) // Add BOM for ui-test-junit4
    androidTestImplementation(libs.ui.test.junit4) // Remove version
}

