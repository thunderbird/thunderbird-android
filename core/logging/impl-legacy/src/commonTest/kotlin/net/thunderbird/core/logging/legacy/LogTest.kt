package net.thunderbird.core.logging.legacy

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import kotlin.test.Test
import net.thunderbird.core.logging.LogEvent
import net.thunderbird.core.logging.LogLevel
import net.thunderbird.core.logging.testing.TestLogger
import net.thunderbird.core.logging.testing.TestLogger.Companion.TIMESTAMP

class LogTest {

    @Test
    fun `init should set logger`() {
        // Arrange
        val logger = TestLogger()

        // Act
        Log.logger = logger
        Log.info(
            tag = "Test tag",
            message = { "Test message" },
        )

        // Assert
        assertThat(logger.events).hasSize(1)
        assertThat(logger.events[0]).isEqualTo(
            LogEvent(
                level = LogLevel.INFO,
                tag = "Test tag",
                message = "Test message",
                throwable = null,
                timestamp = TIMESTAMP,
            ),
        )
    }

    @Test
    fun `log should add all event to the logger`() {
        // Arrange
        val logger = TestLogger()
        val exceptionVerbose = Exception("Verbose exception")
        val exceptionDebug = Exception("Debug exception")
        val exceptionInfo = Exception("Info exception")
        val exceptionWarn = Exception("Warn exception")
        val exceptionError = Exception("Error exception")

        Log.logger = logger

        // Act
        Log.verbose(
            tag = "Verbose tag",
            throwable = exceptionVerbose,
            message = { "Verbose message" },
        )
        Log.debug(
            tag = "Debug tag",
            throwable = exceptionDebug,
            message = { "Debug message" },
        )
        Log.info(
            tag = "Info tag",
            throwable = exceptionInfo,
            message = { "Info message" },
        )
        Log.warn(
            tag = "Warn tag",
            throwable = exceptionWarn,
            message = { "Warn message" },
        )
        Log.error(
            tag = "Error tag",
            throwable = exceptionError,
            message = { "Error message" },
        )

        // Assert
        val events = logger.events
        assertThat(events).hasSize(5)
        assertThat(events[0]).isEqualTo(
            LogEvent(
                level = LogLevel.VERBOSE,
                tag = "Verbose tag",
                message = "Verbose message",
                throwable = exceptionVerbose,
                timestamp = TIMESTAMP,
            ),
        )
        assertThat(events[1]).isEqualTo(
            LogEvent(
                level = LogLevel.DEBUG,
                tag = "Debug tag",
                message = "Debug message",
                throwable = exceptionDebug,
                timestamp = TIMESTAMP,
            ),
        )
        assertThat(events[2]).isEqualTo(
            LogEvent(
                level = LogLevel.INFO,
                tag = "Info tag",
                message = "Info message",
                throwable = exceptionInfo,
                timestamp = TIMESTAMP,
            ),
        )
        assertThat(events[3]).isEqualTo(
            LogEvent(
                level = LogLevel.WARN,
                tag = "Warn tag",
                message = "Warn message",
                throwable = exceptionWarn,
                timestamp = TIMESTAMP,
            ),
        )
        assertThat(events[4]).isEqualTo(
            LogEvent(
                level = LogLevel.ERROR,
                tag = "Error tag",
                message = "Error message",
                throwable = exceptionError,
                timestamp = TIMESTAMP,
            ),
        )
    }
}
