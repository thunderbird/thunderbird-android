package net.thunderbird.gradle.plugin.app.baselineprofile

import org.gradle.api.Project

/**
 * Whether the benchmark and baseline profile variants are built, enabled with `-Pbenchmark=true`.
 *
 * They are off by default because they double the minified app variants and make a full build much heavier.
 */
internal fun Project.isBenchmarkEnabled(): Boolean =
    providers.gradleProperty("benchmark").map(String::toBoolean).getOrElse(false)
