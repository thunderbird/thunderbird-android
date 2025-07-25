plugins {
    id("org.jetbrains.kotlinx.kover")
}

kover {
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
