package net.thunderbird.core.logging.file

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import java.io.File
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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

    private lateinit var dateTimeFormatted: String
    private val initialTimestamp = 1234567890L
    private lateinit var logFile: File
    private lateinit var fileLocation: String
    private lateinit var fileManager: FakeFileSystemManager
    private lateinit var testSubject: AndroidFileLogSink

    @Before
    fun setUp() {
        val instant = Instant.fromEpochMilliseconds(initialTimestamp)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        dateTimeFormatted = LocalDateTime.Formats.ISO.format(dateTime)
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

    @Test
    fun shouldHaveCorrectLogLevel() {
        assertThat(testSubject.level).isEqualTo(LogLevel.INFO)
    }

    @Test
    fun shouldLogMessageToFile() = runTest {
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

        // Arrange
        val logFile = File(fileLocation, "test_log.txt")
        assertThat(logFile.exists()).isEqualTo(true)
        assertThat(logFile.readText())
            .isEqualTo("$dateTimeFormatted priority = INFO, Test log message\n")
    }

//    @Test
//    fun shouldLogMultipleMessagesToFile() {
//        // Arrange
//        val message = "Test log message"
//        for (num in 0..5) {
//            val event = LogEvent(
//                timestamp = initialTimestamp + num,
//                level = LogLevel.INFO,
//                tag = "TestTag",
//                message = message + num,
//                throwable = null,
//            )
//            testSubject.log(event)
//        }
//
//        // Arrange
//        val logFile = File(fileLocation, "test_log.txt")
//        assertThat(logFile.exists()).isEqualTo(true)
//        assertThat(logFile.readText())
//            .isEqualTo("1970-01-15T01:56:07.89 priority = INFO, Test log message")
//    }

    @Test
    fun shouldExportLogFile() = runTest {
        // Arrange
        val event = LogEvent(
            timestamp = initialTimestamp,
            level = LogLevel.INFO,
            tag = "TestTag",
            message = "Test log message for export",
            throwable = null,
        )
        testSubject.log(event)

        // Act
        val exportUri = "content://test/export.txt"
        testSubject.export(exportUri)

        // Arrange
        val exportedContent = fileManager.exportedContent
        assertThat(exportedContent).isNotNull()
        assertThat(exportedContent!!)
            .isEqualTo("$dateTimeFormatted priority = INFO, Test log message for export\n")
    }

//    @Test
//    fun shouldClearBufferAndExportToFile() = runTest {
//        // Arrange
//        val message = "Test log message"
//        for (num in 0..5) {
//            val event = LogEvent(
//                timestamp = initialTimestamp + num,
//                level = LogLevel.INFO,
//                tag = "TestTag",
//                message = message + num,
//                throwable = null,
//            )
//            testSubject.log(event)
//        }
//
//        val fiveLogString = "1970-01-15T01:56:07.89 priority = INFO, Test log message\n" +
//            "1970-01-15T01:56:07.891 priority = INFO, Test log message1\n" +
//            "1970-01-15T01:56:07.892 priority = INFO, Test log message2\n" +
//            "1970-01-15T01:56:07.893 priority = INFO, Test log message3\n" +
//            "1970-01-15T01:56:07.894 priority = INFO, Test log message4\n"
//
//        // Arrange
//        val logFile = File(fileLocation, "test_log.txt")
//        assertThat(logFile.exists()).isEqualTo(true)
//        assertThat(logFile.readText())
//            .isEqualTo(fiveLogString)
//
//        val exportUri = "content://test/export.txt"
//        testSubject.export(exportUri)
//
//        // Arrange
//        val exportedContent = fileManager.exportedContent
//        assertThat(exportedContent).isNotNull()
//        assertThat(exportedContent!!)
//            .isEqualTo(fiveLogString)
//    }
}
