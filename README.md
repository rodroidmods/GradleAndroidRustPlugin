# Gradle Android Rust Plugin

A Gradle plugin for building Rust libraries with Cargo for Android projects.

## Version 1.0.0 - New Features

### 🚀 New in 1.0.0

#### **Cargo Add Support**
Add dependencies to your Cargo.toml from Gradle:

```bash
./gradlew cargoAdd --dependency serde@1
./gradlew cargoAddMyLib --dependency serde@1
./gradlew cargoAddMyLib --dependency serde@1 --args "--features derive"
```

#### **Cargo Check Support**
Fast syntax checking without a full build:

```bash
./gradlew cargoCheck
./gradlew cargoCheckMyLib
```

#### **Cargo Doc Support**
Generate Rust documentation:

```bash
./gradlew cargoDoc
./gradlew cargoDocMyLib
```

**New Tasks:**
- `cargoAdd` / `cargoAdd<ModuleName>` - Add a Cargo dependency
- `cargoCheck` / `cargoCheck<ModuleName>` - Run cargo check
- `cargoDoc` / `cargoDoc<ModuleName>` - Generate docs

#### **Support for AGP 9.0.0**
Updated the AGP to latest version, also compose bom too and gradle version to 9.3.0 newely latest

---

## Version 0.9.0 Features

#### **Cargo Clippy (Linting) Support**
Run `cargo clippy` to lint your Rust code and catch common mistakes:

```bash
./gradlew cargoClippy              # Run clippy on all Rust modules
./gradlew cargoClippyMyLib         # Run clippy on specific module
```

**Configure clippy strictness:**
```kotlin
androidRust {
    module("mylib") {
        path = file("src/main/jni")
        clippyDenyWarnings = true  // Fail build on warnings (strict mode)

        buildType("release") {
            clippyDenyWarnings = true  // Only strict in release builds
        }
    }
}
```

**Features:**
- Auto-installs clippy if not present
- Configurable per module and build type
- Fail on warnings or report only mode
- Full integration with Gradle task system

#### **Cargo Format Support**
Format and validate Rust code formatting:

```bash
./gradlew cargoFmt                 # Auto-format all Rust modules
./gradlew cargoFmtMyLib            # Auto-format specific module
./gradlew cargoFmtCheck            # Check formatting without modifying
./gradlew cargoFmtCheckMyLib       # Check specific module
```

**Features:**
- Auto-installs rustfmt if not present
- Two modes: auto-fix and check-only
- Perfect for CI/CD pipelines
- Full integration with Gradle task system

**New Tasks:**
- `cargoClippy` / `cargoClippy<ModuleName>` - Run linter
- `cargoFmt` / `cargoFmt<ModuleName>` - Auto-format code
- `cargoFmtCheck` / `cargoFmtCheck<ModuleName>` - Check formatting

---

## Version 0.8.0 Features

### 🚀 Cargo Clean Support
New `cargoClean` feature allows you to run `cargo clean` on your Rust modules:

**Manual clean (always available):**
```bash
./gradlew cargoClean              # Clean all Rust modules
./gradlew cargoCleanMyLib         # Clean specific module "mylib"
```

**Auto-clean with Gradle clean:**
Enable `cargoClean` to automatically run `cargo clean` when you run `./gradlew clean`:

```kotlin
androidRust {
    module("mylib") {
        path = file("src/main/jni")
        cargoClean = true  // Enable auto-clean for this module

        buildType("release") {
            cargoClean = true  // Or enable only for specific build types
        }
    }

    // Or enable globally for all modules
    cargoClean = true
}
```

**New Tasks:**
- `cargoClean` - Runs cargo clean for all Rust modules
- `cargoClean<ModuleName>` - Runs cargo clean for a specific module

---

## Version 0.7.0 Features

### 🚀 Major Improvements

#### 1. **cargo-ndk Integration**
The plugin now uses `cargo-ndk` instead of raw `cargo` commands for building Android libraries. This eliminates common linking errors and simplifies the build process.

