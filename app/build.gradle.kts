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

    // Add the Google services Gradle plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "edu.au.aufondue"
    compileSdk = 35

    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
/*
    tasks.register("generateAuthConfig") {
        doLast {
            val templateFile = file("src/main/res/raw/auth_config_template.json")
            val outputFile = file("src/main/res/raw/auth_config.json")

            var templateContent = templateFile.readText()
            templateContent = templateContent.replace("@microsoft_client_id@", getLocalProperty("MICROSOFT_CLIENT_ID"))
            templateContent = templateContent.replace("@microsoft_tenant_id@", getLocalProperty("MICROSOFT_TENANT_ID"))
            templateContent = templateContent.replace("@microsoft_redirect_uri@", getLocalProperty("MICROSOFT_REDIRECT_URI"))

            outputFile.writeText(templateContent)
        }
    }

    tasks.named("preBuild") {
        dependsOn("generateAuthConfig")
    }
*/


    defaultConfig {
        applicationId = "edu.au.aufondue"
        minSdk = 24
        targetSdk = 35
//        versionCode = 1
//        versionName = "1.0"
//        versionCode = 2
//        versionName = "1.0.1"
//        versionCode = 3
//        versionName = "1.0.2"
//        versionCode = 4
//        versionName = "1.0.3"
//        versionCode = 5
//        versionName = "1.0.4"
        versionCode = 6
        versionName = "1.0.5"
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
    implementation(libs.play.services.location)
    implementation(libs.volley)
    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    testImplementation(composeBom)
    androidTestImplementation(composeBom)
    // Import the BoM for the Firebase platform
    implementation(platform("com.google.firebase:firebase-bom:32.5.0"))

    //FCM
    implementation("com.google.firebase:firebase-messaging:24.1.2")

    // Add the dependency for the Firebase Authentication library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-auth")

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
    //implementation("com.google.android.gms:play-services-maps:19.0.0")
    //implementation("com.google.maps.android:maps-compose:6.2.1")

//    https://mvnrepository.com/artifact/com.microsoft.identity.client/msal
    implementation("com.microsoft.identity.client:msal:5.8.2"){
        exclude(group = "com.microsoft.device.display", module = "display-mask")
    }


    // Retrofit for API calls & okhttp
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    //Moshi
    implementation("com.squareup.moshi:moshi:1.15.1")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")


    // Coroutines for async operations
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    //ViewModel
    implementation("androidx.lifecycle:lifecycle-runtime:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.7")

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