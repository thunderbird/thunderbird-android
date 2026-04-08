package net.thunderbird.cli.weblate

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import java.io.File
import net.thunderbird.cli.weblate.api.Component
import net.thunderbird.cli.weblate.api.ComponentConfig
import net.thunderbird.cli.weblate.api.WeblateClient

@Suppress("TooGenericExceptionCaught")
class WeblateCli : CliktCommand(
    name = "weblate",
) {
    private val token: String by option(
        help = "Weblate API token",
    ).required()

    private val dryRun: Boolean by option(
        help = "Dry run the command without making any changes",
    ).flag()

    private val goldenConfigPath: String by option(
        help = "Path to golden component config JSON",
    ).default("./cli/weblate-cli/golden-component-config.json")

    private val includeFilePath: String by option(
        help = "Path to file with component slug to include (one per line, '#' comments)",
    ).default("./cli/weblate-cli/include-components.txt")

    override fun help(context: Context): String = "Weblate CLI"

    override fun run() {
        val goldenConfig = loadGoldenConfig(goldenConfigPath)
        val includeConfig = loadIncludeConfig(includeFilePath)

        val client = WeblateClient()
        val components = client.loadComponents(token)

        println("Loaded ${components.size} components:")

        components.forEach { component ->
            println()
            println("- ${component.info.name} (slug: ${component.info.slug} # ID: ${component.info.id}) ")
            println()

            if (!includeConfig.contains(component.info.slug)) {
                println("  ⏭\uFE0F  skipped (not listed in include file)")
            } else {
                processComponent(component, goldenConfig, client)
            }
            println()
        }
    }

    @Suppress("NestedBlockDepth")
    private fun processComponent(component: Component, goldenConfig: ComponentConfig, client: WeblateClient) {
        val diffs = ComponentConfigDiff.computeConfigDiff(goldenConfig, component.config, 1)

        if (diffs.isEmpty()) {
            println("  ✅ Config matches common config")
        } else {
            println("  ⚠\uFE0F  Config differs (dry-run). Diff:")
            println()
            diffs.forEach { println("     $it") }
            if (!dryRun) {
                try {
                    val result = client.patchComponent(token, component.info.url, goldenConfig)
                    if (result) {
                        println("    ✅ Updated component config successfully")
                    } else {
                        println("    ❌ Failed to update component config: API request failed")
                    }
                } catch (e: Exception) {
                    println("    ❌ Failed to update component config: ${e.message}")
                }
            }
        }
    }

    private fun loadGoldenConfig(path: String): ComponentConfig {
        val file = File(path)
        if (!file.exists()) {
            error("Golden config file not found: $path")
        }

        return try {
            ComponentConfigLoader().load(file)
        } catch (e: Exception) {
            error("Failed to load golden config: ${e.message}")
        }
    }

    private fun loadIncludeConfig(path: String): Set<String> {
        val file = File(path)
        if (!file.exists()) {
            error("Include file not found: $file — no components will be managed")
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
            error("Failed to read include file $file: ${e.message}")
        }
    }
}