**Benefits:**
- Automatic NDK environment configuration
- No manual environment variable setup needed
- Fewer linking errors and build failures
- Better compatibility with Android NDK
- Automatic rust home finding
- Fixed bugs

The plugin will automatically install `cargo-ndk` if it's not already available.

#### 2. **Full Windows Support**
Complete support for Windows development environment:
- Automatic rustup installation on Windows
- Proper handling of Windows executable paths (.cmd wrappers)
- Windows-specific rustup installation via PowerShell

#### 3. **Gradle Build Cache Support**
Proper Gradle task input/output annotations for intelligent caching:
- `@InputFiles` - Tracks Rust source files (*.rs, Cargo.toml, Cargo.lock)
- `@OutputDirectory` - Tracks JNI libs output directory
- Incremental builds - Only rebuilds when Rust sources change
- Better build performance in CI/CD environments

#### 4. **Parallel ABI Builds**
Multiple ABIs now build in parallel when using Gradle's `--parallel` flag:
- Significantly faster builds when targeting multiple architectures
- Optimal CPU utilization during compilation
- No sequential dependency chains between ABI builds

#### 5. **Enhanced Error Messages**
Detailed, actionable error messages when builds fail:
- Clear indication of what went wrong
- Specific suggestions for common issues
- Direct guidance on how to fix problems
- Better developer experience

#### 6. **Build Validation**
Pre-build validation to catch configuration errors early:
- Validates Rust project paths exist
- Checks for Cargo.toml presence
- Verifies NDK installation
- Ensures module configurations are complete

### 🔧 Configuration

#### Basic Setup

