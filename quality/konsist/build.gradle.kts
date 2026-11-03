plugins {
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    testImplementation(libs.konsist)
    testImplementation(libs.kotlin.test)
}

tasks.withType<Test> {
    outputs.upToDateWhen { false }
}
