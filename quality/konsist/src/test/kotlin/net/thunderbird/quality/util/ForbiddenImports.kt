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
    assertNoQualityViolations(
        rule = message,
        violations = violations.map {
            QualityViolation(
                location = it.location,
                reason = "Forbidden import: ${it.name}",
                suggestion = replacementFor(it.name),
                group = it.name,
            )
        },
    )
}
