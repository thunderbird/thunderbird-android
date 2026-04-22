package net.thunderbird.cli.weblate

data class WeblateConfig(
    val token: String,
    val componentConfigFile: String,
    val managedComponentsFile: String,
    val dryRun: Boolean,
)
