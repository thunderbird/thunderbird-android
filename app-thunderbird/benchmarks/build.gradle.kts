plugins {
    alias(libs.plugins.tb.baselineprofile.producer.app)
}

android {
    namespace = "net.thunderbird.benchmarks"
    targetProjectPath = ":app-thunderbird"

    buildTypes {
        create("beta") {
            matchingFallbacks += listOf("release")
        }
        create("daily") {
            matchingFallbacks += listOf("release")
        }
    }
}

dependencies {
    implementation(projects.benchmarks)
}
