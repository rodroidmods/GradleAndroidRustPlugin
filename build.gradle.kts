plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    id("com.gradle.plugin-publish") version "2.0.0"
}

val pluginId = "io.github.rodroidmods.android-rust"

group = pluginId
version = "0.9.0"

gradlePlugin {
    website = "https://github.com/rodroidmods/GradleAndroidRustPlugin"
    vcsUrl = "https://github.com/rodroidmods/GradleAndroidRustPlugin.git"

    plugins {
        create("AndroidRust") {
            id = pluginId
            implementationClass = "dev.matrix.agp.rust.AndroidRustPlugin"
            displayName = "Plugin for building Rust with Cargo in Android projects"
            description = "This plugin helps with building Rust JNI libraries with Cargo for use in Android projects."
            tags.set(listOf("android", "rust", "jni"))
        }
    }
}

publishing {
    repositories {
        maven {
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
}

repositories {
    mavenCentral()
    google()
    maven("https://plugins.gradle.org/m2/")
}

dependencies {
    gradleApi()

    implementation(libs.agp)
    implementation(libs.agp.api)
}