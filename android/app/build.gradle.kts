plugins {
    alias(libs.plugins.android.application)
}

// Assinatura da versão final: a chave e as senhas ficam fora do repositório, em
// ~/.gradle/gradle.properties (ver README). Sem elas, o APK de release sai sem assinatura.
val releaseStoreFile = providers.gradleProperty("CALJU_RELEASE_STORE_FILE").orNull

android {
    namespace = "io.github.lucaslimacelino_dev.calju"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.lucaslimacelino_dev.calju"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

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

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = providers.gradleProperty("CALJU_RELEASE_STORE_PASSWORD").get()
                keyAlias = providers.gradleProperty("CALJU_RELEASE_KEY_ALIAS").get()
                keyPassword = providers.gradleProperty("CALJU_RELEASE_KEY_PASSWORD").get()
            }
        }
    }

    buildTypes {
        debug {
            // O emulador enxerga o computador em 10.0.2.2 (backend local, sem HTTPS só em debug).
            buildConfigField("String", "API_BASE_URL", "\"http://10.0.2.2:8080/\"")
            buildConfigField("boolean", "ACCOUNT_ENABLED", "true")
        }
        release {
            // Enquanto não há servidor de produção, a versão final é 100% offline: sem conta e
            // sem permissão de internet.
            buildConfigField("String", "API_BASE_URL", "\"https://api.calju.invalid/\"")
            buildConfigField("boolean", "ACCOUNT_ENABLED", "false")
            if (releaseStoreFile != null) {
                signingConfig = signingConfigs.getByName("release")
            }
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
