package net.thunderbird.gradle.plugin.quality.coverage.filter

import kotlinx.kover.gradle.plugin.dsl.KoverReportFiltersConfig

internal fun KoverReportFiltersConfig.androidFilter() {
    excludes {
        classes(
            "*R$*",
            "*BuildConfig",
            "*Manifest*",
        )
    }
}
