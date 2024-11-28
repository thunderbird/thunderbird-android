import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    id("com.diffplug.spotless")
}

configure<SpotlessExtension> {
    configureKotlinGradleCheck(
        targets = listOf(
            "*.gradle.kts",
            "build-plugin/**/*.gradle.kts",
        ),
        project = project,
        libs = libs,
    )

    configureMarkdownCheck(
        targets = listOf(
            "*.md",
            "docs/**/*.md",
        ),
    )

    configureMiscCheck()
}
