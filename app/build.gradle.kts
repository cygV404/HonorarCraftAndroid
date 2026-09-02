import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
    namespace = "de.v404.honorarcraftandroid"
    compileSdk = 37

    defaultConfig {
        applicationId = "de.v404.honorarcraft"
        minSdk = 24
        targetSdk = 36
        versionCode = 5  // must be changed at new release
        versionName = "1.5"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val path = localProperties.getProperty("release.keystore.path")
            storeFile = if (path != null) file(path) else null
            storePassword = localProperties.getProperty("release.keystore.password")
            keyAlias = localProperties.getProperty("release.key.alias")
            keyPassword = localProperties.getProperty("release.key.password")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk {
                debugSymbolLevel = "FULL"
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // MigrationTestHelper liest die exportierten Schemas aus den androidTest-Assets
    sourceSets.getByName("androidTest") {
        assets.directories.add("$projectDir/schemas")
    }
}

// Das Schemaverzeichnis ist zugleich KSP-Ausgabe und androidTest-Asset. Gradle kennt
// diese Abhaengigkeit nicht, deshalb packt der Asset-Merge auf einem frischen Checkout
// den Stand *vor* dem Export - der Migrationstest scheitert dann mit
// "Cannot find the schema file in the assets folder".
tasks.matching { it.name == "mergeDebugAndroidTestAssets" }.configureEach {
    dependsOn("kspDebugKotlin")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    constraints {
        // kotlinx-serialization-core kommt transitiv ueber lifecycle-viewmodel-savedstate
        // in Version 1.7.3 herein. room-testing 2.8.4 bringt Serializer mit, die gegen
        // 1.8.x kompiliert sind -> AbstractMethodError im Migrationstest. AGPs
        // "consistent resolution" pinnt androidTest auf die Versionen des Haupt-Classpath,
        // die Angleichung muss also hier stehen. Hebt nur an, fuegt nichts Neues hinzu.
        implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.8.1")
    }

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    // Use version catalog for Compose BOM
    implementation(platform(libs.androidx.compose.bom))

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation(libs.androidx.compose.ui.text.google.fonts)

    implementation(libs.androidx.compose.material.icons.extended)

    // Image loading
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Room dependencies
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
