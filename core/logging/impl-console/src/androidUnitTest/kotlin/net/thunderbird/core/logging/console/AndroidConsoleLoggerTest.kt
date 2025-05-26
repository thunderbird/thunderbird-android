package net.thunderbird.core.logging.console

import android.util.Log
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import kotlin.test.Test
import net.thunderbird.core.logging.LogEvent
import net.thunderbird.core.logging.LogLevel
import org.robolectric.annotation.Config
import timber.log.Timber

class AndroidConsoleLoggerTest {

    @Test
    fun shouldHaveCorrectLogLevel() {
        // Arrange
        val testSubject = AndroidConsoleLogSink(LogLevel.INFO)

        // Act & Assert
        assertThat(testSubject.level).isEqualTo(LogLevel.INFO)
    }

    @Test
    fun shouldLogMessages() {
        // Arrange
        val testTree = TestTree()
        Timber.plant(testTree)
        val eventVerbose = LogEvent(
            level = LogLevel.VERBOSE,
            tag = "TestTag",
            message = "This is a verbose message",
            throwable = null,
            timestamp = 0L,
        )
        val eventDebug = LogEvent(
            level = LogLevel.DEBUG,
            tag = "TestTag",
            message = "This is a debug message",
            throwable = null,
            timestamp = 0L,
        )
        val eventInfo = LogEvent(
            level = LogLevel.INFO,
            tag = "TestTag",
            message = "This is a info message",
            throwable = null,
            timestamp = 0L,
        )
        val eventWarn = LogEvent(
            level = LogLevel.WARN,
            tag = "TestTag",
            message = "This is a warning message",
            throwable = null,
            timestamp = 0L,
        )
        val eventError = LogEvent(
            level = LogLevel.ERROR,
            tag = "TestTag",
            message = "This is an error message",
            throwable = null,
            timestamp = 0L,
        )

        val testSubject = AndroidConsoleLogSink(LogLevel.VERBOSE)

        // Act
        testSubject.log(eventVerbose)
        testSubject.log(eventDebug)
        testSubject.log(eventInfo)
        testSubject.log(eventWarn)
        testSubject.log(eventError)

        // Assert
        assertThat(testTree.events).hasSize(5)
        assertThat(testTree.events[0]).isEqualTo(eventVerbose)
        assertThat(testTree.events[1]).isEqualTo(eventDebug)
        assertThat(testTree.events[2]).isEqualTo(eventInfo)
        assertThat(testTree.events[3]).isEqualTo(eventWarn)
        assertThat(testTree.events[4]).isEqualTo(eventError)
    }

    @Test
    fun shouldExtractTagFromStackTraceWhenNoTagProvided() {
        // Arrange
        val testTree = TestTree()
        Timber.plant(testTree)
        val eventWithoutTag = LogEvent(
            level = LogLevel.INFO,
            tag = null, // No tag provided
            message = "This is a message without a tag",
            throwable = null,
            timestamp = 0L,
        )

        val testSubject = AndroidConsoleLogSink(LogLevel.VERBOSE)

        // Act
        testSubject.log(eventWithoutTag)

        // Assert
        assertThat(testTree.events).hasSize(1)
        // The tag should have been extracted from the stack trace
        assertThat(testTree.events[0].tag).isNotNull()
        // The tag should be the class name of the caller (AndroidConsoleLoggerTest)
        // Note: The tag is truncated to 23 characters on older Android versions
        assertThat(testTree.events[0].tag).isEqualTo("AndroidConsoleLoggerTes")
    }

    @Config(sdk = [25])
    @Test
    fun shouldTruncateLongTagsToMaxLength() {
        // Arrange
        val testTree = TestTree()
        Timber.plant(testTree)
        val longTag = "ThisIsAVeryLongTagThatExceedsTheMaximumLength"
        val expectedTruncatedTag = "ThisIsAVeryLongTagThatE" // 23 characters
        val eventWithLongTag = LogEvent(
            level = LogLevel.INFO,
            tag = longTag,
            message = "This is a message with a long tag",
            throwable = null,
            timestamp = 0L,
        )

        val testSubject = AndroidConsoleLogSink(LogLevel.VERBOSE)

        // Act
        testSubject.log(eventWithLongTag)

        // Assert
        assertThat(testTree.events).hasSize(1)

        // Debug: Print the actual tag and its length
        val actualTag = testTree.events[0].tag
        println("[DEBUG_LOG] Actual tag: '$actualTag', length: ${actualTag?.length}")
        println("[DEBUG_LOG] Expected tag: '$expectedTruncatedTag', length: ${expectedTruncatedTag.length}")

        // The tag should always be truncated to 23 characters for consistency
        assertThat(actualTag).isEqualTo(expectedTruncatedTag)
    }

    @Config(sdk = [26])
    fun shouldNotTruncateTagsOnNewAndroidVersions() {
        // Arrange
        val testTree = TestTree()
        Timber.plant(testTree)
        val longTag = "ThisIsAVeryLongTagThatExceedsTheMaximumLength"
        val expectedTag = longTag // No truncation on API 26+
        val eventWithLongTag = LogEvent(
            level = LogLevel.INFO,
            tag = longTag,
            message = "This is a message with a long tag",
            throwable = null,
            timestamp = 0L,
        )

        val testSubject = AndroidConsoleLogSink(LogLevel.VERBOSE)

        // Act
        testSubject.log(eventWithLongTag)

        // Assert
        assertThat(testTree.events).hasSize(1)
        assertThat(testTree.events[0].tag).isEqualTo(expectedTag)
    }

    class TestTree : Timber.DebugTree() {

        val events = mutableListOf<LogEvent>()

        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            events.add(LogEvent(mapPriorityToLogLevel(priority), tag, message, t, 0L))
        }

        private fun mapPriorityToLogLevel(priority: Int): LogLevel {
            return when (priority) {
                Log.VERBOSE -> LogLevel.VERBOSE
                Log.DEBUG -> LogLevel.DEBUG
                Log.INFO -> LogLevel.INFO
                Log.WARN -> LogLevel.WARN
                Log.ERROR -> LogLevel.ERROR
                else -> throw IllegalArgumentException("Unknown log priority: $priority")
            }
        }
    }
}
