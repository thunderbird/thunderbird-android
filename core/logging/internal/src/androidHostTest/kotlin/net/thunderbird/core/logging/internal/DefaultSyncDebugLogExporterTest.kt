package net.thunderbird.core.logging.internal

import assertk.assertThat
import assertk.assertions.isEqualTo
import com.eygraber.uri.toKmpUri
import io.github.vinceglb.filekit.PlatformFile
import java.io.File
import kotlinx.coroutines.runBlocking
import net.thunderbird.components.core.logging.LogLevel
import net.thunderbird.components.core.logging.file.FileLogSink
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DefaultSyncDebugLogExporterTest {

    @JvmField
    @Rule
    val folder = TemporaryFolder()

    private val logFile by lazy { File(folder.root, "sync-debug.txt") }
    private val destination by lazy { File(folder.root, "export.txt") }

    @Test
    fun `export should copy and clear the sync log`() = runBlocking {
        logFile.writeText("sync log\n")

        createTestSubject().export(EXPORT_URI)

        assertThat(destination.readText()).isEqualTo("sync log\n")
        assertThat(logFile.readText()).isEqualTo("")
    }

    @Test
    fun `export should create an empty export when nothing was logged`() = runBlocking {
        createTestSubject().export(EXPORT_URI)

        assertThat(destination.readText()).isEqualTo("")
    }

    private fun createTestSubject() = DefaultSyncDebugLogExporter(
        fileLogSink = FileLogSink(level = LogLevel.DEBUG, file = PlatformFile(logFile)),
        logFile = PlatformFile(logFile),
        destinationFile = { PlatformFile(destination) },
    )

    private companion object {
        val EXPORT_URI = "content://test/export.txt".toKmpUri()
    }
}
