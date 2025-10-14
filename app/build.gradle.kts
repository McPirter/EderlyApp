plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("kotlin-kapt")  // Añade este plugin para Moshi
}

android {
    namespace = "com.example.elderly"
    compileSdk = 35



    defaultConfig {
        applicationId = "com.example.elderly"
        minSdk = 30
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
        compose = true
    }
}

dependencies {
    // Dependencias básicas
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.appcompat)

    // Retrofit y conversión JSON
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0") // Cambiado a converter-moshi
    implementation("com.squareup.moshi:moshi-kotlin:1.14.0")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.14.0") // Procesador de anotaciones
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // OkHttp (necesario para RequestBody)
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Wear OS
    implementation("com.google.android.gms:play-services-wearable:18.1.0")

    // Material Design (usa solo una versión)
    implementation("com.google.android.material:material:1.9.0")

    // Mapas
    implementation("org.osmdroid:osmdroid-android:6.1.14")
    testImplementation("junit:junit:4.13.2")


    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
    testImplementation(kotlin("test"))
}