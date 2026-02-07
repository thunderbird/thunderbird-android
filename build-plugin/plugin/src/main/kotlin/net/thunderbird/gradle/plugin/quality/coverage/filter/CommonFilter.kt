package net.thunderbird.gradle.plugin.quality.coverage.filter

import kotlinx.kover.gradle.plugin.dsl.KoverReportFiltersConfig

internal fun KoverReportFiltersConfig.commonFilter() {
    excludes {
        annotatedBy(
            "*Generated*",
            "**Generated**",
            "*Generated",
        )
    }
}
