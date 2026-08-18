plugins {
        alias(libs.plugins.android.application)
        alias(libs.plugins.kotlin.compose)
        alias(libs.plugins.google.services)
        alias(libs.plugins.firebase.crashlytics)

}

import java.util.Properties

val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.isFile) {
    keystorePropertiesFile.inputStream().use(keystoreProperties::load)
}

fun requiredSigningProperty(name: String): String =
    keystoreProperties.getProperty(name)
        ?.trim()
        ?.takeIf { it.isNotEmpty() }
        ?: error("Missing required release signing property: $name in ${keystorePropertiesFile.path}")

val umpDebugGeography = providers.gradleProperty("umpDebugGeography")
    .orNull
    ?.trim()
    ?.uppercase()
    ?.takeIf { it in setOf("DISABLED", "EEA", "REGULATED_US_STATE", "OTHER") }
    ?: "DISABLED"
val umpTestDeviceHash = providers.gradleProperty("umpTestDeviceHash")
    .orNull
    ?.trim()
    ?.uppercase()
    ?.takeIf { it.matches(Regex("[A-F0-9]+")) }
    .orEmpty()
val umpResetTestState = providers.gradleProperty("umpResetTestState")
    .orNull
    ?.toBooleanStrictOrNull()
    ?: false
val admobTestDeviceHash = providers.gradleProperty("admobTestDeviceHash")
    .orNull
    ?.trim()
    ?.uppercase()
    .orEmpty()
require(admobTestDeviceHash.isEmpty() || admobTestDeviceHash.matches(Regex("[A-F0-9]{32}"))) {
    "admobTestDeviceHash must be a 32-character hexadecimal Google Mobile Ads test-device hash"
}

android {
    namespace = "com.ap.messages"

    compileSdk = 37

    defaultConfig {
        applicationId = "com.ap.messages"
        minSdk = 26
        targetSdk = 37
        versionCode = 4
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        debug {
            buildConfigField("String", "UMP_DEBUG_GEOGRAPHY", "\"$umpDebugGeography\"")
            buildConfigField("String", "UMP_TEST_DEVICE_HASH", "\"$umpTestDeviceHash\"")
            buildConfigField("boolean", "UMP_RESET_TEST_STATE", umpResetTestState.toString())
            buildConfigField("String", "ADMOB_TEST_DEVICE_HASH", "\"$admobTestDeviceHash\"")
        }
        release {
            signingConfig = signingConfigs.create("release") {
                storeFile = rootProject.file(requiredSigningProperty("storeFile"))
                storePassword = requiredSigningProperty("storePassword")
                keyAlias = requiredSigningProperty("keyAlias")
                keyPassword = requiredSigningProperty("keyPassword")
            }
            buildConfigField("String", "UMP_DEBUG_GEOGRAPHY", "\"DISABLED\"")
            buildConfigField("String", "UMP_TEST_DEVICE_HASH", "\"\"")
            buildConfigField("boolean", "UMP_RESET_TEST_STATE", "false")
            buildConfigField("String", "ADMOB_TEST_DEVICE_HASH", "\"$admobTestDeviceHash\"")
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.firebase.config)
    implementation(libs.google.mobile.ads)
    implementation(libs.google.ump)
    implementation(libs.google.play.review)
    implementation("com.android.billingclient:billing:9.1.0")

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.compose.material.icons.extended)

    testImplementation(libs.junit)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)

    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
