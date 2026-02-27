package dev.matrix.agp.rust

import dev.matrix.agp.rust.utils.Abi
import dev.matrix.agp.rust.utils.AndroidHelper
import dev.matrix.agp.rust.utils.RustBinaries
import dev.matrix.agp.rust.utils.SemanticVersion
import dev.matrix.agp.rust.utils.log
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
        project.extensions.add("Rust", extension)

        project.afterEvaluate {
            validateModules(extension)

            val minimumSupportedRustVersion = SemanticVersion(extension.minimumSupportedRustVersion)

            val rustInstallBase = project.tasks.register("rustInstallBase", RustInstallTask::class.java) {
                this.rustBinaries.set(rustBinaries)
                this.minimumSupportedRustVersion.set(minimumSupportedRustVersion)
            }

            val rustInstallClippy = project.tasks.register("rustInstallClippy", RustInstallTask::class.java) {
                this.rustBinaries.set(rustBinaries)
                this.minimumSupportedRustVersion.set(minimumSupportedRustVersion)
                this.installClippy.set(true)
            }

            val rustInstallRustfmt = project.tasks.register("rustInstallRustfmt", RustInstallTask::class.java) {
                this.rustBinaries.set(rustBinaries)
                this.minimumSupportedRustVersion.set(minimumSupportedRustVersion)
                this.installRustfmt.set(true)
            }

            registerCargoUtilityTasks(project, extension, rustBinaries, rustInstallBase, rustInstallClippy, rustInstallRustfmt)
            registerCargoCleanTasks(project, extension, rustBinaries)

            val extensionBuildDirectory = project.layout.buildDirectory.dir("intermediates/rust").get().asFile

            val androidHelper = AndroidHelper.create(project)
            if (androidHelper != null && androidHelper.hasAndroidPlugin()) {
                registerAndroidBuildTasks(project, extension, rustBinaries, minimumSupportedRustVersion, extensionBuildDirectory, androidHelper)
            }

            registerDesktopBuildTasks(project, extension, rustBinaries, minimumSupportedRustVersion, extensionBuildDirectory)
            registerIosBuildTasks(project, extension, rustBinaries, minimumSupportedRustVersion, extensionBuildDirectory)
        }
    }

    private fun validateModules(extension: AndroidRustExtension) {
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
    }

    private fun registerCargoUtilityTasks(
        project: Project,
        extension: AndroidRustExtension,
        rustBinaries: RustBinaries,
        rustInstallBase: TaskProvider<RustInstallTask>,
        rustInstallClippy: TaskProvider<RustInstallTask>,
        rustInstallRustfmt: TaskProvider<RustInstallTask>,
    ) {
        val cargoClippyTasks = mutableListOf<TaskProvider<CargoClippyTask>>()
        val cargoFmtTasks = mutableListOf<TaskProvider<CargoFmtTask>>()
        val cargoFmtCheckTasks = mutableListOf<TaskProvider<CargoFmtCheckTask>>()
        val cargoCheckTasks = mutableListOf<TaskProvider<CargoCheckTask>>()
        val cargoDocTasks = mutableListOf<TaskProvider<CargoDocTask>>()
        val cargoAddTasks = mutableListOf<TaskProvider<CargoAddTask>>()

        for ((moduleName, module) in extension.modules) {
            val moduleNameCap = moduleName.replaceFirstChar(Char::titlecase)
            val rustConfiguration = mergeRustConfigurations(module, extension)

            val cargoClippyTask = project.tasks.register("cargoClippy${moduleNameCap}", CargoClippyTask::class.java) {
                this.rustBinaries.set(rustBinaries)
                this.rustProjectDirectory.set(module.path)
                this.moduleName.set(moduleName)
                this.denyWarnings.set(rustConfiguration.clippyDenyWarnings ?: false)
                this.description = "Runs cargo clippy for Rust module '$moduleName'"
                this.group = "rust"
            }
            cargoClippyTask.configure { dependsOn(rustInstallClippy) }
            cargoClippyTasks.add(cargoClippyTask)

            val cargoFmtTask = project.tasks.register("cargoFmt${moduleNameCap}", CargoFmtTask::class.java) {
                this.rustBinaries.set(rustBinaries)
                this.rustProjectDirectory.set(module.path)
                this.moduleName.set(moduleName)
                this.description = "Runs cargo fmt for Rust module '$moduleName'"
                this.group = "rust"
            }
            cargoFmtTask.configure { dependsOn(rustInstallRustfmt) }
            cargoFmtTasks.add(cargoFmtTask)

            val cargoFmtCheckTask = project.tasks.register("cargoFmtCheck${moduleNameCap}", CargoFmtCheckTask::class.java) {
                this.rustBinaries.set(rustBinaries)
                this.rustProjectDirectory.set(module.path)
                this.moduleName.set(moduleName)
                this.description = "Checks cargo fmt for Rust module '$moduleName'"
                this.group = "rust"
            }
            cargoFmtCheckTask.configure { dependsOn(rustInstallRustfmt) }
            cargoFmtCheckTasks.add(cargoFmtCheckTask)

            val cargoCheckTask = project.tasks.register("cargoCheck${moduleNameCap}", CargoCheckTask::class.java) {
                this.rustBinaries.set(rustBinaries)
                this.rustProjectDirectory.set(module.path)
                this.moduleName.set(moduleName)
                this.description = "Runs cargo check for Rust module '$moduleName'"
                this.group = "rust"
            }
            cargoCheckTask.configure { dependsOn(rustInstallBase) }
            cargoCheckTasks.add(cargoCheckTask)

            val cargoDocTask = project.tasks.register("cargoDoc${moduleNameCap}", CargoDocTask::class.java) {
                this.rustBinaries.set(rustBinaries)
                this.rustProjectDirectory.set(module.path)
                this.moduleName.set(moduleName)
                this.description = "Runs cargo doc for Rust module '$moduleName'"
                this.group = "rust"
            }
            cargoDocTask.configure { dependsOn(rustInstallBase) }
            cargoDocTasks.add(cargoDocTask)

            val cargoAddTask = project.tasks.register("cargoAdd${moduleNameCap}", CargoAddTask::class.java) {
                this.rustBinaries.set(rustBinaries)
                this.rustProjectDirectory.set(module.path)
                this.moduleName.set(moduleName)
                this.description = "Runs cargo add for Rust module '$moduleName'"
                this.group = "rust"
            }
            cargoAddTask.configure { dependsOn(rustInstallBase) }
            cargoAddTasks.add(cargoAddTask)
        }

        registerAggregateTask(project, "cargoClippy", "Runs cargo clippy for all Rust modules", cargoClippyTasks)
        registerAggregateTask(project, "cargoFmt", "Runs cargo fmt for all Rust modules", cargoFmtTasks)
        registerAggregateTask(project, "cargoFmtCheck", "Checks cargo fmt for all Rust modules", cargoFmtCheckTasks)
        registerAggregateTask(project, "cargoCheck", "Runs cargo check for all Rust modules", cargoCheckTasks)
        registerAggregateTask(project, "cargoDoc", "Runs cargo doc for all Rust modules", cargoDocTasks)

        if (cargoAddTasks.isNotEmpty()) {
            project.tasks.register("cargoAdd", CargoAddAggregateTask::class.java) {
                this.description = "Runs cargo add for all Rust modules"
                this.group = "rust"
                this.dependsOn(cargoAddTasks)
            }
        }
    }

    private fun registerCargoCleanTasks(
        project: Project,
        extension: AndroidRustExtension,
        rustBinaries: RustBinaries,
    ) {
        val cargoCleanTasks = mutableListOf<TaskProvider<CargoCleanTask>>()
        val modulesWithAutoCargoClean = mutableListOf<String>()

        for ((moduleName, module) in extension.modules) {
            val moduleNameCap = moduleName.replaceFirstChar(Char::titlecase)

            val cargoCleanTask = project.tasks.register("cargoClean${moduleNameCap}", CargoCleanTask::class.java) {
                this.rustBinaries.set(rustBinaries)
                this.rustProjectDirectory.set(module.path)
                this.moduleName.set(moduleName)
                this.description = "Runs cargo clean for Rust module '$moduleName'"
                this.group = "rust"
            }
            cargoCleanTasks.add(cargoCleanTask)

            val hasCargoCleanEnabled = module.buildTypes.values.any { it.cargoClean == true }
                || module.cargoClean == true
                || extension.cargoClean == true

            if (hasCargoCleanEnabled) {
                modulesWithAutoCargoClean.add(moduleName)
            }
        }

        if (cargoCleanTasks.isNotEmpty()) {
            project.tasks.register("cargoClean") {
                this.description = "Runs cargo clean for all Rust modules"
                this.group = "rust"
                this.dependsOn(cargoCleanTasks)
            }
        }

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

    private fun registerAndroidBuildTasks(
        project: Project,
        extension: AndroidRustExtension,
        rustBinaries: RustBinaries,
        minimumSupportedRustVersion: SemanticVersion,
        extensionBuildDirectory: File,
        androidHelper: AndroidHelper,
    ) {
        val tasksByBuildType = HashMap<String, ArrayList<TaskProvider<RustBuildTask>>>()

        val rustInstallBuild = project.tasks.register("rustInstallBuild", RustInstallTask::class.java) {
            this.rustBinaries.set(rustBinaries)
            this.minimumSupportedRustVersion.set(minimumSupportedRustVersion)
            this.installCargoNdk.set(true)
            this.installTargets.set(true)
        }

        val allAndroidAbis = mutableSetOf<Abi>()
        val ndkDirectory = androidHelper.getNdkDirectory()
        if (ndkDirectory == null) {
            log("NDK not found, skipping Android Rust build tasks")
            return
        }
        val ndkVersion = SemanticVersion(androidHelper.getNdkVersion())
        val minSdk = androidHelper.getMinSdk()

        val buildTypeNames = androidHelper.getBuildTypeNames()

        for (buildTypeName in buildTypeNames) {
            val buildTypeNameCap = buildTypeName.replaceFirstChar(Char::titlecase)
            val variantBuildDirectory = File(extensionBuildDirectory, buildTypeName)
            val variantJniLibsDirectory = File(variantBuildDirectory, "jniLibs")

            val cleanTask = project.tasks.register("clean${buildTypeNameCap}RustJniLibs", RustCleanTask::class.java) {
                this.variantJniLibsDirectory.set(variantJniLibsDirectory)
            }

            for ((moduleName, module) in extension.modules) {
                val moduleNameCap = moduleName.replaceFirstChar(Char::titlecase)
                val moduleBuildDirectory = File(variantBuildDirectory, "lib_$moduleName")

                val rustBuildType = module.buildTypes[buildTypeName]
                val rustConfiguration = mergeRustConfigurations(rustBuildType, module, extension)

                val allAbis = resolveAbiList(project, rustConfiguration)
                val androidAbis = allAbis.filter { it.isAndroid }
                if (androidAbis.isEmpty()) continue

                val testTask = when (rustConfiguration.runTests) {
                    true -> {
                        project.tasks.register("test${moduleNameCap}Rust", RustTestTask::class.java) {
                            this.rustBinaries.set(rustBinaries)
                            this.rustProjectDirectory.set(module.path)
                            this.cargoTargetDirectory.set(moduleBuildDirectory)
                        }.also { task ->
                            task.configure { dependsOn(cleanTask) }
                            task.configure { dependsOn(project.tasks.named("rustInstallBase")) }
                        }
                    }
                    else -> null
                }

                allAndroidAbis.addAll(androidAbis)

                for (rustAbi in androidAbis) {
                    val buildTaskName = "build${buildTypeNameCap}${moduleNameCap}Rust[${rustAbi.androidName}]"
                    val buildTask = project.tasks.register(buildTaskName, RustBuildTask::class.java) {
                        this.rustBinaries.set(rustBinaries)
                        this.abi.set(rustAbi)
                        this.apiLevel.set(minSdk)
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
                    buildTask.configure { dependsOn(rustInstallBuild) }
                    buildTask.configure { mustRunAfter(testTask ?: cleanTask) }
                    tasksByBuildType.getOrPut(buildTypeName, ::ArrayList).add(buildTask)
                }
            }

            androidHelper.addJniLibsSourceSet(buildTypeName, variantJniLibsDirectory)
        }

        rustInstallBuild.configure { abiSet.set(allAndroidAbis) }

        for ((buildTypeName, tasks) in tasksByBuildType) {
            val buildTypeNameCap = buildTypeName.replaceFirstChar(Char::titlecase)

            project.tasks.matching { it.name == "pre${buildTypeNameCap}Build" }.configureEach {
                for (task in tasks) {
                    dependsOn(task)
                }
            }

            project.tasks.matching { it.name == "merge${buildTypeNameCap}NativeLibs" }.configureEach {
                for (task in tasks) {
                    dependsOn(task)
                }
            }
        }
    }

    private fun registerDesktopBuildTasks(
        project: Project,
        extension: AndroidRustExtension,
        rustBinaries: RustBinaries,
        minimumSupportedRustVersion: SemanticVersion,
        extensionBuildDirectory: File,
    ) {
        val allDesktopAbis = mutableSetOf<Abi>()
        val desktopBuildTasks = mutableListOf<TaskProvider<DesktopBuildTask>>()

        val rustInstallDesktop = project.tasks.register("rustInstallDesktop", RustInstallTask::class.java) {
            this.rustBinaries.set(rustBinaries)
            this.minimumSupportedRustVersion.set(minimumSupportedRustVersion)
            this.installCargoNdk.set(false)
            this.installTargets.set(true)
        }

        for ((moduleName, module) in extension.modules) {
            val moduleNameCap = moduleName.replaceFirstChar(Char::titlecase)
            val rustConfiguration = mergeRustConfigurations(module, extension)

            val allAbis = resolveAbiList(project, rustConfiguration)
            val desktopAbis = allAbis.filter { it.isDesktop }
            if (desktopAbis.isEmpty()) continue

            allDesktopAbis.addAll(desktopAbis)

            val variantBuildDirectory = File(extensionBuildDirectory, "desktop")
            val moduleBuildDirectory = File(variantBuildDirectory, "lib_$moduleName")
            val desktopResourcesDirectory = File(variantBuildDirectory, "resources")

            for (desktopAbi in desktopAbis) {
                val buildTaskName = "build${moduleNameCap}DesktopRust[${desktopAbi.rustName}]"
                val outputDir = File(desktopResourcesDirectory, desktopAbi.jvmResourcePath)

                val buildTask = project.tasks.register(buildTaskName, DesktopBuildTask::class.java) {
                    this.rustBinaries.set(rustBinaries)
                    this.abi.set(desktopAbi)
                    this.rustProfile.set(rustConfiguration.profile)
                    this.rustProjectDirectory.set(module.path)
                    this.cargoTargetDirectory.set(moduleBuildDirectory)
                    this.desktopResourcesDirectory.set(desktopResourcesDirectory)
                    this.cargoToml.set(project.layout.projectDirectory.file("${module.path.absolutePath}/Cargo.toml"))
                    this.sourceFiles.from(project.fileTree(module.path) {
                        include("**/*.rs")
                        include("**/Cargo.toml")
                        include("**/Cargo.lock")
                    })
                    this.outputDirectory.set(outputDir)
                    this.description = "Builds Rust module '$moduleName' for ${desktopAbi.rustName}"
                    this.group = "rust"
                }
                buildTask.configure { dependsOn(rustInstallDesktop) }
                desktopBuildTasks.add(buildTask)
            }
        }

        rustInstallDesktop.configure { abiSet.set(allDesktopAbis) }

        if (desktopBuildTasks.isNotEmpty()) {
            project.tasks.register("buildDesktopRust") {
                this.description = "Builds all Rust modules for desktop targets"
                this.group = "rust"
                this.dependsOn(desktopBuildTasks)
            }

            val processResources = project.tasks.findByName("processResources")
                ?: project.tasks.findByName("jvmProcessResources")
                ?: project.tasks.findByName("desktopProcessResources")
            if (processResources != null) {
                for (task in desktopBuildTasks) {
                    processResources.dependsOn(task)
                }
            }

            val desktopResourcesDir = File(extensionBuildDirectory, "desktop/resources")
            try {
                val sourceSets = project.extensions.getByName("sourceSets")
                if (sourceSets is org.gradle.api.tasks.SourceSetContainer) {
                    sourceSets.findByName("main")?.resources?.srcDir(desktopResourcesDir)
                }
            } catch (_: Exception) {
            }
        }
    }

    private fun registerIosBuildTasks(
        project: Project,
        extension: AndroidRustExtension,
        rustBinaries: RustBinaries,
        minimumSupportedRustVersion: SemanticVersion,
        extensionBuildDirectory: File,
    ) {
        val allIosAbis = mutableSetOf<Abi>()
        val iosBuildTasks = mutableListOf<TaskProvider<IosBuildTask>>()

        val rustInstallIos = project.tasks.register("rustInstallIos", RustInstallTask::class.java) {
            this.rustBinaries.set(rustBinaries)
            this.minimumSupportedRustVersion.set(minimumSupportedRustVersion)
            this.installCargoNdk.set(false)
            this.installTargets.set(true)
        }

        for ((moduleName, module) in extension.modules) {
            val moduleNameCap = moduleName.replaceFirstChar(Char::titlecase)
            val rustConfiguration = mergeRustConfigurations(module, extension)

            val allAbis = resolveAbiList(project, rustConfiguration)
            val iosAbis = allAbis.filter { it.isIos }
            if (iosAbis.isEmpty()) continue

            allIosAbis.addAll(iosAbis)

            val variantBuildDirectory = File(extensionBuildDirectory, "ios")
            val moduleBuildDirectory = File(variantBuildDirectory, "lib_$moduleName")
            val iosOutputDirectory = File(variantBuildDirectory, "output")

            for (iosAbi in iosAbis) {
                val buildTaskName = "build${moduleNameCap}IosRust[${iosAbi.rustName}]"
                val outputDir = File(iosOutputDirectory, iosAbi.rustTargetTriple)

                val buildTask = project.tasks.register(buildTaskName, IosBuildTask::class.java) {
                    this.rustBinaries.set(rustBinaries)
                    this.abi.set(iosAbi)
                    this.rustProfile.set(rustConfiguration.profile)
                    this.rustProjectDirectory.set(module.path)
                    this.cargoTargetDirectory.set(moduleBuildDirectory)
                    this.iosOutputDirectory.set(iosOutputDirectory)
                    this.cargoToml.set(project.layout.projectDirectory.file("${module.path.absolutePath}/Cargo.toml"))
                    this.sourceFiles.from(project.fileTree(module.path) {
                        include("**/*.rs")
                        include("**/Cargo.toml")
                        include("**/Cargo.lock")
                    })
                    this.outputDirectory.set(outputDir)
                    this.description = "Builds Rust module '$moduleName' for ${iosAbi.rustName}"
                    this.group = "rust"
                }
                buildTask.configure { dependsOn(rustInstallIos) }
                iosBuildTasks.add(buildTask)
            }
        }

        rustInstallIos.configure { abiSet.set(allIosAbis) }

        if (iosBuildTasks.isNotEmpty()) {
            val buildIosRustTask = project.tasks.register("buildIosRust") {
                this.description = "Builds all Rust modules for iOS targets"
                this.group = "rust"
                this.dependsOn(iosBuildTasks)
            }

            project.tasks.matching {
                it.name.startsWith("link") && it.name.contains("Framework") &&
                    (it.name.contains("Ios") || it.name.contains("ios"))
            }.configureEach {
                dependsOn(buildIosRustTask)
            }

            project.tasks.matching {
                it.name.startsWith("cinterop") && (it.name.contains("Ios") || it.name.contains("ios"))
            }.configureEach {
                dependsOn(buildIosRustTask)
            }

            project.tasks.matching {
                it.name.startsWith("compileKotlin") && (it.name.contains("Ios") || it.name.contains("ios"))
            }.configureEach {
                dependsOn(buildIosRustTask)
            }
        }
    }

    private fun <T : org.gradle.api.Task> registerAggregateTask(
        project: Project,
        name: String,
        description: String,
        tasks: List<TaskProvider<T>>,
    ) {
        if (tasks.isNotEmpty()) {
            project.tasks.register(name) {
                this.description = description
                this.group = "rust"
                this.dependsOn(tasks)
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

        val androidAbis = requestedAbi.filter { it.isAndroid }.toSet()
        val nonAndroidAbis = requestedAbi.filter { !it.isAndroid }

        val intersectionAbi = androidAbis.intersect(injectedAbi)
        if (androidAbis.isNotEmpty()) {
            check(intersectionAbi.isNotEmpty()) {
                "ABIs requested by IDE ($injectedAbi) are not supported by the build config (${config.targets})"
            }
        }

        return intersectionAbi.toList() + nonAndroidAbis
    }

    private fun mergeRustConfigurations(vararg configurations: AndroidRustConfiguration?): AndroidRustConfiguration {
        val defaultConfiguration = AndroidRustConfiguration().also {
            it.profile = "release"
            it.targets = Abi.androidEntries().map(Abi::rustName)
            it.runTests = null
            it.disableAbiOptimization = null
            it.cargoClean = null
            it.clippyDenyWarnings = null
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
                if (result.clippyDenyWarnings == null) {
                    result.clippyDenyWarnings = base.clippyDenyWarnings
                }
                result
            }
    }
}
