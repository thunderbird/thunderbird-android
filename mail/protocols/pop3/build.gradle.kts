plugins {
    id "thunderbird.library.jvm"
    alias(libs.plugins.android.lint)
}

if (rootProject.testCoverage) {
    apply plugin: 'jacoco'
}

dependencies {
    api project(":mail:common")

    testImplementation project(":mail:testing")
    testImplementation libs.okio
    testImplementation libs.jzlib
    testImplementation libs.commons.io
}
