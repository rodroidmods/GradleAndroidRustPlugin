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

internal abstract class CargoFmtTask : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Input
    abstract val rustBinaries: Property<RustBinaries>

    @get:Input
    abstract val rustProjectDirectory: Property<File>

    @get:Input
    abstract val moduleName: Property<String>

    @TaskAction
    fun taskAction() {
        val rustBinaries = rustBinaries.get()
        val rustProjectDirectory = rustProjectDirectory.get()
        val moduleName = moduleName.get()

        if (!rustProjectDirectory.exists()) {
            log("Skipping cargo fmt for '$moduleName': directory does not exist: $rustProjectDirectory")
            return
        }

        val cargoTomlFile = File(rustProjectDirectory, "Cargo.toml")
        if (!cargoTomlFile.exists()) {
            log("Skipping cargo fmt for '$moduleName': Cargo.toml not found in: $rustProjectDirectory")
            return
        }

        log("Running cargo fmt for '$moduleName' in $rustProjectDirectory")

        try {
            execOperations.exec {
                standardOutput = System.out
                errorOutput = System.out
                workingDir = rustProjectDirectory

                commandLine(rustBinaries.cargo)
                args("fmt", "--all")
            }.assertNormalExitValue()

            log("Cargo fmt completed for '$moduleName'")
        } catch (e: Exception) {
            throw GradleException(
                """
                Cargo fmt failed for '$moduleName'

                Possible solutions:
                - Ensure rustfmt is installed: rustup component add rustfmt
                - Check for syntax errors in your Rust code
                - Run manually: cargo fmt --all

                Error: ${e.message}
                """.trimIndent(),
                e
            )
        }
    }
}