```kotlin
androidRust {
    module("mylib") {
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

#### Multiple Modules

```kotlin
androidRust {
    minimumSupportedRustVersion = "1.70.0"

    module("core") {
        path = file("src/main/rust/core")
        targets = listOf("arm64", "x86_64")

        buildType("release") {
            runTests = true
            clippyDenyWarnings = true
        }
    }

    module("audio") {
        path = file("src/main/rust/audio")
        targets = listOf("arm64", "x86_64", "arm", "x86")

        buildType("release") {
            runTests = true
        }
    }

    module("network") {
        path = file("src/main/rust/network")
        targets = listOf("arm64", "x86_64")
        cargoClean = true

        buildType("debug") {
            profile = "dev"
        }

        buildType("release") {
            profile = "release"
            clippyDenyWarnings = true
        }
    }
}
```

Each module produces a separate `.so` library:
- `libcore.so`
- `libaudio.so`
- `libnetwork.so`

#### Advanced Options

```kotlin
androidRust {
    minimumSupportedRustVersion = "1.70.0"

    module("mylib") {
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

### Configuration Options

| Option | Description | Default |
|--------|-------------|---------|
| `minimumSupportedRustVersion` | Minimum Rust version required | `""` (no check) |
| `path` | Path to Rust project directory | **Required** |
| `targets` | List of target ABIs | `["arm", "arm64", "x86", "x86_64"]` |
| `profile` | Rust build profile | `"release"` |
| `runTests` | Run `cargo test` before building | `null` (disabled) |
| `disableAbiOptimization` | Disable IDE ABI injection | `null` (false) |
| `cargoClean` | Run `cargo clean` with `./gradlew clean` | `null` (disabled) |
| `clippyDenyWarnings` | Fail build on clippy warnings | `null` (false) |

### Supported ABIs

| Rust Name | Android Name | Architecture |
|-----------|--------------|--------------|
| `arm` | `armeabi-v7a` | 32-bit ARM |
| `arm64` | `arm64-v8a` | 64-bit ARM |
| `x86` | `x86` | 32-bit x86 |
| `x86_64` | `x86_64` | 64-bit x86 |

### Requirements

- Android Gradle Plugin 7.0+
- Rust toolchain (will be auto-installed if missing)
- cargo-ndk (will be auto-installed if missing)
- Android NDK (install via Android Studio SDK Manager)

### How It Works

1. **Auto-Installation**: Plugin automatically installs rustup, Rust toolchain, cargo-ndk, and required target triples
2. **Validation**: Pre-build validation ensures all paths and configurations are correct
3. **Parallel Building**: cargo-ndk builds each target ABI, potentially in parallel
4. **Output Placement**: Compiled .so files are automatically placed in jniLibs directories
5. **Integration**: Android build system picks up the libraries automatically

### How to install

The puglin is avaiable here : https://plugins.gradle.org/plugin/io.github.rodroidmods.android-rust

### Cargo.toml Requirements

Your Rust library must be configured as a C dynamic library:

```toml
[package]
name = "mylib"
version = "0.1.0"
edition = "2021"

[lib]
crate-type = ["cdylib"]

[dependencies]
# your dependencies here
```

### Custom Rust Binary Paths

If you have Rust installed in a custom location, create `local.properties`:

```properties
cargo.bin=/custom/path/to/cargo/bin
```

The plugin will look for `cargo`, `cargo-ndk`, `rustc`, and `rustup` in this directory.

### Build Tasks

The plugin creates tasks for each build type and ABI combination:

- `clean<BuildType>RustJniLibs` - Clean Rust build artifacts
- `test<Module>Rust` - Run Rust tests (if enabled)
- `build<BuildType><Module>Rust[<ABI>]` - Build specific ABI
- `cargoAdd` / `cargoAdd<Module>` - Add a Cargo dependency
- `cargoClean` / `cargoClean<Module>` - Run cargo clean
- `cargoCheck` / `cargoCheck<Module>` - Run cargo check
- `cargoClippy` / `cargoClippy<Module>` - Run cargo clippy linter
- `cargoDoc` / `cargoDoc<Module>` - Generate docs
- `cargoFmt` / `cargoFmt<Module>` - Auto-format Rust code
- `cargoFmtCheck` / `cargoFmtCheck<Module>` - Check code formatting

Example tasks:
- `buildReleaseMyLibRust[arm64-v8a]`
- `buildDebugMyLibRust[x86_64]`
- `testMyLibRust`
- `cargoClean` / `cargoCleanMyLib`
- `cargoClippy` / `cargoClippyMyLib`
- `cargoFmt` / `cargoFmtMyLib`
- `cargoFmtCheck` / `cargoFmtCheckMyLib`

### Gradle Build Cache

The plugin fully supports Gradle's build cache. To enable:

```bash
./gradlew build --build-cache
```

Or add to `gradle.properties`:
```properties
org.gradle.caching=true
```

### Parallel Builds

To build multiple ABIs in parallel:

```bash
./gradlew build --parallel
```

Or add to `gradle.properties`:
```properties
org.gradle.parallel=true
```

### Troubleshooting

#### cargo-ndk not found
The plugin will automatically install cargo-ndk, but if you see errors, manually install:
```bash
cargo install cargo-ndk

and if you have issues with rust beign not founded, than just ad cargo.bin path in local.prop and problem will be fixed
```

#### NDK not found
Install NDK via Android Studio: Tools → SDK Manager → SDK Tools → NDK (Side by side)

#### Library not found when running from Android Studio
If you see "library not found" errors when running from Android Studio, set:
```kotlin
androidRust {
    module("mylib") {
        disableAbiOptimization = true
    }
}
```

This forces building all ABIs instead of just the IDE-injected target.

#### Windows: rustup installation fails
Ensure PowerShell execution policy allows running scripts:
```powershell
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
```

### Migration from 0.6.0

The plugin now uses `cargo-ndk` internally. No configuration changes are required, but you may need to install cargo-ndk:

```bash
cargo install cargo-ndk
```

All existing configurations will continue to work.

### Note

It is recommended to use the latest version 1.0.0, as previous versions have bugs that have been fixed.

### Credits

+ Rodroid Mods
+ Matrix dev

## License

MIT License

## Contributing

Contributions welcome! Please open an issue or pull request on GitHub.
