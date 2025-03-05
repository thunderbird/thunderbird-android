import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    id("com.diffplug.spotless")
}

configure<SpotlessExtension> {
    configureKotlinCheck(
        targets = listOf(
            "**/*.kt",
        ),
        project = project,
        libs = libs,
    )

    configureKotlinGradleCheck(
        targets = listOf(
            "*.gradle.kts",
        ),
        project = project,
        libs = libs,
    )

    configureMiscCheck()
}
