plugins {
    //alias(libs.plugins.android.application)
    //alias(libs.plugins.kotlin.android)
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.android.application")
}

android {
    namespace = "com.mycompany.lovegarden"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.mycompany.lovegarden"
        minSdk = 24
        targetSdk = 35
        versionCode = 2
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
            signingConfig = signingConfigs.getByName("debug")
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

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.activity:activity:1.10.1")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.fragment:fragment-ktx:1.5.6")
    implementation("androidx.navigation:navigation-fragment-ktx:2.8.9")
    implementation("androidx.navigation:navigation-ui-ktx:2.8.9")
    implementation(libs.firebase.auth)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation("com.google.firebase:firebase-database-ktx")
    implementation(platform("com.google.firebase:firebase-bom:33.13.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-auth-ktx")

    // Firestore 의존성
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")

    //lottiefiles json 파일
    implementation("com.airbnb.android:lottie:6.3.0")

}