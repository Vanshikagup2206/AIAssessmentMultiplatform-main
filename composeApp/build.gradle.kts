import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    id("org.jetbrains.kotlin.plugin.compose")
//    id("org.jetbrains.compose") version "1.5.10" // or latest
    kotlin("plugin.serialization") version "1.9.0"
}
kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    jvm("desktop")

    sourceSets {
        val desktopMain by getting {
            resources.srcDir("src/desktopMain/resources")
        }

        androidMain.dependencies {
            implementation(compose.preview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(kotlin("stdlib"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
        }
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
            implementation("androidx.lifecycle:lifecycle-runtime-compose:2.6.2")
            implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
            implementation("net.sourceforge.tess4j:tess4j:5.8.0")  // Tesseract OCR for Java
            implementation("org.slf4j:slf4j-simple:2.0.12")      // Required by Tess4J
            implementation("org.apache.poi:poi-ooxml:5.2.3") // For DOCX files
            implementation("org.apache.pdfbox:pdfbox:2.0.27")// For PDF files
            implementation(compose.material) // For fallback if needed
            implementation("org.apache.commons:commons-io:1.3.2")
            implementation("com.squareup.okhttp3:okhttp:4.9.3") // For making API calls
            implementation("org.json:json:20231013")
            implementation("org.apache.logging.log4j:log4j-core:2.20.0")
            implementation("org.apache.logging.log4j:log4j-api:2.20.0")
            implementation("com.google.code.gson:gson:2.10")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4") // Coroutines
            implementation("io.ktor:ktor-client-core:2.0.0") // HTTP client for API calls
            implementation("io.ktor:ktor-client-cio:2.0.0")
            implementation("io.ktor:ktor-client-content-negotiation:2.3.1")
            implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.1")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.0")
            implementation("com.squareup.okio:okio:3.2.0")
            implementation("org.apache.tika:tika-core:2.9.1")
            implementation("org.apache.tika:tika-parsers-standard-package:2.9.1")
        }
    }
}

android {
    namespace = "com.vanshika.multiplatformproject"
    compileSdk = 34  // ✅ Hardcoded SDK version

    defaultConfig {
        applicationId = "com.vanshika.multiplatformproject"
        minSdk = 21  // ✅ Minimum SDK version (Android 5.0)
        targetSdk = 34  // ✅ Target SDK version (Android 14)
        versionCode = 1
        versionName = "1.0"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.vanshika.multiplatformproject.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Exe)
            packageName = "com.vanshika.multiplatformproject"
            packageVersion = "1.0.0"
            windows {
                javaHome = "C:\\Program Files\\Java\\jdk-17"

//                iconFile.set(file(icon.ico))
            }
        }
    }
}
tasks.withType<Copy> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}