package dev.matrix.agp.rust

import dev.matrix.agp.rust.utils.Abi
import dev.matrix.agp.rust.utils.NullOutputStream
import dev.matrix.agp.rust.utils.Os
import dev.matrix.agp.rust.utils.RustBinaries
import dev.matrix.agp.rust.utils.SemanticVersion
import dev.matrix.agp.rust.utils.log
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import java.io.File

internal fun installRustUp(execOperations: ExecOperations, rustBinaries: RustBinaries) {
    try {
        val result = execOperations.exec {
            standardOutput = NullOutputStream
            errorOutput = NullOutputStream
            executable(rustBinaries.rustup)
            args("-V")
        }

        if (result.exitValue == 0) {
            return
        }
    } catch (_: Exception) {
    }

    log("installing rustup")

    when (Os.current.isWindows) {
        true -> {
            val tempFile = File.createTempFile("rustup-init", ".exe")
            try {
                execOperations.exec {
                    commandLine("powershell", "-Command",
                        "Invoke-WebRequest -Uri 'https://win.rustup.rs/x86_64' -OutFile '${tempFile.absolutePath}'")
                }.assertNormalExitValue()
                
                execOperations.exec {
                    commandLine(tempFile.absolutePath, "-y", "--default-toolchain", "stable")
                }.assertNormalExitValue()
            } finally {
                tempFile.delete()
            }
        }
        else -> {
            execOperations.exec {
                standardOutput = NullOutputStream
                errorOutput = NullOutputStream
                commandLine("bash", "-c", "\"curl\" --proto '=https' --tlsv1.2 -sSf https://sh.rustup.rs | sh -s -- -y")
            }.assertNormalExitValue()
        }
    }
}

internal fun installCargoNdk(execOperations: ExecOperations, rustBinaries: RustBinaries) {
    try {
        val result = execOperations.exec {
            standardOutput = NullOutputStream
            errorOutput = NullOutputStream
            executable(rustBinaries.cargoNdk)
            args("--version")
        }

        if (result.exitValue == 0) {
            return
        }
    } catch (_: Exception) {
    }

    log("installing cargo-ndk")

    execOperations.exec {
        standardOutput = NullOutputStream
        errorOutput = NullOutputStream
        executable(rustBinaries.cargo)
        args("install", "cargo-ndk")
    }.assertNormalExitValue()
}

internal fun installClippy(execOperations: ExecOperations, rustBinaries: RustBinaries) {
    try {
        val result = execOperations.exec {
            standardOutput = NullOutputStream
            errorOutput = NullOutputStream
            executable(rustBinaries.cargo)
            args("clippy", "--version")
        }

        if (result.exitValue == 0) {
            return
        }
    } catch (_: Exception) {
    }

    log("installing clippy")

    execOperations.exec {
        standardOutput = NullOutputStream
        errorOutput = NullOutputStream
        executable(rustBinaries.rustup)
        args("component", "add", "clippy")
    }.assertNormalExitValue()
}

internal fun installRustfmt(execOperations: ExecOperations, rustBinaries: RustBinaries) {
    try {
        val result = execOperations.exec {
            standardOutput = NullOutputStream
            errorOutput = NullOutputStream
            executable(rustBinaries.cargo)
            args("fmt", "--version")
        }

        if (result.exitValue == 0) {
            return
        }
    } catch (_: Exception) {
    }

    log("installing rustfmt")

    execOperations.exec {
        standardOutput = NullOutputStream
        errorOutput = NullOutputStream
        executable(rustBinaries.rustup)
        args("component", "add", "rustfmt")
    }.assertNormalExitValue()
}

internal fun updateRust(execOperations: ExecOperations, rustBinaries: RustBinaries) {
    log("updating rust version")

    execOperations.exec {
        standardOutput = NullOutputStream
        errorOutput = NullOutputStream
        executable(rustBinaries.rustup)
        args("update")
    }.assertNormalExitValue()
}

internal fun installRustTarget(execOperations: ExecOperations, abi: Abi, rustBinaries: RustBinaries) {
    log("installing rust target $abi (${abi.rustTargetTriple})")

    execOperations.exec {
        standardOutput = NullOutputStream
        errorOutput = NullOutputStream
        executable(rustBinaries.rustup)
        args("target", "add", abi.rustTargetTriple)
    }.assertNormalExitValue()
}

internal fun readRustCompilerVersion(execOperations: ExecOperations, rustBinaries: RustBinaries): SemanticVersion {
    val output = ByteArrayOutputStream()
    execOperations.exec {
        standardOutput = output
        errorOutput = NullOutputStream
        executable(rustBinaries.rustc)
        args("--version")
    }.assertNormalExitValue()

    val outputText = String(output.toByteArray())
    val regex = Regex("^rustc (\\d+\\.\\d+\\.\\d+)(-nightly)? .*$", RegexOption.DOT_MATCHES_ALL)
    val match = checkNotNull(regex.matchEntire(outputText)) {
        "failed to parse rust compiler version: $outputText"
    }

    return SemanticVersion(match.groupValues[1])
}

internal fun readRustUpInstalledTargets(execOperations: ExecOperations, rustBinaries: RustBinaries): Set<Abi> {
    val output = ByteArrayOutputStream()
    execOperations.exec {
        standardOutput = output
        errorOutput = NullOutputStream
        executable(rustBinaries.rustup)
        args("target", "list")
    }.assertNormalExitValue()

    val regex = Regex("^(\\S+) \\(installed\\)$", RegexOption.MULTILINE)
    return regex.findAll(String(output.toByteArray()))
        .mapNotNull { target ->
            Abi.values().find { it.rustTargetTriple == target.groupValues[1] }
        }
        .toSet()
}
