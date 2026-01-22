package dev.matrix.agp.rust

import dev.matrix.agp.rust.utils.Abi
import dev.matrix.agp.rust.utils.RustBinaries
import dev.matrix.agp.rust.utils.SemanticVersion
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import javax.inject.Inject

internal abstract class RustInstallTask : DefaultTask() {
    @get:Inject
    abstract val execOperations: ExecOperations

    @get:Input
    abstract val rustBinaries: Property<RustBinaries>

    @get:Input
    abstract val minimumSupportedRustVersion: Property<SemanticVersion>

    @get:Input
    abstract val installCargoNdk: Property<Boolean>

    @get:Input
    abstract val installClippy: Property<Boolean>

    @get:Input
    abstract val installRustfmt: Property<Boolean>

    @get:Input
    abstract val installTargets: Property<Boolean>

    @get:Input
    abstract val abiSet: SetProperty<Abi>

    init {
        minimumSupportedRustVersion.convention(SemanticVersion(0, 0, 0))
        installCargoNdk.convention(false)
        installClippy.convention(false)
        installRustfmt.convention(false)
        installTargets.convention(false)
        abiSet.convention(emptySet())
    }

    @TaskAction
    fun taskAction() {
        val rustBinaries = rustBinaries.get()
        val minimumSupportedRustVersion = minimumSupportedRustVersion.get()
        val installCargoNdk = installCargoNdk.get()
        val installClippy = installClippy.get()
        val installRustfmt = installRustfmt.get()
        val installTargets = installTargets.get()
        val abiSet = abiSet.get()

        installRustUp(execOperations, rustBinaries)

        if (minimumSupportedRustVersion.isValid) {
            val actualVersion = readRustCompilerVersion(execOperations, rustBinaries)
            if (actualVersion < minimumSupportedRustVersion) {
                updateRust(execOperations, rustBinaries)
            }
        }

        if (installCargoNdk) {
            installCargoNdk(execOperations, rustBinaries)
        }
        if (installClippy) {
            installClippy(execOperations, rustBinaries)
        }
        if (installRustfmt) {
            installRustfmt(execOperations, rustBinaries)
        }
        if (installTargets && abiSet.isNotEmpty()) {
            val installedAbiSet = readRustUpInstalledTargets(execOperations, rustBinaries)
            for (abi in abiSet) {
                if (installedAbiSet.contains(abi)) {
                    continue
                }
                installRustTarget(execOperations, abi, rustBinaries)
            }
        }
    }
}
