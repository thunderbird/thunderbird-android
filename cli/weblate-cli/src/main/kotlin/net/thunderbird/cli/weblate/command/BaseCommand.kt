package net.thunderbird.cli.weblate.command

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.requireObject
import java.io.File
import net.thunderbird.cli.weblate.ComponentConfigLoader
import net.thunderbird.cli.weblate.WeblateConfig
import net.thunderbird.cli.weblate.api.ComponentConfig
import net.thunderbird.cli.weblate.api.WeblateClient

@Suppress("TooGenericExceptionCaught")
abstract class BaseCommand(name: String) : CliktCommand(name = name) {

    internal val config by requireObject<WeblateConfig>()

    override fun run() {
        val componentConfig = loadComponentConfig(config.componentConfigFile)
        val managedComponents = loadManagedConfig(config.managedComponentsFile)

        val client = WeblateClient(token = config.token, logLevel = config.logLevel)

        onRun(client, componentConfig, managedComponents)
    }

    abstract fun onRun(
        client: WeblateClient,
        componentConfig: ComponentConfig,
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
                    // Remove inline comments safely; substringBefore handles missing '#'
                    line.substringBefore('#').trim()
                }
                .filter { it.isNotEmpty() }
                .toSet()
        } catch (e: Exception) {
            error("Failed to read managed components file $file: ${e.message}")
        }
    }
}
