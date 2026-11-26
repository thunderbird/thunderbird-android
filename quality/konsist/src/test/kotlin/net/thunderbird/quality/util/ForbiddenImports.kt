package net.thunderbird.quality.util

/**
 * Fails if any project import matches [isForbidden].
 *
 * The report groups the violations by import name, shows the replacement from [replacementFor] next to each name, and
 * lists every location as a `file://` link.
 */
fun assertNoForbiddenImports(
    message: String,
    replacementFor: (importName: String) -> String,
    isForbidden: (importName: String) -> Boolean,
) {
    val violations = projectScope.imports.filter { isForbidden(it.name) }
    if (violations.isEmpty()) return

    val report = violations.groupBy { it.name }
        .toSortedMap()
        .entries
        .joinToString(separator = "\n") { (importName, imports) ->
            val locations = imports.map { "  file://${it.location}" }.sorted()
            (listOf("$importName -> ${replacementFor(importName)}") + locations).joinToString(separator = "\n")
        }

    throw AssertionError("${violations.size} forbidden imports. $message\n\n$report")
}
