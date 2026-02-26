package dev.matrix.agp.rust.utils

import org.gradle.api.Project

enum class Platform {
    Android,
    Desktop,
    Ios,
}

internal enum class Abi(
    val rustName: String,
    val androidName: String,
    val compilerTriple: String,
    val binUtilsTriple: String,
    val rustTargetTriple: String,
    val platform: Platform = Platform.Android,
    val libraryExtension: String = ".so",
    val jvmResourcePath: String = "",
) {
    X86(
        rustName = "x86",
        androidName = "x86",
        compilerTriple = "i686-linux-android",
        binUtilsTriple = "i686-linux-android",
        rustTargetTriple = "i686-linux-android",
    ),
    X86_64(
        rustName = "x86_64",
        androidName = "x86_64",
        compilerTriple = "x86_64-linux-android",
        binUtilsTriple = "x86_64-linux-android",
        rustTargetTriple = "x86_64-linux-android",
    ),
    Arm(
        rustName = "arm",
        androidName = "armeabi-v7a",
        compilerTriple = "armv7a-linux-androideabi",
        binUtilsTriple = "arm-linux-androideabi",
        rustTargetTriple = "armv7-linux-androideabi",
    ),
    Arm64(
        rustName = "arm64",
        androidName = "arm64-v8a",
        compilerTriple = "aarch64-linux-android",
        binUtilsTriple = "aarch64-linux-android",
        rustTargetTriple = "aarch64-linux-android",
    ),

    DesktopLinuxX64(
        rustName = "desktop-linux-x64",
        androidName = "",
        compilerTriple = "x86_64-unknown-linux-gnu",
        binUtilsTriple = "x86_64-unknown-linux-gnu",
        rustTargetTriple = "x86_64-unknown-linux-gnu",
        platform = Platform.Desktop,
        libraryExtension = ".so",
        jvmResourcePath = "linux-x86-64",
    ),
    DesktopWindowsX64(
        rustName = "desktop-windows-x64",
        androidName = "",
        compilerTriple = "x86_64-pc-windows-msvc",
        binUtilsTriple = "x86_64-pc-windows-msvc",
        rustTargetTriple = "x86_64-pc-windows-msvc",
        platform = Platform.Desktop,
        libraryExtension = ".dll",
        jvmResourcePath = "win32-x86-64",
    ),
    DesktopMacosX64(
        rustName = "desktop-macos-x64",
        androidName = "",
        compilerTriple = "x86_64-apple-darwin",
        binUtilsTriple = "x86_64-apple-darwin",
        rustTargetTriple = "x86_64-apple-darwin",
        platform = Platform.Desktop,
        libraryExtension = ".dylib",
        jvmResourcePath = "darwin-x86-64",
    ),
    DesktopMacosArm64(
        rustName = "desktop-macos-arm64",
        androidName = "",
        compilerTriple = "aarch64-apple-darwin",
        binUtilsTriple = "aarch64-apple-darwin",
        rustTargetTriple = "aarch64-apple-darwin",
        platform = Platform.Desktop,
        libraryExtension = ".dylib",
        jvmResourcePath = "darwin-aarch64",
    ),

    IosArm64(
        rustName = "ios-arm64",
        androidName = "",
        compilerTriple = "aarch64-apple-ios",
        binUtilsTriple = "aarch64-apple-ios",
        rustTargetTriple = "aarch64-apple-ios",
        platform = Platform.Ios,
        libraryExtension = ".a",
    ),
    IosSimArm64(
        rustName = "ios-sim-arm64",
        androidName = "",
        compilerTriple = "aarch64-apple-ios-sim",
        binUtilsTriple = "aarch64-apple-ios-sim",
        rustTargetTriple = "aarch64-apple-ios-sim",
        platform = Platform.Ios,
        libraryExtension = ".a",
    ),
    IosSimX64(
        rustName = "ios-sim-x64",
        androidName = "",
        compilerTriple = "x86_64-apple-ios",
        binUtilsTriple = "x86_64-apple-ios",
        rustTargetTriple = "x86_64-apple-ios",
        platform = Platform.Ios,
        libraryExtension = ".a",
    ),
    IosArm64MacCatalyst(
        rustName = "ios-macabi-arm64",
        androidName = "",
        compilerTriple = "aarch64-apple-ios-macabi",
        binUtilsTriple = "aarch64-apple-ios-macabi",
        rustTargetTriple = "aarch64-apple-ios-macabi",
        platform = Platform.Ios,
        libraryExtension = ".a",
    );

    val isAndroid: Boolean get() = platform == Platform.Android
    val isDesktop: Boolean get() = platform == Platform.Desktop
    val isIos: Boolean get() = platform == Platform.Ios

    @Suppress("unused", "MemberVisibilityCanBePrivate")
    companion object {
        fun fromRustName(value: String) = entries.find { it.rustName.equals(value, ignoreCase = true) }

        fun fromAndroidName(value: String) = entries.find {
            it.androidName.equals(value, ignoreCase = true) && it.androidName.isNotEmpty()
        }

        fun fromInjectedBuildAbi(project: Project): Set<Abi> {
            val values = project.properties["android.injected.build.abi"] ?: return emptySet()
            return values.toString().split(",")
                .asSequence()
                .mapNotNull { fromAndroidName(it.trim()) }
                .toSet()
        }

        fun fromRustNames(names: Collection<String>): Set<Abi> {
            return names.asSequence()
                .map {
                    requireNotNull(fromRustName(it)) {
                        "Unsupported target: '$it'. Supported targets: ${entries.joinToString { e -> e.rustName }}"
                    }
                }
                .toSet()
        }

        fun androidEntries() = entries.filter { it.isAndroid }
        fun desktopEntries() = entries.filter { it.isDesktop }
        fun iosEntries() = entries.filter { it.isIos }
    }

    fun cc(apiLevel: Int) = when (Os.current.isWindows) {
        true -> "${compilerTriple}${apiLevel}-clang.cmd"
        else -> "${compilerTriple}${apiLevel}-clang"
    }

    fun ccx(apiLevel: Int) = when (Os.current.isWindows) {
        true -> "${compilerTriple}${apiLevel}-clang++.cmd"
        else -> "${compilerTriple}${apiLevel}-clang++"
    }

    fun ar(ndkVersionMajor: Int) = when (ndkVersionMajor >= 23) {
        true -> "llvm-ar"
        else -> "${binUtilsTriple}-ar"
    }
}
