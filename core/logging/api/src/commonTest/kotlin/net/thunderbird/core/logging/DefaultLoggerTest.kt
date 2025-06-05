package net.thunderbird.core.logging

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.datetime.Instant
import net.thunderbird.core.testing.TestClock

class DefaultLoggerTest {

    @Test
    fun `log should add all event to the sink`() {
        // Arrange
        val sink = FakeLogSink(LogLevel.VERBOSE)
        val exceptionVerbose = Exception("Verbose exception")
        val exceptionDebug = Exception("Debug exception")
        val exceptionInfo = Exception("Info exception")
        val exceptionWarn = Exception("Warn exception")
        val exceptionError = Exception("Error exception")

        val clock = TestClock(
            currentTime = Instant.fromEpochMilliseconds(0L),
        )
        val testSubject = DefaultLogger(
            sink = sink,
            clock = clock,
        )

        // Act
        testSubject.verbose(
            tag = "Verbose tag",
            throwable = exceptionVerbose,
            message = { "Verbose message" },
        )

        clock.advanceTimeBy(1000.milliseconds)
        testSubject.debug(
            tag = "Debug tag",
            throwable = exceptionDebug,
            message = { "Debug message" },
        )

        clock.advanceTimeBy(1000.milliseconds)
        testSubject.info(
            tag = "Info tag",
            throwable = exceptionInfo,
            message = { "Info message" },
        )

        clock.advanceTimeBy(1000.milliseconds)
        testSubject.warn(
            tag = "Warn tag",
            throwable = exceptionWarn,
            message = { "Warn message" },
        )

        clock.advanceTimeBy(1000.milliseconds)
        testSubject.error(
            tag = "Error tag",
            throwable = exceptionError,
            message = { "Error message" },
        )

        // Assert
        val events = sink.events
        assertThat(events).hasSize(5)
        assertThat(events[0]).isEqualTo(
            LogEvent(
                level = LogLevel.VERBOSE,
                tag = "Verbose tag",
                message = "Verbose message",
                throwable = exceptionVerbose,
                timestamp = 0,
            ),
        )
        assertThat(events[1]).isEqualTo(
            LogEvent(
                level = LogLevel.DEBUG,
                tag = "Debug tag",
                message = "Debug message",
                throwable = exceptionDebug,
                timestamp = 1000,
            ),
        )
        assertThat(events[2]).isEqualTo(
            LogEvent(
                level = LogLevel.INFO,
                tag = "Info tag",
                message = "Info message",
                throwable = exceptionInfo,
                timestamp = 2000,
            ),
        )
        assertThat(events[3]).isEqualTo(
            LogEvent(
                level = LogLevel.WARN,
                tag = "Warn tag",
                message = "Warn message",
                throwable = exceptionWarn,
                timestamp = 3000,
            ),
        )
        assertThat(events[4]).isEqualTo(
            LogEvent(
                level = LogLevel.ERROR,
                tag = "Error tag",
                message = "Error message",
                throwable = exceptionError,
                timestamp = 4000,
            ),
        )
    }

    @Test
    fun `log should not add event to the sink if the level is not allowed for the sink`() {
        // Arrange
        val sink = FakeLogSink(LogLevel.INFO)
        val exceptionVerbose = Exception("Verbose exception")
        val exceptionDebug = Exception("Debug exception")
        val exceptionInfo = Exception("Info exception")

        val clock = TestClock(
            currentTime = Instant.fromEpochMilliseconds(0L),
        )
        val testSubject = DefaultLogger(
            sink = sink,
            clock = clock,
        )

        // Act
        testSubject.verbose(
            tag = "Verbose tag",
            throwable = exceptionVerbose,
            message = { "Verbose message" },
        )

        clock.advanceTimeBy(1000.milliseconds)
        testSubject.debug(
            tag = "Debug tag",
            throwable = exceptionDebug,
            message = { "Debug message" },
        )

        clock.advanceTimeBy(1000.milliseconds)
        testSubject.info(
            tag = "Info tag",
            throwable = exceptionInfo,
            message = { "Info message" },
        )

        // Assert
        assertThat(sink.events).hasSize(1)
        assertThat(sink.events[0]).isEqualTo(
            LogEvent(
                level = LogLevel.INFO,
                tag = "Info tag",
                message = "Info message",
                throwable = exceptionInfo,
                timestamp = 2000,
            ),
        )
    }
}
