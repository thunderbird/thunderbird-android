package net.thunderbird.cli.weblate.project

import java.io.File

class ModuleDiscovery {

    fun discoverLocalModules(): List<ModuleInfo> {
        val root = File(ROOT_PATH)
        val modules = mutableMapOf<String, ResourceType>()

        root.walkTopDown()
            .onEnter { dir -> !isSkippedDirectory(dir.name) }
            .forEach { file ->
                val type = getResourceType(file)
                if (type != null) {
                    val modulePath = extractModulePath(file.path)
                    if (modulePath != null && modulePath != "." && !isExcluded(modulePath)) {
                        modules[modulePath] = type
                    }
                }
            }

        return modules.map { (path, type) ->
            ModuleInfo(
                path = path,
                type = type,
                name = path.replace("/", ":"),
                slug = path.replace("/", "-"),
            )
        }
    }

    private fun isSkippedDirectory(name: String): Boolean {
        return name in SKIPPED_DIRECTORIES || name.startsWith("values-")
    }

    private fun getResourceType(file: File): ResourceType? {
        if (file.name != "strings.xml") return null
        val path = file.path
        return when {
            path.contains("/res/values") -> ResourceType.ANDROID
            path.contains("/composeResources/values") -> ResourceType.COMPOSE
            else -> null
        }
    }

    private fun extractModulePath(path: String): String? {
        val normalizedPath = path.removePrefix("./")
        return when {
            normalizedPath.contains("/src/") -> normalizedPath.substringBefore("/src/")
            normalizedPath.contains("/composeResources/") -> normalizedPath.substringBefore("/composeResources/")
            else -> null
        }
    }

    private fun isExcluded(path: String): Boolean {
        return EXCLUDED_PATHS.any { path.contains(it) }
    }

    private companion object {
        private const val ROOT_PATH = "."
        private val SKIPPED_DIRECTORIES = setOf(".git", "build", ".gradle", "gradle", "tmp")
        private val EXCLUDED_PATHS = setOf("app-ui-catalog", "openpgp-api", "/test/")
    }
}
