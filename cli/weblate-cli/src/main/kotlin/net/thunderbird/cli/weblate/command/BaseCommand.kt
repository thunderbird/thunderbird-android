package net.thunderbird.cli.weblate.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import java.io.File
import net.thunderbird.cli.weblate.CliConfig
import net.thunderbird.cli.weblate.ComponentConfigLoader
import net.thunderbird.cli.weblate.api.ComponentConfig
import net.thunderbird.cli.weblate.api.WeblateClient
import net.thunderbird.cli.weblate.project.ModuleInfo

@Suppress("TooGenericExceptionCaught")
abstract class BaseCommand(name: String) : CliktCommand(name = name) {

    internal val config by requireObject<CliConfig>()

    override fun run() {
        val defaultComponentConfig = loadComponentConfig(config.componentConfigFile)
        val managedComponents = loadManagedConfig(config.managedComponentsFile)

        val client = WeblateClient(token = config.token, logLevel = config.logLevel)

        onRun(client, defaultComponentConfig, managedComponents)
    }

    abstract fun onRun(
        client: WeblateClient,
        defaultComponentConfig: ComponentConfig,
        managedComponents: Set<String>,
    )

    private fun loadComponentConfig(path: String): ComponentConfig {
        val file = File(path)
        if (!file.exists()) {
            error("Component config file not found: $path")
        }

        return try {
            ComponentConfigLoader().load(file)
        } catch (e: Exception) {
            error("Failed to load component config: ${e.message}")
        }
    }

    private fun loadManagedConfig(path: String): Set<String> {
        val file = File(path)
        if (!file.exists()) {
            error("Managed components file not found: $file — no components will be managed")
        }

        return try {
            file.readLines()
                .map { it.trim() }
                .map { line ->
                    // Remove inline comments
                    line.substringBefore('#').trim()
                }
                .filter { it.isNotEmpty() }
                .toSet()
        } catch (e: Exception) {
            error("Failed to read managed components file $file: ${e.message}")
        }
    }

    protected fun reportUnmanagedManagedComponents(
        localModules: List<ModuleInfo>,
        managedComponents: Set<String>,
    ) {
        val unmanaged = localModules.filter { it.slug !in managedComponents }

        if (unmanaged.isNotEmpty()) {
            println("\nLocal modules NOT in managed components file:")
            unmanaged.forEach { println("  - ${it.path} (${it.type})") }
        }
    }
}
