# Gradle Android Rust Plugin

A Gradle plugin for building Rust libraries with Cargo for Android, Desktop JVM, and iOS projects with Kotlin Multiplatform (KMP) support.

## Version 2.0.0 - KMP Support

### 🚀 New in 2.0.0

#### **Kotlin Multiplatform Support**
Build Rust libraries for Desktop JVM and iOS targets alongside Android:

```kotlin
androidRust {
    module("mylib") {
        path = file("rust/mylib")
        targets = listOf(
            "arm64", "x86_64",                          // Android
            "desktop-linux-x64", "desktop-windows-x64", // Desktop
            "desktop-macos-arm64",
            "ios-arm64", "ios-sim-arm64"                 // iOS
        )
    }
}
```

#### **Desktop Targets**
Plain `cargo build --target` (no NDK required). Libraries placed in JVM resources for `System.loadLibrary()` / JNA:

| Target Name | Rust Triple | Output | Resource Path |
|-------------|-------------|--------|---------------|
| `desktop-linux-x64` | `x86_64-unknown-linux-gnu` | `.so` | `linux-x86-64/` |
| `desktop-windows-x64` | `x86_64-pc-windows-msvc` | `.dll` | `win32-x86-64/` |
| `desktop-macos-x64` | `x86_64-apple-darwin` | `.dylib` | `darwin-x86-64/` |
| `desktop-macos-arm64` | `aarch64-apple-darwin` | `.dylib` | `darwin-aarch64/` |

#### **iOS Targets**
Static libraries (`.a`) for Kotlin/Native `cinterop` linking:

| Target Name | Rust Triple | Output |
|-------------|-------------|--------|
| `ios-arm64` | `aarch64-apple-ios` | `.a` (Device) |
| `ios-sim-arm64` | `aarch64-apple-ios-sim` | `.a` (Simulator, Apple Silicon) |
| `ios-sim-x64` | `x86_64-apple-ios` | `.a` (Simulator, Intel) |
| `ios-macabi-arm64` | `aarch64-apple-ios-macabi` | `.a` (Mac Catalyst) |

#### **New Build Tasks**
- `buildDesktopRust` — Builds all Rust modules for all desktop targets
- `buildIosRust` — Builds all Rust modules for all iOS targets
- `build<Module>DesktopRust[<target>]` — Build specific desktop target
- `build<Module>IosRust[<target>]` — Build specific iOS target

#### **Separate Install Tasks**
- `rustInstallDesktop` — Installs desktop target triples (no cargo-ndk)
- `rustInstallIos` — Installs iOS target triples (no cargo-ndk)

#### **AGP 9.0.0 Support**
Fully compatible with Android Gradle Plugin 9.0.0 and Gradle 9.3.0.

---

## How to Install

The plugin is available on Gradle Plugin Portal: https://plugins.gradle.org/plugin/io.github.rodroidmods.android-rust

```kotlin
plugins {
    id("io.github.rodroidmods.android-rust") version "2.0.0"
}
```

---

## Configuration

### Android Only

