package net.thunderbird.cli.weblate.command

import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.mordant.terminal.YesNoPrompt
import net.thunderbird.cli.weblate.api.ComponentInfo
import net.thunderbird.cli.weblate.api.WeblateClient

@Suppress("TooGenericExceptionCaught", "MemberNameEqualsClassName")
class DeleteComponent : BaseCommand(name = "delete") {

    private val slugToDelete: String by option(
        "--slug",
        help = "The slug of the component to delete",
    ).required()

    override fun help(context: Context): String = "Delete a component from Weblate"

    override fun onRun(
        client: WeblateClient,
        defaultComponentConfig: net.thunderbird.cli.weblate.api.ComponentConfig,
        managedComponents: Set<String>,
    ) {
        val components = client.loadComponents()
        val component = components.find { it.info.slug == slugToDelete }

        if (component == null) {
            println()
            println("    ❌ Could not find component with slug: $slugToDelete")
            println()
            println("    Available slugs:")
            components.forEach { println("        ${it.info.slug}") }
            return
        }

        println("Found component: ${component.info.name} (slug: ${component.info.slug} # ID: ${component.info.id})")

        if (config.dryRun) {
            println("    Dry run: would delete component")
        } else {
            if (YesNoPrompt("    Are you sure you want to delete this component?", terminal).ask() == true) {
                executeDeleteComponent(client, component.info)
            } else {
                println("    Deletion cancelled.")
            }
        }
    }

    private fun executeDeleteComponent(
        client: WeblateClient,
        info: ComponentInfo,
    ) {
        try {
            val success = client.deleteComponent(info.url)
            if (success) {
                println("    ✅ Deleted component successfully")
            } else {
                println("    ❌ Failed to delete component: API request failed")
            }
        } catch (e: Exception) {
            println("    ❌ Failed to delete component: ${e.message}")
        }
    }
}
