package net.thunderbird.core.file

import android.content.Context
import android.net.Uri
import assertk.assertThat
import assertk.assertions.isEqualTo
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
        val sink = checkNotNull(testSubject.openSink(uri.toString()))
        val writeBuffer = Buffer().apply { write(testText.encodeToByteArray()) }
        sink.write(writeBuffer, writeBuffer.size)
        sink.flush()
        sink.close()

        val source = checkNotNull(testSubject.openSource(uri.toString()))
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
