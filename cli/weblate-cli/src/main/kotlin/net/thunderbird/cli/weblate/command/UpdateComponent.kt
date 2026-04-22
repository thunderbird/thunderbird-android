package net.thunderbird.cli.weblate.command

import com.github.ajalt.clikt.core.Context
import net.thunderbird.cli.weblate.ComponentConfigDiff
import net.thunderbird.cli.weblate.api.Component
import net.thunderbird.cli.weblate.api.ComponentConfig
import net.thunderbird.cli.weblate.api.ComponentPatch
import net.thunderbird.cli.weblate.api.WeblateClient

@Suppress("TooGenericExceptionCaught")
class UpdateComponent : BaseCommand(name = "update") {
    override fun help(context: Context): String = "Update managed components"

    override fun onRun(
        client: WeblateClient,
        componentConfig: ComponentConfig,
        managedComponents: Set<String>,
    ) {
        val allComponents = client.loadComponents()
        val (managed, skipped) = allComponents.partition { it.info.slug in managedComponents }

        println("Loaded ${allComponents.size} components (${managed.size} managed, ${skipped.size} skipped):")

        managed.forEach { component ->
            println()
            println("- ${component.info.name} (slug: ${component.info.slug} # ID: ${component.info.id}) ")
            println()
            processComponent(component, componentConfig, client)
            println()
        }

        if (skipped.isNotEmpty()) {
            println("-------")
            println()
            println("Skipped components (not in managed list):")
            println()
            skipped.forEach { println("${it.info.slug} # ID: ${it.info.id}") }
            println()
        }
    }

    @Suppress("NestedBlockDepth")
    private fun processComponent(component: Component, componentConfig: ComponentConfig, client: WeblateClient) {
        val diffs = ComponentConfigDiff.computeConfigDiff(componentConfig, component.config, 1)

        if (diffs.isEmpty()) {
            println("  ✅ Config matches common config")
        } else {
            println("  ⚠\uFE0F  Config differs:")
            println()
            diffs.forEach { println("     $it") }
            if (!config.dryRun) {
                try {
                    val result = client.patchComponent(
                        component.info.url,
                        ComponentPatch(
                            category = component.info.category,
                            linkedComponent = component.info.linkedComponent,
                            config = componentConfig,
                            locked = component.info.locked,
                        ),
                    )
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
}