```kotlin
androidRust {
    module("library") {
        path = file("../rust/mylib")
        targets = listOf("arm", "arm64", "x86", "x86_64")

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

### Android + Desktop (KMP)

```kotlin
androidRust {
    module("mylib") {
        path = file("rust/mylib")
        targets = listOf(
            "arm64", "x86_64",
            "desktop-linux-x64", "desktop-windows-x64", "desktop-macos-arm64"
        )
    }
}
```

### Android + iOS (KMP)

```kotlin
androidRust {
    module("mylib") {
        path = file("rust/mylib")
        targets = listOf(
            "arm64", "x86_64",
            "ios-arm64", "ios-sim-arm64"
        )
    }
}
```

### Full KMP (Android + Desktop + iOS)

```kotlin
androidRust {
    module("mylib") {
        path = file("rust/mylib")
        targets = listOf(
            "arm64", "x86_64",
            "desktop-linux-x64", "desktop-windows-x64", "desktop-macos-arm64",
            "ios-arm64", "ios-sim-arm64"
        )
    }
}
```

### Multiple Modules

```kotlin
androidRust {
    minimumSupportedRustVersion = "1.70.0"

    module("core") {
        path = file("src/main/rust/core")
        targets = listOf("arm64", "x86_64", "desktop-linux-x64", "ios-arm64")

        buildType("release") {
            runTests = true
            clippyDenyWarnings = true
        }
    }

    module("audio") {
        path = file("src/main/rust/audio")
        targets = listOf("arm64", "x86_64", "arm", "x86")
    }

    module("network") {
        path = file("src/main/rust/network")
        targets = listOf("arm64", "x86_64")
        cargoClean = true
    }
}
```

### Advanced Options

```kotlin
androidRust {
    minimumSupportedRustVersion = "1.70.0"

    module("library") {
        path = file("../rust/mylib")
        targets = listOf("arm64")
        runTests = true
        disableAbiOptimization = false

        buildType("debug") {
            profile = "dev"
        }

        buildType("release") {
            profile = "release"
        }
    }
}
```

---

## Configuration Options

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

## Cargo.toml Requirements

### Android Only
```toml
[lib]
crate-type = ["cdylib"]
```

### Android + Desktop
```toml
[lib]
crate-type = ["cdylib"]
```

### Android + iOS (or all platforms)
```toml
[lib]
crate-type = ["cdylib", "staticlib"]
```

iOS requires `staticlib` to produce `.a` static libraries for Kotlin/Native `cinterop`.

---

## Build Tasks

### Android
- `clean<BuildType>RustJniLibs` — Clean Rust build artifacts
- `test<Module>Rust` — Run Rust tests (if enabled)
- `build<BuildType><Module>Rust[<ABI>]` — Build specific ABI

### Desktop
- `buildDesktopRust` — Build all desktop targets
- `build<Module>DesktopRust[<target>]` — Build specific desktop target

### iOS
- `buildIosRust` — Build all iOS targets
- `build<Module>IosRust[<target>]` — Build specific iOS target

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

### Example Commands

```bash
./gradlew cargoAdd --dependency serde --features derive
./gradlew cargoClippy
./gradlew cargoFmt
./gradlew cargoFmtCheck
./gradlew cargoCheck
./gradlew cargoDoc
./gradlew buildDesktopRust
./gradlew buildIosRust
```

---

## Output Paths

### Android
`.so` files → `build/intermediates/rust/<buildType>/jniLibs/<abi>/`

### Desktop
Libraries → `build/intermediates/rust/desktop/resources/<platform-arch>/`

| Platform | Path |
|----------|------|
| Linux x64 | `linux-x86-64/libmylib.so` |
| Windows x64 | `win32-x86-64/mylib.dll` |
| macOS x64 | `darwin-x86-64/libmylib.dylib` |
| macOS arm64 | `darwin-aarch64/libmylib.dylib` |

### iOS
Static libraries → `build/intermediates/rust/ios/output/<target>/`

---

## Requirements

- Android Gradle Plugin 9.0+ (for Android targets)
- Gradle 9.0+
- Rust toolchain (auto-installed if missing)
- cargo-ndk (auto-installed for Android targets)
- Android NDK (install via Android Studio SDK Manager, for Android targets)

---

## Custom Rust Binary Paths

If you have Rust installed in a custom location, create `local.properties`:

```properties
cargo.bin=/custom/path/to/cargo/bin
```

---

## Gradle Build Cache

```bash
./gradlew build --build-cache
```

Or add to `gradle.properties`:
```properties
org.gradle.caching=true
```

## Parallel Builds

```bash
./gradlew build --parallel
```

Or add to `gradle.properties`:
```properties
org.gradle.parallel=true
```

---

## Troubleshooting

#### cargo-ndk not found
The plugin auto-installs cargo-ndk for Android targets. If issues persist:
```bash
cargo install cargo-ndk
```

If Rust is not found, add `cargo.bin` path in `local.properties`.

#### NDK not found
Install NDK via Android Studio: Tools → SDK Manager → SDK Tools → NDK (Side by side)

#### Library not found when running from Android Studio
```kotlin
androidRust {
    module("library") {
        disableAbiOptimization = true
    }
}
```

#### Windows: rustup installation fails
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

#### iOS: no .a files produced
Ensure your `Cargo.toml` includes `staticlib`:
```toml
[lib]
crate-type = ["cdylib", "staticlib"]
```

---

## Credits

+ Rodroid Mods
+ Matrix dev

## License

MIT License

## Contributing

Contributions welcome! Please open an issue or pull request on GitHub.
