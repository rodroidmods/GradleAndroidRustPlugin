package dev.matrix.agp.rust

import dev.matrix.agp.rust.utils.RustBinaries
import dev.matrix.agp.rust.utils.log
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

internal abstract class CargoAddTask : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Input
    abstract val rustBinaries: Property<RustBinaries>

    @get:Input
    abstract val rustProjectDirectory: Property<File>

    @get:Input
    abstract val moduleName: Property<String>

    @get:Input
    abstract val dependency: Property<String>

    @get:Input
    abstract val extraArgs: ListProperty<String>

    init {
        dependency.convention("")
        extraArgs.convention(emptyList())
    }

    @Option(option = "dependency", description = "Cargo dependency spec, e.g. serde@1")
    fun setDependency(value: String) {
        dependency.set(value)
    }

    @Option(option = "args", description = "Additional cargo add args, space-separated")
    fun setArgs(value: String) {
        val parsed = value.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        extraArgs.set(parsed)
    }

    @TaskAction
    fun taskAction() {
        val rustBinaries = rustBinaries.get()
        val rustProjectDirectory = rustProjectDirectory.get()
        val moduleName = moduleName.get()
        val dependency = dependency.get().trim()
        val extraArgs = extraArgs.get()

        if (!rustProjectDirectory.exists()) {
            log("Skipping cargo add for '$moduleName': directory does not exist: $rustProjectDirectory")
            return
        }

        val cargoTomlFile = File(rustProjectDirectory, "Cargo.toml")
        if (!cargoTomlFile.exists()) {
            log("Skipping cargo add for '$moduleName': Cargo.toml not found in: $rustProjectDirectory")
            return
        }

        if (dependency.isEmpty()) {
            throw GradleException("cargoAdd requires --dependency <name[@version]>")
        }

        log("Running cargo add for '$moduleName' in $rustProjectDirectory")

        try {
            execOperations.exec {
                standardOutput = System.out
                errorOutput = System.out
                workingDir = rustProjectDirectory

                commandLine(rustBinaries.cargo)
                args("add", dependency)
                if (extraArgs.isNotEmpty()) {
                    args(extraArgs)
                }
            }.assertNormalExitValue()

            log("Cargo add completed for '$moduleName'")
        } catch (e: Exception) {
            throw GradleException(
                """
                Cargo add failed for '$moduleName'

                Possible solutions:
                - Ensure your Cargo version supports cargo add or install cargo-edit
                - Check the dependency spec: $dependency
                - Run manually: cargo add $dependency

                Error: ${e.message}
                """.trimIndent(),
                e
            )
        }
    }
}
