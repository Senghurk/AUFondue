import org.gradle.api.Project
import java.util.Properties
import java.io.FileInputStream

fun Project.getLocalProperty(key: String, defaultValue: String = ""): String {
    val localProperties = Properties()
    val localPropertiesFile = project.rootProject.file("local.properties")
    if (localPropertiesFile.exists()) {
        localProperties.load(FileInputStream(localPropertiesFile))
    }
    return localProperties.getProperty(key, defaultValue)
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "edu.au.aufondue"
    compileSdk = 35

    defaultConfig {
        applicationId = "edu.au.aufondue"
        minSdk = 24
        targetSdk = 35
//        versionCode = 1
//        versionName = "1.0"
//        versionCode = 2        // Change from 1 to 2
//        versionName = "1.0.1"  // Update to reflect new version
        versionCode = 3
        versionName = "1.0.2"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        manifestPlaceholders["MAPS_API_KEY"] = getLocalProperty("MAPS_API_KEY")


    }

    signingConfigs {
        create("release") {
            storeFile = file("aufondue-release.keystore")  // Relative path
            storePassword = getLocalProperty("KEYSTORE_PASSWORD")
            keyAlias = getLocalProperty("KEY_ALIAS")
            keyPassword = getLocalProperty("KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            isDebuggable = false
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
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core)
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    testImplementation(composeBom)
    androidTestImplementation(composeBom)

    // Material 3
    implementation("androidx.compose.material3:material3:1.3.1")

    // Icons dependency - Add this explicitly
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")

    // Core Android dependencies
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Compose dependencies
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")

    // Coil compose
    implementation("io.coil-kt.coil3:coil:3.0.4")
    implementation("io.coil-kt.coil3:coil-compose:3.0.4")
    implementation("io.coil-kt.coil3:coil-network-okhttp:3.0.4")



    // Navigation for Compose
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // MAP Dependency
    implementation("com.google.android.gms:play-services-maps:19.0.0")
    implementation("com.google.maps.android:maps-compose:6.2.1")

    // https://mvnrepository.com/artifact/com.microsoft.identity.client/msal
//    implementation("com.microsoft.identity.client:msal:5.4.0")


    // ViewModel utilities for Compose
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")

    // Development Tools
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")

    // Testing
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    testImplementation("androidx.compose.ui:ui-test-junit4")
    androidTestImplementation("androidx.compose.ui:ui-test")
}