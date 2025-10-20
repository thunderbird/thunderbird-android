package net.thunderbird.core.file

import android.content.Context
import android.net.Uri
import assertk.assertThat
import assertk.assertions.isEqualTo
import com.eygraber.uri.toKmpUri
import kotlinx.io.Buffer
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AndroidFileSystemManagerTest {

    private val appContext: Context = RuntimeEnvironment.getApplication()

    private val testSubject = AndroidFileSystemManager(appContext.contentResolver)

    @JvmField
    @Rule
    val folder = TemporaryFolder()

    @Test
    fun openSinkAndOpenSource_writeAndReadFileContentRoundtrip() {
        // Arrange
        val tempFile = folder.newFile("tb-file-fs-test-android.txt")
        val uri: Uri = Uri.fromFile(tempFile)
        val testText = "Hello Thunderbird Android!"

        // Act
        val sink = checkNotNull(testSubject.openSink(uri.toKmpUri()))
        val writeBuffer = Buffer().apply { write(testText.encodeToByteArray()) }
        sink.write(writeBuffer, writeBuffer.size)
        sink.flush()
        sink.close()

        val source = checkNotNull(testSubject.openSource(uri.toKmpUri()))
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
    fun openSink_withAppend_shouldAppendToExistingContent() {
        // Arrange
        val tempFile = folder.newFile("tb-file-fs-test-android-append.txt")
        val uri: Uri = Uri.fromFile(tempFile)
        val initial = "Hello"
        val extra = " Android"

        // Write initial content (truncate by default)
        run {
            val sink = checkNotNull(testSubject.openSink(uri.toKmpUri()))
            val buf = Buffer().apply { write(initial.encodeToByteArray()) }
            sink.write(buf, buf.size)
            sink.flush()
            sink.close()
        }

        // Append extra content
        run {
            val sink = checkNotNull(testSubject.openSink(uri.toKmpUri(), WriteMode.Append))
            val buf = Buffer().apply { write(extra.encodeToByteArray()) }
            sink.write(buf, buf.size)
            sink.flush()
            sink.close()
        }

        // Read back
        val source = checkNotNull(testSubject.openSource(uri.toKmpUri()))
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
    fun openSink_withTruncate_shouldOverwriteExistingContent() {
        // Arrange
        val tempFile = folder.newFile("tb-file-fs-test-android-truncate.txt")
        val uri: Uri = Uri.fromFile(tempFile)
        val first = "First"
        val second = "Second"

        // Write first content
        run {
            val sink = checkNotNull(testSubject.openSink(uri.toKmpUri(), WriteMode.Truncate))
            val buf = Buffer().apply { write(first.encodeToByteArray()) }
            sink.write(buf, buf.size)
            sink.flush()
            sink.close()
        }

        // Overwrite with second content
        run {
            val sink = checkNotNull(testSubject.openSink(uri.toKmpUri(), WriteMode.Truncate))
            val buf = Buffer().apply { write(second.encodeToByteArray()) }
            sink.write(buf, buf.size)
            sink.flush()
            sink.close()
        }

        // Read back
        val source = checkNotNull(testSubject.openSource(uri.toKmpUri()))
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
