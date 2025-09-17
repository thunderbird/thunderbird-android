package net.thunderbird.gradle.plugin.quality.coverage

import org.gradle.api.provider.Property

internal const val DEFAULT_MIN_BRANCH_COVERAGE = 70
internal const val DEFAULT_MIN_LINE_COVERAGE = 75

interface CodeCoverageExtension {

    /**
     * Whether code coverage is disabled.
     */
    val disabled: Property<Boolean>

    /**
     * Minimum required branch coverage in percent (0-100).
     */
    val branchCoverage: Property<Int>

    /**
     * Minimum required line coverage in percent (0-100).
     */
    val lineCoverage: Property<Int>

}

internal fun CodeCoverageExtension.initialize() {
    disabled.convention(false)
    branchCoverage.convention(DEFAULT_MIN_BRANCH_COVERAGE)
    lineCoverage.convention(DEFAULT_MIN_LINE_COVERAGE)
}

internal fun CodeCoverageExtension.finalizeValueOnRead() {
    disabled.finalizeValueOnRead()
    branchCoverage.finalizeValueOnRead()
    lineCoverage.finalizeValueOnRead()
}
