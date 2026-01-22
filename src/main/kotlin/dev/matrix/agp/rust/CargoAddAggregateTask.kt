package dev.matrix.agp.rust

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.options.Option

internal abstract class CargoAddAggregateTask : DefaultTask() {
    @Option(option = "dependency", description = "Cargo dependency spec, e.g. serde@1")
    fun setDependency(value: String) {
        project.tasks.withType(CargoAddTask::class.java).configureEach {
            dependencySpec.set(value)
        }
    }

    @Option(option = "args", description = "Additional cargo add args, space-separated")
    fun setArgs(value: String) {
        val parsed = value.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        project.tasks.withType(CargoAddTask::class.java).configureEach {
            extraArgs.addAll(parsed)
        }
    }

    @Option(option = "arg", description = "Single cargo add arg (repeatable)")
    fun addArg(value: String) {
        project.tasks.withType(CargoAddTask::class.java).configureEach {
            extraArgs.add(value)
        }
    }

    @Option(option = "features", description = "Comma or space separated feature list")
    fun setFeatures(value: String) {
        project.tasks.withType(CargoAddTask::class.java).configureEach {
            featuresSpec.set(value)
        }
    }
}
