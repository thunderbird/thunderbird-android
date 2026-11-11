package net.thunderbird.quality.util

/** A source finding. [location] is Konsist's absolute path with line and column. */
data class QualityViolation(
    val location: String,
    val reason: String,
    val suggestion: String? = null,
    val group: String? = null,
)

fun QualityViolation.render(): String = buildString {
    append("file://")
    append(location.removePrefix("file://"))
    append("\n  ")
    append(reason)
    suggestion?.let { append("\n  Fix: $it") }
}

fun renderQualityReport(rule: String, violations: Collection<QualityViolation>): String {
    val count = violations.size
    val heading = "$rule: $count ${if (count == 1) "violation" else "violations"}"
    val findings = violations.groupBy { it.group.orEmpty() }.toSortedMap().map { (group, findings) ->
        val body = findings.sortedWith(compareBy({ it.location }, { it.reason }, { it.suggestion.orEmpty() }))
            .joinToString(separator = "\n\n") { it.render() }
        if (group.isEmpty()) body else "$group\n\n$body"
    }.joinToString(separator = "\n\n")
    return if (findings.isEmpty()) heading else "$heading\n\n$findings"
}

fun assertNoQualityViolations(rule: String, violations: Collection<QualityViolation>) {
    if (violations.isNotEmpty()) throw AssertionError(renderQualityReport(rule, violations))
}
