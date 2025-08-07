plugins {
    id("org.jetbrains.kotlinx.kover")
}

/**
 * To enable Kover provide the `enableKover` property by adding `./gradlew koverHtmlReport -PenableKover`.
 */

kover {
    if (!hasProperty("enableKover")) {
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
                minBound(0)
            }
        }
    }
}
