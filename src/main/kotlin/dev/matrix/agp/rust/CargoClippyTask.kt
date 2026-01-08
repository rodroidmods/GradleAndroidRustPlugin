package dev.matrix.agp.rust

import dev.matrix.agp.rust.utils.RustBinaries
import dev.matrix.agp.rust.utils.log
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

internal abstract class CargoClippyTask : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Input
    abstract val rustBinaries: Property<RustBinaries>

    @get:Input
    abstract val rustProjectDirectory: Property<File>

    @get:Input
    abstract val moduleName: Property<String>

    @get:Input
    abstract val denyWarnings: Property<Boolean>

    @TaskAction
    fun taskAction() {
        val rustBinaries = rustBinaries.get()
        val rustProjectDirectory = rustProjectDirectory.get()
        val moduleName = moduleName.get()
        val denyWarnings = denyWarnings.get()

        if (!rustProjectDirectory.exists()) {
            log("Skipping cargo clippy for '$moduleName': directory does not exist: $rustProjectDirectory")
            return
        }

        val cargoTomlFile = File(rustProjectDirectory, "Cargo.toml")
        if (!cargoTomlFile.exists()) {
            log("Skipping cargo clippy for '$moduleName': Cargo.toml not found in: $rustProjectDirectory")
            return
        }

        log("Running cargo clippy for '$moduleName' in $rustProjectDirectory")

        try {
            execOperations.exec {
                standardOutput = System.out
                errorOutput = System.out
                workingDir = rustProjectDirectory

                commandLine(rustBinaries.cargo)
                args("clippy", "--all-targets", "--all-features")

                if (denyWarnings) {
                    args("--", "-D", "warnings")
                }
            }.assertNormalExitValue()

            log("Cargo clippy completed for '$moduleName'")
        } catch (e: Exception) {
            throw GradleException(
                """
                Cargo clippy failed for '$moduleName'

                Possible solutions:
                - Fix the clippy warnings/errors in your Rust code
                - Set clippyDenyWarnings = false to allow warnings
                - Run manually: cargo clippy --all-targets --all-features

                Error: ${e.message}
                """.trimIndent(),
                e
            )
        }
    }
}
