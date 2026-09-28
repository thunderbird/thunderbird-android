plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.feature.mail.message.composer"
}

dependencies {
    api(projects.core.common)
    api(projects.core.android.webkit.api)
}

codeCoverage {
    branchCoverage = 0
    lineCoverage = 0
}
