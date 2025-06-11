package net.thunderbird.core.logging.file

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import java.io.File
import kotlin.test.Test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
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

    @Test
    fun shouldHaveCorrectLogLevel() {
        assertThat(testSubject.level).isEqualTo(LogLevel.INFO)
    }

    @Test
    fun shouldLogMessageToFile() = runTest {
        // Arrange
        val message = "Test log message"
        val event = LogEvent(
            timestamp = 1234567890,
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
            .isEqualTo("1970-01-15T01:56:07.89 priority = INFO, Test log message\n")
    }

//    @Test
//    fun shouldLogMultipleMessagesToFile() {
//        // Arrange
//        val message = "Test log message"
//        val event = LogEvent(
//            timestamp = 1234567890,
//            level = LogLevel.INFO,
//            tag = "TestTag",
//            message = message,
//            throwable = null,
//        )
//        val event1 = LogEvent(
//            timestamp = 1234567891,
//            level = LogLevel.INFO,
//            tag = "TestTag",
//            message = message + "1",
//            throwable = null,
//        )
//        val event2 = LogEvent(
//            timestamp = 1234567892,
//            level = LogLevel.INFO,
//            tag = "TestTag",
//            message = message + "2",
//            throwable = null,
//        )
//        val event3 = LogEvent(
//            timestamp = 1234567893,
//            level = LogLevel.INFO,
//            tag = "TestTag",
//            message = message + "3",
//            throwable = null,
//        )
//        val event4 = LogEvent(
//            timestamp = 1234567894,
//            level = LogLevel.INFO,
//            tag = "TestTag",
//            message = message + "4",
//            throwable = null,
//        )
//
//        // Act
//        testSubject.log(event)
//        testSubject.log(event1)
//        testSubject.log(event2)
//        testSubject.log(event3)
//        testSubject.log(event4)
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
            timestamp = 1234567890,
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
            .isEqualTo("1970-01-15T01:56:07.89 priority = INFO, Test log message for export\n")
    }

//    @Test
//    fun shouldClearBufferAndExportToFile() = runTest {
//        // Arrange
//        val message = "Test log message"
//        val event = LogEvent(
//            timestamp = 1234567890,
//            level = LogLevel.INFO,
//            tag = "TestTag",
//            message = message,
//            throwable = null,
//        )
//        val event1 = LogEvent(
//            timestamp = 1234567891,
//            level = LogLevel.INFO,
//            tag = "TestTag",
//            message = message + "1",
//            throwable = null,
//        )
//        val event2 = LogEvent(
//            timestamp = 1234567892,
//            level = LogLevel.INFO,
//            tag = "TestTag",
//            message = message + "2",
//            throwable = null,
//        )
//        val event3 = LogEvent(
//            timestamp = 1234567893,
//            level = LogLevel.INFO,
//            tag = "TestTag",
//            message = message + "3",
//            throwable = null,
//        )
//        val event4 = LogEvent(
//            timestamp = 1234567894,
//            level = LogLevel.INFO,
//            tag = "TestTag",
//            message = message + "4",
//            throwable = null,
//        )
//
//        val fiveLogString = "1970-01-15T01:56:07.89 priority = INFO, Test log message\n" +
//            "1970-01-15T01:56:07.891 priority = INFO, Test log message1\n" +
//            "1970-01-15T01:56:07.892 priority = INFO, Test log message2\n" +
//            "1970-01-15T01:56:07.893 priority = INFO, Test log message3\n" +
//            "1970-01-15T01:56:07.894 priority = INFO, Test log message4\n"
//
//        // Act
//        testSubject.log(event)
//        testSubject.log(event1)
//        testSubject.log(event2)
//        testSubject.log(event3)
//        testSubject.log(event4)
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
