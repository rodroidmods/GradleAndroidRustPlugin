package dev.matrix.agp.rust.utils

import org.gradle.api.Project
import java.io.File
import java.util.Properties

internal class AndroidHelper private constructor(
    private val project: Project,
) {
    companion object {
        fun create(project: Project): AndroidHelper? {
            return try {
                Class.forName("com.android.build.api.variant.AndroidComponentsExtension")
                AndroidHelper(project)
            } catch (_: ClassNotFoundException) {
                null
            }
        }
    }

    fun hasAndroidPlugin(): Boolean {
        return findApplicationExtension() != null
            || findLibraryExtension() != null
            || hasKmpLibraryPlugin()
    }

    private fun hasKmpLibraryPlugin(): Boolean {
        return project.plugins.hasPlugin("com.android.kotlin.multiplatform.library")
    }

    fun getNdkVersion(): String {
        return findApplicationExtension()?.ndkVersion
            ?: findLibraryExtension()?.ndkVersion
            ?: ""
    }

    fun getMinSdk(): Int {
        return findApplicationExtension()?.defaultConfig?.minSdk
            ?: findLibraryExtension()?.defaultConfig?.minSdk
            ?: getMinSdkFromKmpLibrary()
            ?: 21
    }

    private fun getMinSdkFromKmpLibrary(): Int? {
        return try {
            val kotlinExt = project.extensions.findByName("kotlin") ?: return null
            val method = kotlinExt.javaClass.methods.find { it.name == "getAndroidLibrary" } ?: return null
            val androidLibrary = method.invoke(kotlinExt) ?: return null
            val minSdkMethod = androidLibrary.javaClass.methods.find { it.name == "getMinSdk" } ?: return null
            minSdkMethod.invoke(androidLibrary) as? Int
        } catch (_: Exception) {
            null
        }
    }

    fun getNdkDirectory(): File? {
        val componentsExt = findComponentsExtension()
        if (componentsExt != null) {
            try {
                val ndkDir = componentsExt.sdkComponents.ndkDirectory.get().asFile
                if (ndkDir.exists()) return ndkDir
            } catch (_: Exception) {
            }
        }

        val sdkDir = getSdkDirectory() ?: return null
        val ndkRoot = File(sdkDir, "ndk")
        if (!ndkRoot.exists()) return null

        val ndkVersions = ndkRoot.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedDescending()
        return ndkVersions?.firstOrNull()
    }

    private fun getSdkDirectory(): File? {
        try {
            val localProps = File(project.rootDir, "local.properties")
            if (localProps.exists()) {
                val props = Properties()
                localProps.inputStream().use { props.load(it) }
                val sdkDir = props.getProperty("sdk.dir")
                if (sdkDir != null) {
                    val dir = File(sdkDir)
                    if (dir.exists()) return dir
                }
            }
        } catch (_: Exception) {
        }

        val androidHome = System.getenv("ANDROID_HOME") ?: System.getenv("ANDROID_SDK_ROOT")
        if (androidHome != null) {
            val dir = File(androidHome)
            if (dir.exists()) return dir
        }

        return null
    }

    fun getBuildTypeNames(): List<String> {
        val appExt = findApplicationExtension()
        if (appExt != null) {
            return appExt.buildTypes.map { it.name }
        }
        val libExt = findLibraryExtension()
        if (libExt != null) {
            return libExt.buildTypes.map { it.name }
        }
        return listOf("release")
    }

    fun addJniLibsSourceSet(buildTypeName: String, jniLibsDir: File) {
        val appExt = findApplicationExtension()
        if (appExt != null) {
            appExt.sourceSets.findByName(buildTypeName)?.jniLibs?.directories?.add(jniLibsDir.path)
            return
        }
        val libExt = findLibraryExtension()
        if (libExt != null) {
            libExt.sourceSets.findByName(buildTypeName)?.jniLibs?.directories?.add(jniLibsDir.path)
        }
    }

    fun findComponentsExtension(): com.android.build.api.variant.AndroidComponentsExtension<*, *, *>? {
        return project.extensions.findByType(com.android.build.api.variant.ApplicationAndroidComponentsExtension::class.java)
            ?: project.extensions.findByType(com.android.build.api.variant.LibraryAndroidComponentsExtension::class.java)
            ?: try {
                project.extensions.findByType(com.android.build.api.variant.AndroidComponentsExtension::class.java)
            } catch (_: Exception) {
                null
            }
    }

    fun getComponentsExtension(): com.android.build.api.variant.AndroidComponentsExtension<*, *, *> {
        return checkNotNull(findComponentsExtension()) { "Couldn't find android components extension" }
    }

    private fun findApplicationExtension(): com.android.build.api.dsl.ApplicationExtension? =
        project.extensions.findByType(com.android.build.api.dsl.ApplicationExtension::class.java)

    private fun findLibraryExtension(): com.android.build.api.dsl.LibraryExtension? =
        project.extensions.findByType(com.android.build.api.dsl.LibraryExtension::class.java)
}
