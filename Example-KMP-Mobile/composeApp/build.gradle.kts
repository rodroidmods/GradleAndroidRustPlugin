import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("com.android.kotlin.multiplatform.library")
    id("io.github.rodroidmods.android-rust")
}

kotlin {
    androidLibrary {
        namespace = "com.examplemobilekmp.rustrodroid.shared"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()
    }

    val rustIosOutputDir = layout.buildDirectory.dir("intermediates/rust/ios/output").get().asFile
    val rustHeaderDir = rootProject.file("rust/ios/include")

    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }

        iosTarget.compilations.getByName("main") {
            cinterops {
                val rustios by creating {
                    defFile(project.file("src/nativeInterop/cinterop/rustios.def"))
                    includeDirs(rustHeaderDir)
                    extraOpts("-libraryPath", rustIosOutputDir.resolve(iosTarget.name.mapToRustTarget()).absolutePath)
                }
            }
        }
    }

    sourceSets {
        androidMain.dependencies {
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.activity.compose)
        }
        commonMain.dependencies {
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

fun String.mapToRustTarget(): String = when (this) {
    "iosArm64" -> "aarch64-apple-ios"
    "iosSimulatorArm64" -> "aarch64-apple-ios-sim"
    "iosX64" -> "x86_64-apple-ios"
    else -> this
}

Rust {
    module("rustios") {
        path = file("../rust/ios")
        targets = listOf("ios-arm64", "ios-sim-arm64")

        buildType("debug") {
            profile = "dev"
        }

        buildType("release") {
            profile = "release"
        }
    }
}
