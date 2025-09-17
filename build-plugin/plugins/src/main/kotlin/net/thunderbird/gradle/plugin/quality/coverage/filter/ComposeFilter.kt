package net.thunderbird.gradle.plugin.quality.coverage.filter

import kotlinx.kover.gradle.plugin.dsl.KoverReportFiltersConfig

internal fun KoverReportFiltersConfig.composeFilter() {
    excludes {
        annotatedBy(
            "androidx.compose.ui.tooling.preview.Preview",
            "androidx.compose.ui.tooling.preview.PreviewLightDark",
            "app.k9mail.core.ui.compose.common.annotation.PreviewDevices",
            "app.k9mail.core.ui.compose.common.annotation.PreviewDevicesWithBackground",
            "app.k9mail.core.ui.compose.designsystem.PreviewLightDarkLandscape",
        )
    }
}
