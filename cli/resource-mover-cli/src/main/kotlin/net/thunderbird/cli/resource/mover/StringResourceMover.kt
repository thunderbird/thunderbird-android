package net.thunderbird.cli.resource.mover

import java.io.File
import kotlin.system.exitProcess

@Suppress("TooManyFunctions")
class StringResourceMover {

    fun moveKeys(source: String, target: String, keys: List<String>) {
        fun File.isComposeResource(): Boolean = File(this, COMPOSE_RESOURCE_PATH).exists()

        val sourceDir = File(source)
        val sourceBaseResourcePath = if (sourceDir.isComposeResource()) COMPOSE_RESOURCE_PATH else RESOURCE_PATH
        val sourcePath = File(source + sourceBaseResourcePath)

        val targetDir = File(target)
        val isTargetComposeResources = targetDir.isComposeResource()
        val targetBaseResourcePath = if (isTargetComposeResources) COMPOSE_RESOURCE_PATH else RESOURCE_PATH
        val targetPath = File(target + targetBaseResourcePath)

        if (!sourcePath.exists()) {
            println("\nSource path does not exist: $sourcePath\n")
            return
        }

        println("\nMoving keys $keys")
        println("       from \"$sourcePath\" -> \"$targetPath\"\n")
        for (key in keys) {
            moveKey(sourcePath, targetPath, key, isTargetComposeResources)
        }
    }

    private fun moveKey(sourcePath: File, targetPath: File, key: String, isTargetComposeResources: Boolean) {
        println("\nMoving key: $key\n")

        sourcePath.walk()
            .filter { it.name.startsWith(VALUES_PATH) }
            .forEach { sourceDir ->
                val sourceFile = sourceDir.resolve(STRING_RESOURCE_FILE_NAME)
                if (sourceFile.exists()) {
                    moveKeyDeclaration(sourceFile, targetPath, key, isTargetComposeResources)
                }
            }
    }

    private fun moveKeyDeclaration(sourceFile: File, targetPath: File, key: String, isTargetComposeResources: Boolean) {
        if (containsKey(sourceFile, key)) {
            println("\nFound key in file: ${sourceFile.path}\n")

            val targetFile = getOrCreateTargetFile(targetPath, sourceFile, isTargetComposeResources)
            val keyDeclaration = extractKeyDeclaration(sourceFile, key)

            println("    Key declaration: $keyDeclaration")

            copyKeyToTarget(targetFile, keyDeclaration, key)
            deleteKeyFromSource(sourceFile, keyDeclaration)

            if (isSourceFileEmpty(sourceFile)) {
                println("    Source file is empty: ${sourceFile.path} -> deleting it.")
                sourceFile.delete()
            }
        }
    }

    private fun containsKey(sourceFile: File, key: String): Boolean {
        val keyPattern = createKeyPattern(key)
        val sourceContent = sourceFile.readText()
        return keyPattern.containsMatchIn(sourceContent)
    }

    private fun extractKeyDeclaration(sourceFile: File, key: String): String {
        val keyPattern = createKeyPattern(key)
        val declaration = StringBuilder()
        var isTagClosed = true

        sourceFile.forEachLine { line ->
            if (keyPattern.containsMatchIn(line)) {
                declaration.appendLine(line)
                isTagClosed = isTagClosed(line)
            } else if (!isTagClosed) {
                declaration.appendLine(line)
                isTagClosed = isTagClosed(line)
            }
        }

        return declaration.toString()
    }

    private fun createKeyPattern(key: String): Regex {
        return KEY_PATTERN.replace(KEY_PLACEHOLDER, Regex.escape(key)).toRegex()
    }

    private fun isTagClosed(line: String): Boolean {
        return line.contains(STRING_CLOSING_TAG) || line.contains(PLURALS_CLOSING_TAG)
    }

