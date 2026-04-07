package net.thunderbird.cli.weblate

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import net.thunderbird.cli.weblate.client.WeblateClient

class WeblateCli : CliktCommand(
    name = "weblate",
) {
    private val token: String by option(
        help = "Weblate API token",
    ).required()

    private val dryRun: Boolean by option(
        help = "Dry run the command without making any changes",
    ).flag()

    override fun help(context: Context): String = "Weblate CLI"

    override fun run() {
        val client = WeblateClient()
        val components = client.loadComponents(token)

        println("Loaded ${components.size} components:")

        components.forEach { component -> println("- ${component.name} (ID: ${component.id})") }
    }
}
