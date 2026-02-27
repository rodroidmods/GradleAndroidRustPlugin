# Gradle Android Rust Plugin

A Gradle plugin for building Rust libraries with Cargo for **Android**, **Desktop JVM**, and **iOS** projects with full **Kotlin Multiplatform (KMP)** support.

[![KMP Build](https://github.com/rodroidslint/GradleAndroidRustPlugin/actions/workflows/kmp-build.yml/badge.svg)](https://github.com/rodroidslint/GradleAndroidRustPlugin/actions/workflows/kmp-build.yml)

## Version 2.2.0

### 🚀 What's New

- **`Rust {}` DSL** — New top-level extension alongside `androidRust {}` for cleaner configuration
- **Automatic iOS lifecycle hooking** — iOS Rust builds run automatically before `linkFramework`, `cinterop`, and `compileKotlinIos` tasks
- **Automatic Android lifecycle hooking** — Rust builds hook into `preBuild` and `mergeNativeLibs` via lazy task matching (works with AGP 9.0+)
- **AGP 9.0.1 + Gradle 9.3.1** — Fully compatible with the latest Android Gradle Plugin
- **KMP library support** — Works with `com.android.kotlin.multiplatform.library` modules
- **Independent NDK discovery** — Falls back to `local.properties` → `sdk.dir` → `ndk/` for KMP library modules

---

## How to Install

The plugin is available on Gradle Plugin Portal: https://plugins.gradle.org/plugin/io.github.rodroidmods.android-rust

```kotlin
plugins {
    id("io.github.rodroidmods.android-rust") version "2.2.0"
}
```

---

## Quick Start

### Android Only (Jetpack Compose)

```kotlin
plugins {
    id("com.android.application")
    id("io.github.rodroidmods.android-rust")
}

Rust {
    module("mylib") {
        path = file("rust/mylib")
        targets = listOf("arm64", "x86_64")
    }
}
```

### KMP Mobile (Android + iOS)

**Architecture**: Split into two modules for AGP 9.0 compatibility:

```
project/
├── androidApp/          # com.android.application — Android Rust (.so)
├── composeApp/          # com.android.kotlin.multiplatform.library — iOS Rust (.a)
└── rust/
    ├── Cargo.toml       # Workspace
    ├── core/            # Shared Rust logic
    ├── android/         # JNI bindings (cdylib)
    └── ios/             # C-ABI bindings (staticlib)
        └── include/
            └── rustios.h  # C header for cinterop
```

**`androidApp/build.gradle.kts`** — Builds Android Rust:

```kotlin
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
    id("io.github.rodroidmods.android-rust")
}

Rust {
    module("rustandroid") {
        path = file("../rust/android")
        targets = listOf("arm64", "x86_64")
    }
}
```

**`composeApp/build.gradle.kts`** — Builds iOS Rust with cinterop:

```kotlin
plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    id("com.android.kotlin.multiplatform.library")
    id("io.github.rodroidmods.android-rust")
}

kotlin {
    val rustIosOutputDir = layout.buildDirectory.dir("intermediates/rust/ios/output").get().asFile
    val rustHeaderDir = rootProject.file("rust/ios/include")

    listOf(iosArm64(), iosSimulatorArm64()).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "ComposeApp"
            isStatic = false
            linkerOpts("-L${rustIosOutputDir.resolve(iosTarget.name.mapToRustTarget())}", "-lrustios")
        }

        iosTarget.compilations.getByName("main") {
            cinterops {
                val rustios by creating {
                    defFile(project.file("src/nativeInterop/cinterop/rustios.def"))
                    includeDirs(rustHeaderDir)
                }
            }
        }
    }
}

Rust {
    module("rustios") {
        path = file("../rust/ios")
        targets = listOf("ios-arm64", "ios-sim-arm64")
    }
}
```

---

## Configuration

### `Rust {}` DSL (Recommended)

```kotlin
Rust {
    minimumSupportedRustVersion = "1.70.0"

    module("mylib") {
        path = file("rust/mylib")
        targets = listOf("arm64", "x86_64", "ios-arm64", "ios-sim-arm64")

        buildType("debug") {
            profile = "dev"
            runTests = true
        }

        buildType("release") {
            profile = "release"
        }
    }
}
```

### `androidRust {}` DSL (Legacy, still supported)

```kotlin
androidRust {
    module("mylib") {
        path = file("rust/mylib")
        targets = listOf("arm64", "x86_64")
    }
}
```

### Configuration Options

| Option | Description | Default |
|--------|-------------|---------|
| `minimumSupportedRustVersion` | Minimum Rust version required | `""` (no check) |
| `path` | Path to Rust project directory | **Required** |
| `targets` | List of target names | `["arm", "arm64", "x86", "x86_64"]` |
| `profile` | Rust build profile | `"release"` |
| `runTests` | Run `cargo test` before building | `null` (disabled) |
| `disableAbiOptimization` | Disable IDE ABI injection | `null` (false) |
| `cargoClean` | Run `cargo clean` with `./gradlew clean` | `null` (disabled) |
| `clippyDenyWarnings` | Fail build on clippy warnings | `null` (false) |

---

## Supported Targets

### Android

| Target Name | Android ABI | Architecture |
|-------------|-------------|--------------|
| `arm` | `armeabi-v7a` | 32-bit ARM |
| `arm64` | `arm64-v8a` | 64-bit ARM |
| `x86` | `x86` | 32-bit x86 |
| `x86_64` | `x86_64` | 64-bit x86 |

### Desktop

| Target Name | Rust Triple | Library Extension |
|-------------|-------------|-------------------|
| `desktop-linux-x64` | `x86_64-unknown-linux-gnu` | `.so` |
| `desktop-windows-x64` | `x86_64-pc-windows-msvc` | `.dll` |
| `desktop-macos-x64` | `x86_64-apple-darwin` | `.dylib` |
| `desktop-macos-arm64` | `aarch64-apple-darwin` | `.dylib` |

### iOS

| Target Name | Rust Triple | Description |
|-------------|-------------|-------------|
| `ios-arm64` | `aarch64-apple-ios` | Physical devices |
| `ios-sim-arm64` | `aarch64-apple-ios-sim` | Simulator (Apple Silicon) |
| `ios-sim-x64` | `x86_64-apple-ios` | Simulator (Intel) |
| `ios-macabi-arm64` | `aarch64-apple-ios-macabi` | Mac Catalyst |

---

## iOS Cinterop Setup

To use Rust from Kotlin/Native on iOS, you need:

### 1. C Header (`rust/ios/include/rustios.h`)

```c
#ifndef RUSTIOS_H
#define RUSTIOS_H

#include <stdint.h>

char* rust_greeting_c(const char* name);
void rust_greeting_free(char* s);
uint64_t fibonacci_c(uint32_t n);

#endif
```

### 2. Cinterop Definition (`composeApp/src/nativeInterop/cinterop/rustios.def`)

```
headers = rustios.h
```

### 3. iOS Kotlin Bridge (`composeApp/src/iosMain/kotlin/.../RustBridge.ios.kt`)

```kotlin
@OptIn(ExperimentalForeignApi::class)
actual object RustBridge {
    actual fun rustGreeting(name: String): String {
        val result = rust_greeting_c(name)
        val greeting = result?.toKString() ?: "Unknown"
        rust_greeting_free(result)
        return greeting
    }

    actual fun fibonacci(n: Int): Long {
        return fibonacci_c(n.toUInt()).toLong()
    }
}
```

### 4. Cargo.toml (iOS crate)

```toml
[lib]
crate-type = ["staticlib"]
```

---

## Build Tasks

### Android
- `build<BuildType><Module>Rust[<ABI>]` — Build specific ABI
- `clean<BuildType>RustJniLibs` — Clean Rust build artifacts

### iOS
- `buildIosRust` — Build all Rust modules for all iOS targets
- `build<Module>IosRust[<target>]` — Build specific iOS target

> iOS build tasks **automatically run** before `linkFramework`, `cinterop`, and `compileKotlinIos` tasks.

### Desktop
- `buildDesktopRust` — Build all desktop targets
- `build<Module>DesktopRust[<target>]` — Build specific desktop target

### Tooling
- `cargoAdd` / `cargoAdd<Module>` — Add a Cargo dependency
- `cargoClean` / `cargoClean<Module>` — Run cargo clean
- `cargoCheck` / `cargoCheck<Module>` — Run cargo check
- `cargoClippy` / `cargoClippy<Module>` — Run clippy linter
- `cargoDoc` / `cargoDoc<Module>` — Generate docs
- `cargoFmt` / `cargoFmt<Module>` — Auto-format Rust code
- `cargoFmtCheck` / `cargoFmtCheck<Module>` — Check formatting

### Install
- `rustInstallBase` — Install rustup and toolchain
- `rustInstallBuild` — Install cargo-ndk and Android targets
- `rustInstallDesktop` — Install desktop targets (no cargo-ndk)
- `rustInstallIos` — Install iOS targets (no cargo-ndk)
- `rustInstallClippy` — Install clippy
- `rustInstallRustfmt` — Install rustfmt

---

## Output Paths

| Platform | Output Path |
|----------|-------------|
| Android | `build/intermediates/rust/<buildType>/jniLibs/<abi>/lib*.so` |
| Desktop | `build/intermediates/rust/desktop/resources/<platform-arch>/lib*` |
| iOS | `build/intermediates/rust/ios/output/<target-triple>/lib*.a` |

---

## Requirements

- **Android Gradle Plugin** 9.0+ (for Android targets)
- **Gradle** 9.0+
- **Rust toolchain** (auto-installed if missing)
- **cargo-ndk** (auto-installed for Android targets)
- **Android NDK** (install via Android Studio SDK Manager)

---

## Example Project

See [`Example-KMP-Mobile/`](Example-KMP-Mobile/) for a complete KMP project with:
- Android app with Rust JNI bindings
- iOS app with Rust via cinterop
- Shared Compose Multiplatform UI
- CI/CD via GitHub Actions

```bash
# Build Android
./gradlew :androidApp:assembleRelease

# Build iOS framework (Rust builds automatically)
./gradlew :composeApp:linkReleaseFrameworkIosSimulatorArm64
```

---

## Troubleshooting

#### cargo-ndk not found
The plugin auto-installs cargo-ndk for Android targets. If issues persist:
```bash
cargo install cargo-ndk
```

#### NDK not found
Install NDK via Android Studio: Tools → SDK Manager → SDK Tools → NDK (Side by side)

#### iOS: undefined symbols in Xcode
Use `isStatic = false` (dynamic framework) so Rust symbols are embedded:
```kotlin
iosTarget.binaries.framework {
    isStatic = false  // Embeds Rust symbols into framework
    linkerOpts("-L${rustOutputDir}", "-lrustios")
}
```

#### Library not found when running from Android Studio
```kotlin
Rust {
    module("library") {
        disableAbiOptimization = true
    }
}
```

#### iOS: no .a files produced
Ensure your `Cargo.toml` includes `staticlib`:
```toml
[lib]
crate-type = ["staticlib"]
```

#### Windows: rustup installation fails
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

---

## Credits

+ Rodroid Mods
+ Matrix dev

## License

MIT License

## Contributing

Contributions welcome! Please open an issue or pull request on GitHub.
