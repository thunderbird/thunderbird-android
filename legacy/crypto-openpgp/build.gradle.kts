plugins {
    id(ThunderbirdPlugins.Library.android)
}

dependencies {
    implementation(projects.legacy.core)
}

android {
    namespace = "com.fsck.k9.crypto.openpgp"
}

codeCoverage {
    lineCoverage.set(67)
}
