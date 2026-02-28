plugins {
    id("org.jetbrains.kotlin.jvm")
}

val os = System.getProperty("os.name").lowercase()
val arch = System.getProperty("os.arch").lowercase()
val isMacX64 = os.contains("mac") && arch == "x86_64"

kotlin {
    jvmToolchain {
        languageVersion = JavaLanguageVersion.of(11)
        // Fix for ADOPTIUM not working on Mac OS x86_64
        vendor.set(if (isMacX64) JvmVendorSpec.AZUL else JvmVendorSpec.ADOPTIUM)
    }
}

dependencies {
    testImplementation(libs.konsist)
    testImplementation(libs.kotlin.test)
}

tasks.withType<Test> {
    outputs.upToDateWhen { false }
}
