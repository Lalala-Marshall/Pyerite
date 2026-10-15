import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

fun localProp(name: String, default: String = ""): String =
    (localProperties.getProperty(name) ?: default).replace("\"", "\\\"")

fun envOrLocal(name: String): String {
    val fromEnv = System.getenv(name)?.trim().orEmpty()
    if (fromEnv.isNotEmpty()) return fromEnv
    return localProperties.getProperty(name)?.trim().orEmpty()
}

val pyeriteVersionCode = providers.gradleProperty("pyerite.versionCode").orNull?.let { raw ->
    raw.toIntOrNull() ?: error("pyerite.versionCode must be an integer")
} ?: 1

val pyeriteVersionName = providers.gradleProperty("pyerite.versionName").orNull
    ?.trim()
    ?.takeIf { it.isNotEmpty() }
    ?: "1.0"

val releaseKeystore = envOrLocal("PYERITE_KEYSTORE_PATH").takeIf { it.isNotEmpty() }?.let { raw ->
    val candidate = File(raw)
    if (candidate.isAbsolute) candidate else rootProject.file(raw)
}?.takeIf { it.isFile }

val requireReleaseSigning = providers.gradleProperty("pyerite.requireReleaseSigning").orNull == "true"
if (requireReleaseSigning && releaseKeystore == null) {
    error("Release signing keystore was not found. Set PYERITE_KEYSTORE_PATH.")
}

android {
    namespace = "com.marshall.pyerite"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.marshall.pyerite"
        minSdk = 24
        targetSdk = 37
        versionCode = pyeriteVersionCode
        versionName = pyeriteVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "EVE_SSO_CLIENT_ID", "\"${localProp("EVE_SSO_CLIENT_ID")}\"")
        buildConfigField(
            "String",
            "EVE_SSO_REDIRECT_URI",
            "\"${localProp("EVE_SSO_REDIRECT_URI", "eveauthpyerite://login.pyerite.callback/")}\"",
        )
    }

    signingConfigs {
        if (releaseKeystore != null) {
            create("release") {
                storeFile = releaseKeystore
                storePassword = envOrLocal("PYERITE_KEYSTORE_PASSWORD")
                keyAlias = envOrLocal("PYERITE_KEY_ALIAS")
                keyPassword = envOrLocal("PYERITE_KEY_PASSWORD")
            }
        }
    }
    buildTypes {
        release {
            if (releaseKeystore != null) {
                signingConfig = signingConfigs.getByName("release")
            }
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
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.appcompat)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // coil
    implementation(libs.coil.compose)

    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Koin
    implementation(libs.koin.android)
    implementation(libs.koin.compose)

    // Retrofit2 + OkHttp + kotlinx.serialization
    implementation(libs.retrofit2)
    implementation(libs.retrofit2.kotlinx.serialization.converter)
    implementation(libs.okhttp)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.vico.compose.m3)

    // Custom Tabs for EVE SSO (tokens use Android Keystore + SharedPreferences)
    implementation(libs.androidx.browser)
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}