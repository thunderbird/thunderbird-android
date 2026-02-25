plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
}

dependencies {
    testImplementation(libs.konsist)
    testImplementation(libs.kotlin.test)
}

tasks.withType<Test> {
    outputs.upToDateWhen { false }
}
