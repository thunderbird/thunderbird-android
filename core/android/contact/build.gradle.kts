plugins {
    id(ThunderbirdPlugins.Library.android)
}

android {
    namespace = "net.thunderbird.core.android.contact"
}

dependencies {
    implementation(projects.mail.common)
}

codeCoverage {
    branchCoverage.set(0)
    lineCoverage.set(0)
}
