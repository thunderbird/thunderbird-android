import groovy.json.JsonOutput

plugins {
    id(ThunderbirdPlugins.Library.jvm)
    alias(libs.plugins.android.lint)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(projects.backend.api)
    implementation(projects.core.common)
    implementation(projects.feature.mail.folder.api)

    implementation(libs.kotlinx.serialization.json)

    testImplementation(projects.mail.testing)
}

tasks.register<UpdateDemoMailbox>("updateDemoMailbox") {
    group = "demo"
    description = "Update mailbox/contents.json from src/main/resources/mailbox contents."

    inputDir.set(layout.projectDirectory.dir("src/main/resources/mailbox"))
    outputFile.set(layout.projectDirectory.file("src/main/resources/mailbox/contents.json"))
}

@CacheableTask
abstract class UpdateDemoMailbox : DefaultTask() {

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inputDir: DirectoryProperty

    @get:OutputFile
    abstract val outputFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val mailboxRoot = inputDir.get().asFile

        val topLevelFolderMap = mailboxRoot.listFiles()
            ?.filter { it.isDirectory }
            ?.sortedBy { it.name }
            ?.mapNotNull { folder ->
                buildFolderNode(folder).takeIf { it.first }?.let { folder.name to it.second }
            }
            ?.toMap(LinkedHashMap()) ?: linkedMapOf()

        // Ensure special folders exist (even if empty)
        SPECIAL_FOLDERS.forEach { id ->
            topLevelFolderMap.putIfAbsent(
                id,
                linkedMapOf(
                    "name" to displayName(id),
                    "type" to folderType(id),
                    "messageServerIds" to emptyList<String>(),
                ),
            )
        }

        // Reorder: special folders first, then others alphabetically
        val orderedTopLevelMap = linkedMapOf<String, Map<String, Any?>>()
        SPECIAL_FOLDERS.forEach { id -> topLevelFolderMap[id]?.let { orderedTopLevelMap[id] = it } }
        topLevelFolderMap.keys.filter { it !in SPECIAL_FOLDERS }.sorted().forEach { id ->
            orderedTopLevelMap[id] = topLevelFolderMap[id]!!
        }

        val contentsFile = outputFile.get().asFile
        contentsFile.parentFile?.mkdirs()

        val json = JsonOutput.prettyPrint(JsonOutput.toJson(orderedTopLevelMap))
        contentsFile.writeText("$json\n")
        println("Wrote \"${contentsFile.toPath()}\" with ${orderedTopLevelMap.size} top-level folder(s)")
    }

    private fun buildFolderNode(folder: File): Pair<Boolean, Map<String, Any?>> {
        val files = folder.listFiles() ?: emptyArray()
        val messageIds = files.filter { it.isFile && it.name.endsWith(".eml") }
            .map { it.name.removeSuffix(".eml") }
            .sorted()

        val subFolderNodes = files.filter { it.isDirectory }
            .sortedBy { it.name }
            .mapNotNull { subFolder ->
                buildFolderNode(subFolder).takeIf { it.first }?.let { subFolder.name to it.second }
            }
            .toMap(LinkedHashMap())

        val shouldInclude = messageIds.isNotEmpty() || subFolderNodes.isNotEmpty() || isSpecialFolder(folder.name)

        val node = linkedMapOf<String, Any?>(
            "name" to displayName(folder.name),
            "type" to folderType(folder.name),
            "messageServerIds" to messageIds,
        )
        if (subFolderNodes.isNotEmpty()) {
            node["subFolders"] = subFolderNodes
        }
        return shouldInclude to node
    }

    private fun isSpecialFolder(name: String) = SPECIAL_FOLDERS.contains(name.lowercase())

    private fun folderType(name: String) = when (name.lowercase()) {
        "inbox" -> "INBOX"
        "drafts" -> "DRAFTS"
        "sent" -> "SENT"
        "spam" -> "SPAM"
        "trash" -> "TRASH"
        "archive" -> "ARCHIVE"
        else -> "REGULAR"
    }

    private fun displayName(name: String): String {
        return name.replace('-', ' ')
            .replace('_', ' ')
            .split(' ')
            .filter { it.isNotBlank() }
            .joinToString(" ") { it.lowercase().replaceFirstChar { c -> c.titlecase() } }
    }

    companion object {
        private val SPECIAL_FOLDERS = listOf("inbox", "drafts", "sent", "spam", "trash", "archive")
    }
}
