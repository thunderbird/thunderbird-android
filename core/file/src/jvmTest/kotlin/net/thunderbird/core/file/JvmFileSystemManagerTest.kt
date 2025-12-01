package net.thunderbird.core.file

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.eygraber.uri.Uri
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
}
