package net.thunderbird.quality.util

import com.lemonappdev.konsist.api.provider.KoLocationProvider
import com.lemonappdev.konsist.api.provider.KoNameProvider
import com.lemonappdev.konsist.api.provider.KoPathProvider

/**
 * Fails if any of [declarations] is not in [baseline].
 *
 * A baseline entry is `<path from the project root>::<name>`, so declarations with the same name in different files
 * stay apart. An entry covers every declaration with that key, such as overloads in one file. The report lists every
 * new violation as a `file://` link. It also fails on baseline entries that no longer match a declaration, so the
 * baseline can only shrink.
 */
fun <T> assertNoViolations(
    message: String,
    declarations: List<T>,
    baseline: Set<String> = emptySet(),
) where T : KoNameProvider, T : KoPathProvider, T : KoLocationProvider {
    val keyed = declarations.groupBy { "${it.projectPath.removePrefix("/")}::${it.name}" }
    val violations = keyed.filterKeys { it !in baseline }
    val staleEntries = baseline - keyed.keys
    if (violations.isEmpty() && staleEntries.isEmpty()) return

    val report = buildList {
        violations.map { (key, matches) ->
            (listOf("  $key") + matches.map { "    file://${it.location}" }).joinToString(separator = "\n")
        }.sorted().let(::addAll)
        if (staleEntries.isNotEmpty()) {
            add("Fixed, remove from the baseline:")
            staleEntries.sorted().forEach { add("  $it") }
        }
    }.joinToString(separator = "\n")

    throw AssertionError(
        "${violations.size} violations, ${staleEntries.size} stale baseline entries. $message\n\n$report",
    )
}
