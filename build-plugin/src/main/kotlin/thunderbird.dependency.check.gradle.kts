import com.github.benmanes.gradle.versions.updates.DependencyUpdatesTask

// The versions plugin is applied in settings.gradle.kts so every project contributes to the report.
tasks.withType<DependencyUpdatesTask> {
    // Compose Hot Reload's tooling configurations fail to resolve and only hold the plugin's own runtime.
    filterConfigurations = Spec { !it.name.startsWith("composeHotReload") }
    rejectVersionIf {
        isNonStable(candidate.version) && !isNonStable(currentVersion)
    }
}

fun isNonStable(version: String): Boolean {
    val stableKeyword = listOf("RELEASE", "FINAL", "GA").any { version.uppercase().contains(it) }
    val regex = "^[\\d,.v-]+(-r)?$".toRegex()
    val isStable = stableKeyword || regex.matches(version)
    return isStable.not()
}
