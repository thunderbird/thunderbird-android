package net.thunderbird.core.file

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.eygraber.uri.Uri
import java.io.File
import kotlin.test.fail
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
        val uri = Uri.parse(tempFile.toURI().toString())
        val sink = checkNotNull(testSubject.openSink(uri))

        // Act
        val writeBuffer = Buffer().apply { write(testText.encodeToByteArray()) }
        sink.write(writeBuffer, writeBuffer.size)
        sink.flush()
        sink.close()

        val source = checkNotNull(testSubject.openSource(uri))
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

    @Test
    fun `openSink with Append should append to existing content`() {
        // Arrange
        val tempFile: File = folder.newFile("tb-file-fs-append.txt")
        val uri = Uri.parse(tempFile.toURI().toString())
        val initial = "Hello"
        val extra = " World"

        // Write initial content (truncate by default)
        run {
            val sink = checkNotNull(testSubject.openSink(uri))
            val buf = Buffer().apply { write(initial.encodeToByteArray()) }
            sink.write(buf, buf.size)
            sink.flush()
            sink.close()
        }

        // Append extra content
        run {
            val sink = checkNotNull(testSubject.openSink(uri, WriteMode.Append))
            val buf = Buffer().apply { write(extra.encodeToByteArray()) }
            sink.write(buf, buf.size)
            sink.flush()
            sink.close()
        }

        // Read back
        val source = checkNotNull(testSubject.openSource(uri))
        val readBuffer = Buffer()
        source.readAtMostTo(readBuffer, 1024)
        val bytes = ByteArray(readBuffer.size.toInt())
        repeat(bytes.size) { i -> bytes[i] = readBuffer.readByte() }
        val result = bytes.decodeToString()
        source.close()

        // Assert
        assertThat(result).isEqualTo(initial + extra)
    }

    @Test
    fun `openSink with Truncate should overwrite existing content`() {
        // Arrange
        val tempFile: File = folder.newFile("tb-file-fs-truncate.txt")
        val uri = Uri.parse(tempFile.toURI().toString())
        val first = "First"
        val second = "Second"

        // Write first content
        run {
            val sink = checkNotNull(testSubject.openSink(uri, WriteMode.Truncate))
            val buf = Buffer().apply { write(first.encodeToByteArray()) }
            sink.write(buf, buf.size)
            sink.flush()
            sink.close()
        }

        // Overwrite with second content
        run {
            val sink = checkNotNull(testSubject.openSink(uri, WriteMode.Truncate))
            val buf = Buffer().apply { write(second.encodeToByteArray()) }
            sink.write(buf, buf.size)
            sink.flush()
            sink.close()
        }

        // Read back
        val source = checkNotNull(testSubject.openSource(uri))
        val readBuffer = Buffer()
        source.readAtMostTo(readBuffer, 1024)
        val bytes = ByteArray(readBuffer.size.toInt())
        repeat(bytes.size) { i -> bytes[i] = readBuffer.readByte() }
        val result = bytes.decodeToString()
        source.close()

        // Assert
        assertThat(result).isEqualTo(second)
    }

    @Test
    fun `delete on existing file should succeed and remove file`() {
        // Arrange
        val tempFile: File = folder.newFile("tb-file-fs-test-jvm-delete.txt")
        val uri = Uri.parse(tempFile.toURI().toString())
        // Ensure file has some content
        run {
            val sink = checkNotNull(testSubject.openSink(uri))
            val buf = Buffer().apply { write("x".encodeToByteArray()) }
            sink.write(buf, buf.size)
            sink.flush()
            sink.close()
        }

        // Act
        testSubject.delete(uri)

        // Assert: file should no longer exist on disk
        assertThat(tempFile.exists()).isEqualTo(false)
    }

    @Test
    fun `delete non existing file should not throw`() {
        // Arrange
        val tempFile: File = folder.newFile("tb-file-fs-test-jvm-delete-missing.txt")
        val uri = Uri.parse(tempFile.toURI().toString())
        // Ensure the file is missing
        check(tempFile.delete())

        // Act + Assert: should not throw even if file is missing
        try {
            testSubject.delete(uri)
        } catch (e: Exception) {
            fail("Deletion of non-existing file threw an exception: $e")
        }
    }
}
