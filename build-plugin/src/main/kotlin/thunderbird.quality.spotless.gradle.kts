import com.diffplug.gradle.spotless.SpotlessExtension

plugins {
    id("com.diffplug.spotless")
}

configure<SpotlessExtension> {
    kotlin {
        ktlint(libs.versions.ktlint.get())
        target("**/*.kt")
        targetExclude("**/build/", "**/resources/", "plugins/openpgp-api-lib/")
    }
    kotlinGradle {
        ktlint(libs.versions.ktlint.get())
        target("**/*.gradle.kts")
        targetExclude("**/build/")
    }
    format("markdown") {
        prettier()
        target("**/*.md")
        targetExclude("plugins/openpgp-api-lib/")
    }
    format("misc") {
        target("**/*.gradle", "**/.gitignore")
        trimTrailingWhitespace()
    }
}
