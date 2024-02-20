package net.thunderbird.cli.translation

class SupportedLanguagesFormatter {
    fun format(languageCodes: List<String>) = buildString {
        appendLine("<?xml version=\"1.0\" encoding=\"utf-8\"?>")
        appendLine("<resources>")
        appendLine("    <string-array name=\"supported_languages\" translatable=\"false\">")
        appendLine("        <item />")
        languageCodes.forEach {
            appendLine("        <item>$it</item>")
        }
        appendLine("    </string-array>")
        appendLine("</resources>")
    }.trim()
}
