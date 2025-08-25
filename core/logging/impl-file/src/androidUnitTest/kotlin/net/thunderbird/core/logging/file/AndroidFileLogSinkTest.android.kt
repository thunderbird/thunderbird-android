package net.thunderbird.core.logging.file

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import java.io.File
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import net.thunderbird.core.logging.LogEvent
import net.thunderbird.core.logging.LogLevel
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidFileLogSinkTest {

    @JvmField
    @Rule
    val folder = TemporaryFolder()

    private val initialTimestamp = 1234567890L
    private lateinit var logFile: File
    private lateinit var fileLocation: String
    private lateinit var fileManager: FakeFileSystemManager
    private lateinit var testSubject: AndroidFileLogSink

    @Before
    fun setUp() {
        fileLocation = folder.newFolder().absolutePath
        logFile = File(fileLocation, "test_log.txt")
        fileManager = FakeFileSystemManager()
        testSubject = AndroidFileLogSink(
            level = LogLevel.INFO,
            fileName = "test_log",
            fileLocation = fileLocation,
            fileSystemManager = fileManager,
            coroutineContext = UnconfinedTestDispatcher(),
        )
    }
    fun timeSetup(timeStamp: Long): String {
        val instant = Instant.fromEpochMilliseconds(timeStamp)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        return LocalDateTime.Formats.ISO.format(dateTime)
    }

    @Test
    fun shouldHaveCorrectLogLevel() {
        assertThat(testSubject.level).isEqualTo(LogLevel.INFO)
    }

    @Test
    fun shouldLogMessageToFile() {
        // Arrange
        val message = "Test log message"
        val event = LogEvent(
            timestamp = initialTimestamp,
            level = LogLevel.INFO,
            tag = "TestTag",
            message = message,
            throwable = null,
        )

        // Act
        testSubject.log(event)
        runBlocking {
            testSubject.flushAndCloseBuffer()
        }

        // Arrange
        val logFile = File(fileLocation, "test_log.txt")
        assertThat(logFile.exists()).isEqualTo(true)
        assertThat(logFile.readText())
            .isEqualTo("${timeSetup(initialTimestamp)} priority = INFO, Test log message\n")
    }

    @Test
    fun shouldLogMultipleMessagesToFile() {
        // Arrange
        val message = "Test log message"
        var fiveLogString: String = ""
        for (num in 0..3) {
            val event = LogEvent(
                timestamp = initialTimestamp + num,
                level = LogLevel.INFO,
                tag = "TestTag",
                message = message + num,
                throwable = null,
            )
            testSubject.log(event)
            fiveLogString = fiveLogString + "${timeSetup(event.timestamp)} priority = INFO, ${event.message}\n"
        }

        val logFile = File(fileLocation, "test_log.txt")
        assertThat(logFile.exists()).isEqualTo(true)
        assertThat(logFile.readText())
            .isEqualTo("")

        val eventTippingBuffer = LogEvent(
            timestamp = initialTimestamp + 6,
            level = LogLevel.INFO,
            tag = "TestTag",
            message = message + "buffered",
            throwable = null,
        )
        fiveLogString =
            fiveLogString +
            "${timeSetup(eventTippingBuffer.timestamp)} priority = INFO, ${eventTippingBuffer.message}\n"
        testSubject.log(eventTippingBuffer)

        // Arrange
        assertThat(logFile.exists()).isEqualTo(true)
        assertThat(logFile.readText())
            .isEqualTo(fiveLogString)
    }

    @Test
    fun shouldExportLogFile() {
        // Arrange
        val event = LogEvent(
            timestamp = initialTimestamp,
            level = LogLevel.INFO,
            tag = "TestTag",
            message = "Test log message for export",
            throwable = null,
        )
        testSubject.log(event)
        runBlocking {
            testSubject.flushAndCloseBuffer()
        }

        // Act
        val exportUri = "content://test/export.txt"
        testSubject.export(exportUri)

        // Arrange
        val exportedContent = fileManager.exportedContent
        assertThat(exportedContent).isNotNull()
        assertThat(exportedContent!!)
            .isEqualTo("${timeSetup(initialTimestamp)} priority = INFO, Test log message for export\n")
    }

    @Test
    fun shouldClearBufferAndExportToFile() {
        // Arrange
        val message = "Test log message"
        var logString1: String = ""

        for (num in 0..4) {
            val event = LogEvent(
                timestamp = initialTimestamp + num,
                level = LogLevel.INFO,
                tag = "TestTag",
                message = message + num,
                throwable = null,
            )
            testSubject.log(event)
            logString1 = logString1 + "${timeSetup(event.timestamp)} priority = INFO, ${event.message}\n"
        }
        val event = LogEvent(
            timestamp = initialTimestamp + 5,
            level = LogLevel.INFO,
            tag = "TestTag",
            message = message + 5,
            throwable = null,
        )
        testSubject.log(event)

        var logString2: String = logString1 + "${timeSetup(event.timestamp)} priority = INFO, ${event.message}\n"

        // Arrange
        val logFile = File(fileLocation, "test_log.txt")
        assertThat(logFile.exists()).isEqualTo(true)
        assertThat(logFile.readText())
            .isEqualTo(logString1)

        val exportUri = "content://test/export.txt"
        testSubject.export(exportUri)

        // Arrange
        val exportedContent = fileManager.exportedContent
        assertThat(exportedContent).isNotNull()
        assertThat(exportedContent!!)
            .isEqualTo(logString2)
    }
}
