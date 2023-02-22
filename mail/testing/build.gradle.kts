plugins {
    id "thunderbird.library.jvm"
    alias(libs.plugins.android.lint)
}

if (rootProject.testCoverage) {
    apply plugin: 'jacoco'
}

dependencies {
    api project(":mail:common")

    api libs.okio
    api libs.junit
}
