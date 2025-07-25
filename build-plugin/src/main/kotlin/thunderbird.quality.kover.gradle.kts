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
                annotatedBy("androidx.compose.ui.tooling.preview.Preview")
            }
        }

        verify {
            rule("line-coverage") {
                minBound(60)
            }
        }
    }
}
