package com.fsck.k9.helper

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEqualTo
import assertk.assertions.isTrue
import java.io.File
import kotlin.test.Test
import org.junit.Rule
import org.junit.rules.TemporaryFolder

class FileHelperTest {

    @Rule
    @JvmField
    val tempFolder = TemporaryFolder()

    @Test
    fun `touchFile should create file`() {
        val parent = tempFolder.newFolder("parent")

        FileHelper.touchFile(parent, "FileName")

        assertThat(File(parent, "FileName").exists()).isTrue()
    }

    @Test
    fun `touchFile should change file modification time`() {
        val parent = tempFolder.newFolder("parent")
        val lastModified = 12345L
        val file = File(parent, "FileName")
        file.createNewFile()
        file.setLastModified(lastModified)

        FileHelper.touchFile(parent, "FileName")

        assertThat(file.lastModified()).isNotEqualTo(lastModified)
    }

    @Test
    fun `renameOrMoveByCopying should move file when destination does not exist`() {
        val source = tempFolder.newFile("source")
        val destination = File(tempFolder.root, "destination")
        val sourceContent = "content"
        source.writeText(sourceContent)

        FileHelper.renameOrMoveByCopying(source, destination)

        assertThat(source.exists()).isFalse()
        assertThat(destination.exists()).isTrue()
        assertThat(destination.readText()).isEqualTo(sourceContent)
    }

    @Test
    fun `renameOrMoveByCopying should move file when destination exists`() {
        val source = tempFolder.newFile("source")
        val destination = tempFolder.newFile("destination")
        val sourceContent = "content"
        source.writeText(sourceContent)

        FileHelper.renameOrMoveByCopying(source, destination)

        assertThat(source.exists()).isFalse()
        assertThat(destination.exists()).isTrue()
        assertThat(destination.readText()).isEqualTo(sourceContent)
    }
}
