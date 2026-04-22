package net.thunderbird.cli.weblate

import io.ktor.client.plugins.logging.LogLevel

data class WeblateConfig(
    val token: String,
    val componentConfigFile: String,
    val managedComponentsFile: String,
    val dryRun: Boolean,
    val logLevel: LogLevel,
)
