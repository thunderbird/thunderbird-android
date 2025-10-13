package net.thunderbird.core.file

import assertk.assertThat
import assertk.assertions.isEqualTo
import java.io.File
import kotlinx.io.Buffer
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class JvmFileSystemManagerTest {

    private val testSubject = JvmFileSystemManager()

    @JvmField
    @Rule
    val folder = TemporaryFolder()

    @Test
    fun `openSink and openSource should write and read file content roundtrip`() {
        // Arrange
        val tempFile: File = folder.newFile("tb-file-fs-test.txt")
        val testText = "Hello Thunderbird!"
        val sink = checkNotNull(testSubject.openSink(tempFile.absolutePath, "wt"))

        // Act
        val writeBuffer = Buffer().apply { write(testText.encodeToByteArray()) }
        sink.write(writeBuffer, writeBuffer.size)
        sink.flush()
        sink.close()

        val source = checkNotNull(testSubject.openSource(tempFile.absolutePath))
        val readBuffer = Buffer()
        source.readAtMostTo(readBuffer, 1024)
        val bytes = ByteArray(readBuffer.size.toInt())
        for (i in bytes.indices) {
            bytes[i] = readBuffer.readByte()
        }
        val result = bytes.decodeToString()
        source.close()

        // Assert
        assertThat(result).isEqualTo(testText)
    }
}
