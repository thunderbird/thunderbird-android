package net.thunderbird.cli.badging

enum class ProductFlavor {
    FULL,
    FOSS,
    ;

    override fun toString(): String {
        return name.lowercase()
    }

    fun toCamelCase(): String {
        return name.lowercase().replaceFirstChar { it.uppercase() }
    }
}
