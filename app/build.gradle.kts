plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
}

val releaseStoreFilePath = providers.environmentVariable("DECKAPP_RELEASE_STORE_FILE").orNull
val releaseStorePassword = providers.environmentVariable("DECKAPP_RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("DECKAPP_RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("DECKAPP_RELEASE_KEY_PASSWORD").orNull
val hasStableReleaseKey = listOf(
    releaseStoreFilePath,
    releaseStorePassword,
    releaseKeyAlias,
    releaseKeyPassword
).all { !it.isNullOrBlank() }
val taggedVersionName = providers.gradleProperty("deckappVersionName").orNull
val taggedVersionCode = providers.gradleProperty("deckappVersionCode").orNull?.toIntOrNull()

android {
    namespace = "com.deckapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.deckapp.ttrpg"
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "0.1.6"
        if (taggedVersionCode != null) versionCode = taggedVersionCode
        if (!taggedVersionName.isNullOrBlank()) versionName = taggedVersionName
    }

    signingConfigs {
        if (hasStableReleaseKey) {
            create("stableRelease") {
                storeFile = file(releaseStoreFilePath!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-DEV"
        }
        release {
            isMinifyEnabled = false
            signingConfig = if (hasStableReleaseKey) {
                signingConfigs.getByName("stableRelease")
            } else {
                signingConfigs.getByName("debug")
            }
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions { jvmTarget = "17" }

    buildFeatures { compose = true }

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
    
    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":feature:library"))
    implementation(project(":feature:deck"))
    implementation(project(":feature:draw"))
    implementation(project(":feature:import"))
    implementation(project(":feature:session"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:tables"))
    implementation(project(":feature:encounters"))
    implementation(project(":feature:npcs"))
    implementation(project(":feature:wiki"))
    implementation(project(":feature:reference"))
    implementation(project(":feature:hexploration"))
    implementation(project(":feature:mythic"))
    implementation(project(":feature:dice"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.glance)
    implementation(libs.androidx.glance.appwidget)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose.ui)
    implementation(libs.bundles.lifecycle)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    ksp(libs.hilt.android.compiler)

    debugImplementation(libs.androidx.compose.ui.tooling)
}
