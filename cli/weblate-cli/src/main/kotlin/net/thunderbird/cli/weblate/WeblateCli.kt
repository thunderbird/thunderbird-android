package net.thunderbird.cli.weblate

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.enum
import io.ktor.client.plugins.logging.LogLevel

@Suppress("TooGenericExceptionCaught")
class WeblateCli : CliktCommand(
    name = "weblate",
) {
    internal val token: String by option(
        help = "Weblate API token",
    ).required()

    internal val componentConfigFile: String by option(
        "--component-config-file",
        help = "Path to component config JSON",
    ).default("./cli/weblate-cli/default-component-config.json")

    internal val managedComponentsFile: String by option(
        "--managed-components-file",
        help = "Path to file with managed component slugs (one per line, '#' comments)",
    ).default("./cli/weblate-cli/managed-components.txt")
    internal val dryRun: Boolean by option(
        help = "Dry run the command without making any changes",
    ).flag()

    internal val logLevel: LogLevel by option(
        "--log-level",
        help = "Log level for the Weblate API client",
    ).enum<LogLevel>(ignoreCase = true).default(LogLevel.NONE)

    override fun help(context: Context): String = "Weblate CLI"

    override fun run() {
        currentContext.findOrSetObject {
            CliConfig(
                token = token,
                componentConfigFile = componentConfigFile,
                managedComponentsFile = managedComponentsFile,
                dryRun = dryRun,
                logLevel = logLevel,
            )
        }
    }
}
