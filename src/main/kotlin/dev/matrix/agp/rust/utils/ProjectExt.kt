package dev.matrix.agp.rust.utils

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ApplicationAndroidComponentsExtension
import com.android.build.api.variant.LibraryAndroidComponentsExtension
import org.gradle.api.Project

internal fun Project.findApplicationExtension(): ApplicationExtension? =
    extensions.findByType(ApplicationExtension::class.java)

internal fun Project.findLibraryExtension(): LibraryExtension? =
    extensions.findByType(LibraryExtension::class.java)

internal fun Project.hasAndroidPlugin(): Boolean =
    findApplicationExtension() != null || findLibraryExtension() != null

internal fun Project.getNdkVersion(): String =
    findApplicationExtension()?.ndkVersion
        ?: findLibraryExtension()?.ndkVersion
        ?: ""

internal fun Project.getMinSdk(): Int =
    findApplicationExtension()?.defaultConfig?.minSdk
        ?: findLibraryExtension()?.defaultConfig?.minSdk
        ?: 21

internal fun Project.findAndroidComponentsExtension(): AndroidComponentsExtension<*, *, *>? =
    extensions.findByType(ApplicationAndroidComponentsExtension::class.java)
        ?: extensions.findByType(LibraryAndroidComponentsExtension::class.java)

internal fun Project.getAndroidComponentsExtension() = checkNotNull(findAndroidComponentsExtension()) {
    "Couldn't find android components extension"
}
