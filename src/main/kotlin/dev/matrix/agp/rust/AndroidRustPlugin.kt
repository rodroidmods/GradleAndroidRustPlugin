package dev.matrix.agp.rust

import com.android.build.gradle.internal.tasks.factory.dependsOn
import dev.matrix.agp.rust.utils.Abi
import dev.matrix.agp.rust.utils.RustBinaries
import dev.matrix.agp.rust.utils.SemanticVersion
import dev.matrix.agp.rust.utils.getAndroidComponentsExtension
import dev.matrix.agp.rust.utils.getAndroidExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import org.gradle.process.ExecOperations
import java.io.File
import javax.inject.Inject

@Suppress("unused")
abstract class AndroidRustPlugin @Inject constructor(
    private val execOperations: ExecOperations,
) : Plugin<Project> {
    override fun apply(project: Project) {
        val rustBinaries = RustBinaries(project)
        val extension = project.extensions.create("androidRust", AndroidRustExtension::class.java)
        val androidExtension = project.getAndroidExtension()
        val androidComponents = project.getAndroidComponentsExtension()
        val tasksByBuildType = HashMap<String, ArrayList<TaskProvider<RustBuildTask>>>()

        androidComponents.finalizeDsl { _ ->
            for ((moduleName, module) in extension.modules) {
                try {
                    val modulePath = module.path
                    require(modulePath.exists()) {
                        "Rust module '$moduleName': path does not exist: $modulePath"
                    }

                    val cargoToml = File(modulePath, "Cargo.toml")
                    require(cargoToml.exists()) {
                        "Rust module '$moduleName': Cargo.toml not found at $modulePath"
                    }
                } catch (e: UninitializedPropertyAccessException) {
                    throw IllegalStateException("Rust module '$moduleName': path must be specified")
                }
            }

            // Register cargoClean tasks for each module
            val cargoCleanTasks = mutableListOf<TaskProvider<CargoCleanTask>>()
            val modulesWithAutoCargoClean = mutableListOf<String>()

            for ((moduleName, module) in extension.modules) {
                val moduleNameCap = moduleName.replaceFirstChar(Char::titlecase)
                val cargoCleanTaskName = "cargoClean${moduleNameCap}"

                val cargoCleanTask = project.tasks.register(cargoCleanTaskName, CargoCleanTask::class.java) {
                    this.rustBinaries.set(rustBinaries)
                    this.rustProjectDirectory.set(module.path)
                    this.moduleName.set(moduleName)
                    this.description = "Runs cargo clean for Rust module '$moduleName'"
                    this.group = "rust"
                }
                cargoCleanTasks.add(cargoCleanTask)

                // Check if any build type has cargoClean enabled
                val hasCargoCleanEnabled = module.buildTypes.values.any { it.cargoClean == true }
                    || module.cargoClean == true
                    || extension.cargoClean == true

                if (hasCargoCleanEnabled) {
                    modulesWithAutoCargoClean.add(moduleName)
                }
            }

            // Register aggregate cargoClean task that runs all module cargo cleans
            if (cargoCleanTasks.isNotEmpty()) {
                project.tasks.register("cargoClean") {
                    this.description = "Runs cargo clean for all Rust modules"
                    this.group = "rust"
                    this.dependsOn(cargoCleanTasks)
                }
            }

            // Hook cargoClean to Gradle clean task for modules with cargoClean enabled
            project.afterEvaluate {
                val gradleCleanTask = project.tasks.findByName("clean")
                if (gradleCleanTask != null && modulesWithAutoCargoClean.isNotEmpty()) {
                    for ((moduleName, _) in extension.modules) {
                        if (modulesWithAutoCargoClean.contains(moduleName)) {
                            val moduleNameCap = moduleName.replaceFirstChar(Char::titlecase)
                            val cargoCleanTask = project.tasks.findByName("cargoClean${moduleNameCap}")
                            if (cargoCleanTask != null) {
                                gradleCleanTask.dependsOn(cargoCleanTask)
                            }
                        }
                    }
                }
            }

            val allRustAbiSet = mutableSetOf<Abi>()
            val ndkDirectory = androidExtension.ndkDirectory
            val ndkVersion = SemanticVersion(androidExtension.ndkVersion)
            val extensionBuildDirectory = project.layout.buildDirectory.dir("intermediates/rust").get().asFile

            for (buildType in androidExtension.buildTypes) {
                val buildTypeNameCap = buildType.name.replaceFirstChar(Char::titlecase)

                val variantBuildDirectory = File(extensionBuildDirectory, buildType.name)
                val variantJniLibsDirectory = File(variantBuildDirectory, "jniLibs")

                val cleanTaskName = "clean${buildTypeNameCap}RustJniLibs"
                val cleanTask = project.tasks.register(cleanTaskName, RustCleanTask::class.java) {
                    this.variantJniLibsDirectory.set(variantJniLibsDirectory)
                }

                for ((moduleName, module) in extension.modules) {
                    val moduleNameCap = moduleName.replaceFirstChar(Char::titlecase)
                    val moduleBuildDirectory = File(variantBuildDirectory, "lib_$moduleName")

                    val rustBuildType = module.buildTypes[buildType.name]
                    val rustConfiguration = mergeRustConfigurations(rustBuildType, module, extension)

                    val testTask = when (rustConfiguration.runTests) {
                        true -> {
                            val testTaskName = "test${moduleNameCap}Rust"
                            project.tasks.register(testTaskName, RustTestTask::class.java) {
                                this.rustBinaries.set(rustBinaries)
                                this.rustProjectDirectory.set(module.path)
                                this.cargoTargetDirectory.set(moduleBuildDirectory)
                            }.dependsOn(cleanTask)
                        }

                        else -> null
                    }

                    val rustAbiSet = resolveAbiList(project, rustConfiguration)
                    allRustAbiSet.addAll(rustAbiSet)

                    for (rustAbi in rustAbiSet) {
                        val buildTaskName = "build${buildTypeNameCap}${moduleNameCap}Rust[${rustAbi.androidName}]"
                        val buildTask = project.tasks.register(buildTaskName, RustBuildTask::class.java) {
                            this.rustBinaries.set(rustBinaries)
                            this.abi.set(rustAbi)
                            this.apiLevel.set(androidExtension.defaultConfig.minSdk ?: 21)
                            this.ndkVersion.set(ndkVersion)
                            this.ndkDirectory.set(ndkDirectory)
                            this.rustProfile.set(rustConfiguration.profile)
                            this.rustProjectDirectory.set(module.path)
                            this.cargoTargetDirectory.set(moduleBuildDirectory)
                            this.variantJniLibsDirectory.set(variantJniLibsDirectory)
                            this.cargoToml.set(project.layout.projectDirectory.file("${module.path.absolutePath}/Cargo.toml"))
                            this.sourceFiles.from(project.fileTree(module.path) {
                                include("**/*.rs")
                                include("**/Cargo.toml")
                                include("**/Cargo.lock")
                            })
                            this.outputDirectory.set(variantJniLibsDirectory)
                        }
                        buildTask.configure {
                            mustRunAfter(testTask ?: cleanTask)
                        }
                        tasksByBuildType.getOrPut(buildType.name, ::ArrayList).add(buildTask)
                    }
                }

                androidExtension.sourceSets.findByName(buildType.name)?.jniLibs?.srcDir(variantJniLibsDirectory)
            }

            val minimumSupportedRustVersion = SemanticVersion(extension.minimumSupportedRustVersion)
            installRustComponentsIfNeeded(
                execOperations,
                minimumSupportedRustVersion,
                allRustAbiSet,
                rustBinaries
            )
        }

        androidComponents.onVariants { variant ->
            val tasks = tasksByBuildType[variant.buildType] ?: return@onVariants
            val variantName = variant.name.replaceFirstChar(Char::titlecase)

            project.afterEvaluate {
                val parentTask = project.tasks.getByName("pre${variantName}Build")
                for (task in tasks) {
                    parentTask.dependsOn(task)
                }
            }
        }
    }

    private fun resolveAbiList(project: Project, config: AndroidRustConfiguration): Collection<Abi> {
        val requestedAbi = Abi.fromRustNames(config.targets)

        if (config.disableAbiOptimization == true) {
            return requestedAbi
        }

        val injectedAbi = Abi.fromInjectedBuildAbi(project)
        if (injectedAbi.isEmpty()) {
            return requestedAbi
        }

        val intersectionAbi = requestedAbi.intersect(injectedAbi)
        check(intersectionAbi.isNotEmpty()) {
            "ABIs requested by IDE ($injectedAbi) are not supported by the build config (${config.targets})"
        }

        return intersectionAbi.toList()
    }

    private fun mergeRustConfigurations(vararg configurations: AndroidRustConfiguration?): AndroidRustConfiguration {
        val defaultConfiguration = AndroidRustConfiguration().also {
            it.profile = "release"
            it.targets = Abi.values().mapTo(ArrayList(), Abi::rustName)
            it.runTests = null
            it.disableAbiOptimization = null
            it.cargoClean = null
        }

        return configurations.asSequence()
            .filterNotNull()
            .plus(defaultConfiguration)
            .reduce { result, base ->
                if (result.profile.isEmpty()) {
                    result.profile = base.profile
                }
                if (result.targets.isEmpty()) {
                    result.targets = base.targets
                }
                if (result.runTests == null) {
                    result.runTests = base.runTests
                }
                if (result.disableAbiOptimization == null) {
                    result.disableAbiOptimization = base.disableAbiOptimization
                }
                if (result.cargoClean == null) {
                    result.cargoClean = base.cargoClean
                }
                result
            }
    }
}