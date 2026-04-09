package net.thunderbird.cli.weblate

import net.thunderbird.cli.weblate.api.ComponentConfig

object ComponentConfigDiff {

    fun computeConfigDiff(expected: ComponentConfig, actual: ComponentConfig, indentLevel: Int = 0): List<String> {
        return fields.mapNotNull { it.diff(expected, actual, indentLevel) }
    }

    private fun <T> value(
        name: String,
        selector: (ComponentConfig) -> T,
    ): DiffField = ValueField(name, selector)

    private fun set(
        name: String,
        selector: (ComponentConfig) -> List<String>,
    ): DiffField = SetField(name, selector)

    private fun multiline(
        name: String,
        selector: (ComponentConfig) -> String,
    ): DiffField = MultilineField(name, selector)

    private val fields: List<DiffField> = listOf(
        value("license") { it.license },
        value("license_url") { it.licenseUrl },
        value("agreement") { it.agreement },
        value("priority") { it.priority },
        value("is_glossary") { it.isGlossary },
        value("glossary_color") { it.glossaryColor },

        value("enable_suggestions") { it.enableSuggestions },
        value("suggestion_voting") { it.suggestionVoting },
        value("suggestion_autoaccept") { it.suggestionAutoaccept },

        value("allow_translation_propagation") { it.allowTranslationPropagation },

        value("check_flags") { it.checkFlags },
        value("variant_regex") { it.variantRegex },
        set("enforced_checks") { it.enforcedChecks },
        value("secondary_language") { it.secondaryLanguage },

        value("repoweb") { it.repoweb },
        value("push_on_commit") { it.pushOnCommit },
        value("commit_pending_age") { it.commitPendingAge },
        value("auto_lock_error") { it.autoLockError },

        multiline("commit_message") { it.commitMessage },
        multiline("add_message") { it.addMessage },
        multiline("delete_message") { it.deleteMessage },
        multiline("merge_message") { it.mergeMessage },
        multiline("addon_message") { it.addonMessage },
        multiline("pull_message") { it.pullMessage },

        value("language_regex") { it.languageRegex },
        value("key_filter") { it.keyFilter },

        value("file_format_params.xml_closing_tags") { it.fileFormatParams.xmlClosingTags },

        value("edit_template") { it.editTemplate },
        value("intermediate") { it.intermediate },
        value("new_lang") { it.newLang },
        value("language_code_style") { it.languageCodeStyle },
        value("screenshot_filemask") { it.screenshotFilemask },
    )
}

private interface DiffField {
    fun diff(expected: ComponentConfig, actual: ComponentConfig, indentLevel: Int): String?
}

private class ValueField<T>(
    private val name: String,
    private val selector: (ComponentConfig) -> T,
) : DiffField {
    override fun diff(expected: ComponentConfig, actual: ComponentConfig, indentLevel: Int): String? {
        val expectedValue = selector(expected)
        val actualValue = selector(actual)
        val indent = " ".repeat(indentLevel * 2)

        return if (expectedValue != actualValue) {
            "$indent$name: expected=$expectedValue, actual=$actualValue"
        } else {
            null
        }
    }
}

private class SetField(
    private val name: String,
    private val selector: (ComponentConfig) -> List<String>,
) : DiffField {
    override fun diff(expected: ComponentConfig, actual: ComponentConfig, indentLevel: Int): String? {
        val expectedValue = selector(expected)
        val actualValue = selector(actual)

        return if (expectedValue.toSet() != actualValue.toSet()) {
            listDiff(name, expectedValue, actualValue, indentLevel)
        } else {
            null
        }
    }

    private fun listDiff(name: String, expected: List<String>, actual: List<String>, indentLevel: Int): String {
        val indent = " ".repeat(indentLevel * 2)
        val expectedSet = expected.toSet()
        val actualSet = actual.toSet()

        val missing = expected.filter { it !in actualSet }
        val unexpected = actual.filter { it !in expectedSet }

        val inner = buildString {
            if (missing.isNotEmpty()) {
                appendLine("missing:")
                missing.forEach { appendLine("  - $it") }
            }
            if (unexpected.isNotEmpty()) {
                appendLine("unexpected:")
                unexpected.forEach { appendLine("  + $it") }
            }
        }.trimEnd()

        return if (inner.isEmpty()) {
            ""
        } else {
            buildString {
                appendLine("$indent$name:")
                append(indentText(inner, indentLevel + 1))
            }.trimEnd()
        }
    }
}

private class MultilineField(
    private val name: String,
    private val selector: (ComponentConfig) -> String,
) : DiffField {
    override fun diff(expected: ComponentConfig, actual: ComponentConfig, indentLevel: Int): String? {
        val expectedValue = selector(expected)
        val actualValue = selector(actual)

        return if (expectedValue != actualValue) {
            multilineDiff(name, expectedValue, actualValue, indentLevel)
        } else {
            null
        }
    }

    private fun multilineDiff(name: String, expected: String, actual: String, indentLevel: Int): String {
        val indent = " ".repeat(indentLevel * 2)
        val expectedLines = expected.lines()
        val actualLines = actual.lines()
        val max = maxOf(expectedLines.size, actualLines.size)

        val inner = buildString {
            for (i in 0 until max) {
                val exp = expectedLines.getOrNull(i)
                val act = actualLines.getOrNull(i)
                if (exp != act) {
                    val expText = exp ?: "<missing>"
                    val actText = act ?: "<missing>"
                    appendLine("     [${i + 1}] expected: $expText")
                    appendLine("     [${i + 1}] actual  : $actText")
                }
            }
        }.trimEnd()

        return if (inner.isEmpty()) {
            ""
        } else {
            buildString {
                appendLine("$indent$name:")
                append(indentText(inner, indentLevel + 1))
            }.trimEnd()
        }
    }
}

/**
 * Indent a multi-line string by a given indent level. Each level equals 2 spaces by default.
 */
private fun indentText(text: String, level: Int, spacesPerLevel: Int = 2): String =
    text.lines().joinToString("\n") { " ".repeat(level * spacesPerLevel) + it }
