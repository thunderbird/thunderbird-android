import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension

plugins {
    id("io.gitlab.arturbosch.detekt")
}

configure<DetektExtension> {
    source = project.files(
        project.file(project.rootDir),
    )

    config = project.rootProject.files("config/detekt/detekt.yml")
    baseline = project.rootProject.file("config/detekt/baseline.yml")
}

tasks.withType<Detekt>().configureEach {
    jvmTarget = ThunderbirdProjectConfig.javaVersion.toString()

    exclude(
        "**/.gradle/**",
        "**/.idea/**",
        "**/build/**",
        ".github/**",
        "gradle/**",
    )

    reports {
        html.required.set(true)
        sarif.required.set(true)
        xml.required.set(true)
    }
}
