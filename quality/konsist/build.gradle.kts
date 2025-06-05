plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    testImplementation(libs.konsist)
    testImplementation(libs.kotlin.test)
}
