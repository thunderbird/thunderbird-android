package net.thunderbird.cli.translation

class ResourceConfigurationsFormatter {
    fun format(languageCodes: List<String>) = buildString {
        appendLine("android {")
        appendLine("    defaultConfig {")
        appendLine("        resourceConfigurations.addAll(")
        appendLine("            listOf(")
        languageCodes.forEach { code ->
            appendLine("                \"$code\",")
        }
        appendLine("            ),")
        appendLine("        )")
        appendLine("    }")
        appendLine("}")
    }.trim()
}
