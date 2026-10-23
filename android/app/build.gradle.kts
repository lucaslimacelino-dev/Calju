plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "io.github.lucaslimacelino_dev.calju"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.lucaslimacelino_dev.calju"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        javaCompileOptions {
            annotationProcessorOptions {
                arguments["room.schemaLocation"] = "$projectDir/schemas"
            }
        }
    }

    sourceSets {
        getByName("main") {
            assets.directories.add("../../data/catalog")
        }
        getByName("androidTest") {
            assets.directories.add("$projectDir/schemas")
        }
    }

    buildTypes {
        debug {
            // O emulador enxerga o computador em 10.0.2.2 (backend local, sem HTTPS só em debug).
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/\"")
        }
        release {
            // Endereço de produção definido na Fase 9; até lá a versão final não tem servidor.
            buildConfigField("String", "API_BASE_URL", "\"https://api.calju.invalid/\"")
            isMinifyEnabled = true
            isShrinkResources = true
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

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }

    testOptions {
        animationsDisabled = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.activity)
    implementation(libs.material)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.recyclerview)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.room.runtime)
    annotationProcessor(libs.room.compiler)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.work.runtime)

    testImplementation(libs.junit)
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.arch.core.testing)

    androidTestImplementation(libs.junit.ext)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.espresso.accessibility)
    androidTestImplementation(libs.okhttp.mockwebserver)
}