    private fun copyKeyToTarget(targetFile: File, keyDeclaration: String, key: String) {
        println("    Moving key to file: ${targetFile.path}")

        if (containsKey(targetFile, key)) {
            println("    Key already exists in target file: ${targetFile.path} replacing it.")
            replaceKeyInTarget(targetFile, keyDeclaration, key)
        } else {
            addKeyToTarget(targetFile, keyDeclaration)
        }
    }

    private fun addKeyToTarget(targetFile: File, keyDeclaration: String) {
        val targetContent = StringBuilder()

        targetFile.forEachLine { line ->
            if (line.contains(RESOURCE_CLOSING_TAG)) {
                targetContent.appendLine(keyDeclaration.trimEnd())
                targetContent.appendLine(line)
            } else {
                targetContent.appendLine(line)
            }
        }

        targetFile.writeText(targetContent.toString())
    }

    private fun replaceKeyInTarget(targetFile: File, keyDeclaration: String, key: String) {
        println("    Replacing key in file: ${targetFile.path}")

        val oldKeyDeclaration = extractKeyDeclaration(targetFile, key)
        val targetContent = targetFile.readText()

        targetFile.writeText(targetContent.replace(oldKeyDeclaration, keyDeclaration))
    }

    private fun deleteKeyFromSource(sourceFile: File, keyDeclaration: String) {
        println("    Deleting key from file: ${sourceFile.path}")

        val sourceContent = sourceFile.readText()

        sourceFile.writeText(sourceContent.replace(keyDeclaration, ""))
    }

    private fun isSourceFileEmpty(sourceFile: File): Boolean {
        val sourceContent = sourceFile.readText()
        return sourceContent.contains(STRING_CLOSING_TAG).not() && sourceContent.contains(PLURALS_CLOSING_TAG).not()
    }

    private fun getOrCreateTargetFile(targetPath: File, sourceFile: File, isTargetComposeResources: Boolean): File {
        val targetFilePath = targetPath.resolve(sourceFile.parentFile.name)
        val targetFile = File(targetFilePath, sourceFile.name)
        val targetDirectory = targetFile.parentFile

        if (!targetDirectory.exists()) {
            targetDirectory.mkdirs()
            println("    Target directory created: ${targetDirectory.path}")
        }

        if (!targetFile.exists()) {
            createTargetFile(targetFile, isTargetComposeResources)
        }

        return targetFile
    }

    private fun createTargetFile(targetFile: File, isTargetComposeResources: Boolean) {
        val isNewFileCreated: Boolean = targetFile.createNewFile()
        if (!isNewFileCreated) {
            printError("Target file could not be created: ${targetFile.path}")
            exitProcess(-1)
        }

        targetFile.writeText(
            if (isTargetComposeResources) TARGET_FILE_CONTENT_COMPOSE_RESOURCE else TARGET_FILE_CONTENT,
        )
        println("Target file ${targetFile.path} created")
    }

    private fun printError(message: String) {
        System.err.println("\n$message\n")
    }

    private companion object {
        const val RESOURCE_PATH = "/src/main/res/"
        const val COMPOSE_RESOURCE_PATH = "/src/commonMain/composeResources/"
        const val KEY_PLACEHOLDER = "{KEY}"
        const val KEY_PATTERN = """name="$KEY_PLACEHOLDER""""
        const val VALUES_PATH = "values"
        const val STRING_RESOURCE_FILE_NAME = "strings.xml"
        const val STRING_CLOSING_TAG = "</string>"
        const val PLURALS_CLOSING_TAG = "</plurals>"
        const val RESOURCE_CLOSING_TAG = "</resources>"

        val TARGET_FILE_CONTENT = """
            <?xml version="1.0" encoding="UTF-8"?>
            <resources xmlns:xliff="urn:oasis:names:tc:xliff:document:1.2">
            </resources>

        """.trimIndent()

        val TARGET_FILE_CONTENT_COMPOSE_RESOURCE = """
            <?xml version="1.0" encoding="UTF-8"?>
            <resources>
            </resources>

        """.trimIndent()
    }
}
