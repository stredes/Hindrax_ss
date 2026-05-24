import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

val releaseKeystorePropertiesFile = rootProject.file("keystore/hindrax-release.properties")
val releaseKeystoreProperties = Properties().apply {
    if (releaseKeystorePropertiesFile.exists()) {
        releaseKeystorePropertiesFile.inputStream().use { load(it) }
    }
}
val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties().apply {
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

fun signingValue(envKey: String, propertyKey: String): String? {
    return System.getenv(envKey)
        ?: releaseKeystoreProperties.getProperty(propertyKey)?.takeIf { it.isNotBlank() }
}

fun localConfigValue(envKey: String, propertyKey: String, fallback: String): String {
    return System.getenv(envKey)
        ?: localProperties.getProperty(propertyKey)?.takeIf { it.isNotBlank() }
        ?: fallback
}

fun buildConfigString(value: String): String {
    val escaped = value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
    return "\"$escaped\""
}

val releaseKeystorePath = signingValue("HINDRAX_KEYSTORE_PATH", "storeFile")
val hasReleaseKeystore = !releaseKeystorePath.isNullOrBlank()

android {
    namespace = "com.hindrax.ss"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.hindrax.ss"
        minSdk = 24
        targetSdk = 36
        versionCode = 50
        versionName = "1.49"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField(
            "String",
            "API_HINDRAX_DEFAULT_BASE_URL",
            buildConfigString(localConfigValue("API_HINDRAX_BASE_URL", "apiHindrax.baseUrl", "https://api-hindrax.vercel.app"))
        )
        buildConfigField(
            "String",
            "API_HINDRAX_DEFAULT_TOKEN",
            buildConfigString(localConfigValue("API_HINDRAX_TOKEN", "apiHindrax.token", ""))
        )
        buildConfigField(
            "boolean",
            "API_HINDRAX_DEFAULT_ENABLED",
            localConfigValue("API_HINDRAX_ENABLED", "apiHindrax.enabled", "false").toBooleanStrictOrNull()?.toString() ?: "false"
        )
    }

    signingConfigs {
        create("release") {
            if (hasReleaseKeystore) {
                storeFile = rootProject.file(releaseKeystorePath!!)
                storePassword = signingValue("HINDRAX_KEYSTORE_PASSWORD", "storePassword")
                keyAlias = signingValue("HINDRAX_KEY_ALIAS", "keyAlias")
                keyPassword = signingValue("HINDRAX_KEY_PASSWORD", "keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
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
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

tasks.register("testClasses") {
    group = "verification"
    description = "Compatibility alias for Android builds that expect a JVM-style testClasses task."
    dependsOn("testDebugUnitTest")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    
    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    // Room
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    
    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    
    // Serialization
    implementation(libs.kotlinx.serialization.json)

    // OkHttp
    implementation(libs.okhttp)

    // Exif
    implementation(libs.androidx.exifinterface)

    // USB Serial
    implementation(libs.usb.serial)

    testImplementation(libs.junit)
    testImplementation(libs.json)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
