plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

// Stable signing identity. Previous releases were signed with whatever debug
// keys the CI runner generated, so no two builds could update each other.
// The key lives outside the repository; pass it in through the environment.
val signingStore: String? = System.getenv("ELM327_KEYSTORE")
val signingStorePassword: String? = System.getenv("ELM327_KEYSTORE_PASSWORD")
val signingAlias: String? = System.getenv("ELM327_KEY_ALIAS")
val signingKeyPassword: String? = System.getenv("ELM327_KEY_PASSWORD")
val hasSigningKey = listOf(signingStore, signingStorePassword, signingAlias, signingKeyPassword)
    .all { !it.isNullOrBlank() && File(signingStore!!).exists() }

android {
    namespace = "com.elm327.emulator"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.elm327.emulator"
        minSdk = 26
        targetSdk = 34
        versionCode = 3
        versionName = "1.2.1"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    signingConfigs {
        if (hasSigningKey) {
            create("release") {
                storeFile = File(signingStore!!)
                storePassword = signingStorePassword
                keyAlias = signingAlias
                keyPassword = signingKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            if (hasSigningKey) signingConfig = signingConfigs.getByName("release")
        }
        release {
            isMinifyEnabled = false
            if (hasSigningKey) signingConfig = signingConfigs.getByName("release")
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
        freeCompilerArgs = listOf("-Xkotlin-compiler-execution-strategy=in-process")
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    testImplementation("junit:junit:4.13.2")
}
