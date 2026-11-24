plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.jffloresriera.maizellm"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.jffloresriera.maizellm"
        // minSdk 24 lo exige la librería de inferencia (llama-android); el requisito real
        // del proyecto es RAM (6 GB+), no versión de Android.
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }
    // La versión del compilador de Compose ya la resuelve el plugin
    // org.jetbrains.kotlin.plugin.compose a partir de la versión de Kotlin;
    // composeOptions.kotlinCompilerExtensionVersion ya no se usa.

    // El .gguf se empaqueta como asset. Si Android lo comprimiera dentro del APK,
    // habría que descomprimirlo en memoria antes de poder leerlo: con noCompress
    // queda almacenado tal cual y se puede copiar directo a almacenamiento interno.
    androidResources {
        noCompress += "gguf"
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")

    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Inferencia on-device: AAR precompilado de llama.cpp, sin NDK/CMake propios.
    // MIT, Maven Central. https://github.com/ffmpegkit-maintained/llama-android
    implementation("dev.ffmpegkit-maintained:llama-android:0.1.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
