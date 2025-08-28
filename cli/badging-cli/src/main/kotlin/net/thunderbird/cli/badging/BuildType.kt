package net.thunderbird.cli.badging

enum class BuildType {
    RELEASE,
    BETA,
    DAILY,
    DEBUG,
    ;

    override fun toString(): String {
        return name.lowercase()
    }

    fun toCamelCase(): String {
        return name.lowercase().replaceFirstChar { it.uppercase() }
    }
}
