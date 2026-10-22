plugins {
    alias(libs.plugins.tb.baselineprofile.producer.app)
}

android {
    namespace = "com.fsck.k9.benchmarks"
    targetProjectPath = ":app-k9mail"
}

dependencies {
    implementation(projects.benchmarks)
}
