package dev.matrix.agp.rust

import dev.matrix.agp.rust.utils.Abi
import dev.matrix.agp.rust.utils.RustBinaries
import dev.matrix.agp.rust.utils.log
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

internal abstract class IosBuildTask : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Input
    abstract val rustBinaries: Property<RustBinaries>

    @get:Input
    abstract val abi: Property<Abi>

    @get:Input
    abstract val rustProfile: Property<String>

    @get:Input
    abstract val rustProjectDirectory: Property<File>

    @get:Input
    abstract val cargoTargetDirectory: Property<File>

    @get:Input
    abstract val iosOutputDirectory: Property<File>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val sourceFiles: ConfigurableFileCollection

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val cargoToml: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @TaskAction
    fun taskAction() {
        val rustBinaries = rustBinaries.get()
        val abi = abi.get()
        val rustProfile = rustProfile.get()
        val rustProjectDirectory = rustProjectDirectory.get()
        val cargoTargetDirectory = cargoTargetDirectory.get()
        val iosOutputDirectory = iosOutputDirectory.get()

        require(rustProjectDirectory.exists()) {
            "Rust project directory not found: $rustProjectDirectory"
        }

        val cargoTomlFile = File(rustProjectDirectory, "Cargo.toml")
        require(cargoTomlFile.exists()) {
            "Cargo.toml not found in: $rustProjectDirectory"
        }

        log("Building ${cargoTomlFile.parentFile.name} for ${abi.rustName} (${abi.rustTargetTriple})")

        try {
            execOperations.exec {
                standardOutput = System.out
                errorOutput = System.out
                workingDir = rustProjectDirectory

                environment("CARGO_TARGET_DIR", cargoTargetDirectory.absolutePath)

                commandLine(rustBinaries.cargo)
                args("build")
                args("--target", abi.rustTargetTriple)

                if (rustProfile.isNotEmpty() && rustProfile != "dev") {
                    args("--profile", rustProfile)
                }
            }.assertNormalExitValue()
        } catch (e: Exception) {
            throw GradleException(
                """
                iOS Rust build failed for ${abi.rustName} (${abi.rustTargetTriple})
                
                Possible solutions:
                - Ensure your Cargo.toml has [lib] crate-type = ["staticlib"] or ["cdylib", "staticlib"]
                - Install the target: rustup target add ${abi.rustTargetTriple}
                - Try running manually: cargo build --target ${abi.rustTargetTriple}
                
                Error: ${e.message}
                """.trimIndent(),
                e
            )
        }

        copyStaticLibToOutput(abi, rustProfile, cargoTargetDirectory, iosOutputDirectory)
    }

    private fun copyStaticLibToOutput(
        abi: Abi,
        rustProfile: String,
        cargoTargetDirectory: File,
        iosOutputDirectory: File,
    ) {
        val profileDir = when (rustProfile) {
            "dev", "" -> "debug"
            else -> rustProfile
        }

        val targetOutputDir = File(cargoTargetDirectory, "${abi.rustTargetTriple}/$profileDir")
        if (!targetOutputDir.exists()) {
            log("Warning: build output directory not found: $targetOutputDir")
            return
        }

        val outputDir = File(iosOutputDirectory, abi.rustName)
        outputDir.mkdirs()

        val staticLibFiles = targetOutputDir.listFiles { file ->
            file.isFile && file.name.endsWith(".a") && file.name.startsWith("lib")
        } ?: emptyArray()

        for (libFile in staticLibFiles) {
            val destFile = File(outputDir, libFile.name)
            libFile.copyTo(destFile, overwrite = true)
            log("Copied ${libFile.name} → ${outputDir.path}")
        }

        if (staticLibFiles.isEmpty()) {
            log("Warning: no .a static libraries found in $targetOutputDir — ensure Cargo.toml has crate-type = [\"staticlib\"]")
        }
    }
}
