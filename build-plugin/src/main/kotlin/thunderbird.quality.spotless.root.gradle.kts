import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    id("com.diffplug.spotless")
}

configure<SpotlessExtension> {
    kotlin {
        target(
            "build-plugin/src/*/kotlin/*.kt",
            "build-plugin/src/*/kotlin/**/*.kt",
        )
        targetExclude(
            "build/",
            "build-plugin/build/",
        )

        ktlint(libs.versions.ktlint.get())
            .setEditorConfigPath("${project.rootProject.projectDir}/.editorconfig")
            .editorConfigOverride(kotlinEditorConfigOverride)
    }

    kotlinGradle {
        target(
            "*.gradle.kts",
            "build-plugin/*.gradle.kts",
            "build-plugin/src/*/kotlin/*.kts",
            "build-plugin/src/*/kotlin/**/*.kts",
        )
        targetExclude(
            "build/",
            "build-plugin/build/",
        )

        ktlint(libs.versions.ktlint.get())
            .setEditorConfigPath("${project.rootProject.projectDir}/.editorconfig")
            .editorConfigOverride(
                mapOf(
                    "ktlint_standard_function-signature" to "disabled",
                    "ktlint_code_style" to "intellij_idea",
                ),
            )
    }

    flexmark {
        target(
            "*.md",
            "docs/*.md",
            "docs/**/*.md",
        )
        targetExclude(
            "build/",
        )
        flexmark()
    }

    format("misc") {
        target(".gitignore")
        trimTrailingWhitespace()
    }
}
