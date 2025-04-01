import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    id("org.jetbrains.kotlin.plugin.compose")

//    alias(libs.plugins.composeCompiler)

}
kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_11)
        }
    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    jvm("desktop")

    sourceSets {
        val desktopMain by getting

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

            implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.21")
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
            // Image handling (to show extracted images)
            implementation("org.apache.commons:commons-io:1.3.2")


            implementation("com.squareup.okhttp3:okhttp:4.9.3") // For making API calls
            implementation("org.apache.poi:poi-ooxml:5.2.3")
            implementation("org.json:json:20231013")
            implementation("org.apache.logging.log4j:log4j-core:2.20.0")
            implementation("org.apache.logging.log4j:log4j-api:2.20.0")
            implementation ("com.google.code.gson:gson:2.10")
//            implementation("com.google.ai.client.generativeai:generativeai:0.6.0") // Gemini AI
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4") // Coroutines
            implementation("io.ktor:ktor-client-core:2.0.0") // HTTP client for API calls
            implementation("io.ktor:ktor-client-cio:2.0.0")
            implementation("com.squareup.okio:okio:3.2.0")
        }
    }
}

android {
    namespace = "com.vanshika.multiplatformproject"
//    compileSdk = libs.versions.android.compileSdk.get().toInt()
    compileSdk = 34  // ✅ Hardcoded SDK version

    defaultConfig {
        applicationId = "com.vanshika.multiplatformproject"
//        minSdk = libs.versions.android.minSdk.get().toInt()
//        targetSdk = libs.versions.android.targetSdk.get().toInt()
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.desktop {
    application {
        mainClass = "com.vanshika.multiplatformproject.MainKt"

        nativeDistributions {
//            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            targetFormats(org.jetbrains.compose.desktop.application.dsl.TargetFormat.Exe)
            packageName = "com.vanshika.multiplatformproject"
            packageVersion = "1.0.0"
            windows {
                iconFile.set(project.file("icon.ico"))
            }
        }
    }
}
