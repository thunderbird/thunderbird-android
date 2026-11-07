package net.thunderbird.app.common

/**
 * Prefixes of the build types added by the Baseline Profile plugin, see `BaselineProfileProducerAppPlugin`.
 */
private val BASELINE_PROFILE_BUILD_TYPE_PREFIXES = listOf("nonMinified", "benchmark")

/**
 * Returns the build type used to select the feature flag overrides.
 *
 * The Baseline Profile plugin adds copies of the app build types, like `nonMinifiedDaily` and `benchmarkDaily`.
 * They use the flags of the build type they copy, `daily` in this example, so profiles and benchmarks run the same
 * code as the published app.
 */
internal fun featureFlagBuildType(buildType: String): String {
    val prefix = BASELINE_PROFILE_BUILD_TYPE_PREFIXES.firstOrNull { prefix ->
        buildType.length > prefix.length && buildType.startsWith(prefix)
    } ?: return buildType

    return buildType.removePrefix(prefix).replaceFirstChar { it.lowercaseChar() }
}
