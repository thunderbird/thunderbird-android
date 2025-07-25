plugins {
    id("org.jetbrains.kotlinx.kover")
}

/**
 * To enable Kover provide the `testCoverageEnabled` property by adding `./gradlew koverHtmlReport -PtestCoverageEnabled`.
 */

val testCoverageEnabled = hasProperty("testCoverageEnabled")

kover {
    if (!testCoverageEnabled) {
        disable()
    }

    reports {
        filters {
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

        verify {
            rule("line-coverage") {
                minBound(60)
            }
        }
    }
}
