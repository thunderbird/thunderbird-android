package net.thunderbird.gradle.plugin.testing.rule

import java.io.File
import org.junit.rules.TemporaryFolder

/**
 * JUnit test rule that provides a temporary folder for test projects with file writing capabilities.
 *
 * Extends JUnit's TemporaryFolder to provide a convenient method for creating and writing files
 * in the temporary directory. Used in Gradle plugin tests to set up test project structures.
 */
internal class ProjectTempFolderRule : TemporaryFolder() {
    /**
     * Creates a new file with the given name in the temporary folder and writes the specified content to it.
     *
     * @param name The name of the file to create
     * @param content The text content to write to the file
     * @return The created File object with the written content
     */
    fun writeFile(name: String, content: String): File =
        newFile(name).apply { writeText(content) }
}
