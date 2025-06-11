import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import assertk.assertThat
import assertk.assertions.isEqualTo
import java.io.OutputStream
import kotlin.test.Test
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.file.FileLogSink
import net.thunderbird.core.logging.file.PlatformConfig
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class AndroidFileLogSinkTest {

    @JvmField
    @Rule
    val folder = TemporaryFolder()

    private val contentUri = mock<Uri>()
    private val outputStream = mock<OutputStream>()

    @Test
    fun shouldHaveCorrectLogLevel() {
        val mockContext = mock<Context> {
            on { filesDir } doReturn folder.newFolder()
        }
        // Arrange
        val testSubject = FileLogSink(
            LogLevel.INFO,
            fileName = "fileName",
            fileLocation = mockContext.filesDir.toString(),
            configuration = PlatformConfig(contentResolver = createContentResolver()),
        )

        // Act & Assert
        assertThat(testSubject.level).isEqualTo(LogLevel.INFO)
    }

    /**
     * Need integration test for log and output instead of unit
     */

    private fun createContentResolver(): ContentResolver {
        return mock {
            on { openOutputStream(contentUri, "wt") } doReturn outputStream
        }
    }
}
