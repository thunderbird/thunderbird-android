package net.thunderbird.cli.weblate.project

data class ModuleInfo(
    val path: String,
    val type: ResourceType,
    val name: String,
    val slug: String,
)
