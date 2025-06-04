package net.thunderbird.core.logging.legacy

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
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

    @Test
    fun `legacy methods should log correctly`() {
        // Arrange
        val logger = TestLogger()
        val exception = Exception("Test exception")
        Log.logger = logger

        // Act - Test all legacy method signatures for each log level

        // Verbose methods
        Log.v("Verbose message %s", "arg1")
        Log.v(exception, "Verbose message with exception %s", "arg1")
        Log.v(exception)

        // Debug methods
        Log.d("Debug message %s", "arg1")
        Log.d(exception, "Debug message with exception %s", "arg1")
        Log.d(exception)

        // Info methods
        Log.i("Info message %s", "arg1")
        Log.i(exception, "Info message with exception %s", "arg1")
        Log.i(exception)

        // Warn methods
        Log.w("Warn message %s", "arg1")
        Log.w(exception, "Warn message with exception %s", "arg1")
        Log.w(exception)

        // Error methods
        Log.e("Error message %s", "arg1")
        Log.e(exception, "Error message with exception %s", "arg1")
        Log.e(exception)

        // Assert
        val events = logger.events
        assertThat(events).hasSize(15)

        // Verify verbose events
        assertThat(events[0]).isEqualTo(
            LogEvent(
                level = LogLevel.VERBOSE,
                tag = null,
                message = "Verbose message arg1",
                throwable = null,
                timestamp = TIMESTAMP,
            ),
        )
        assertThat(events[1]).isEqualTo(
            LogEvent(
                level = LogLevel.VERBOSE,
                tag = null,
                message = "Verbose message with exception arg1",
                throwable = exception,
                timestamp = TIMESTAMP,
            ),
        )
        assertThat(events[2]).isEqualTo(
            LogEvent(
                level = LogLevel.VERBOSE,
                tag = null,
                message = "Test exception",
                throwable = exception,
                timestamp = TIMESTAMP,
            ),
        )

        // Verify debug events
        assertThat(events[3]).isEqualTo(
            LogEvent(
                level = LogLevel.DEBUG,
                tag = null,
                message = "Debug message arg1",
                throwable = null,
                timestamp = TIMESTAMP,
            ),
        )
        assertThat(events[4]).isEqualTo(
            LogEvent(
                level = LogLevel.DEBUG,
                tag = null,
                message = "Debug message with exception arg1",
                throwable = exception,
                timestamp = TIMESTAMP,
            ),
        )
        assertThat(events[5]).isEqualTo(
            LogEvent(
                level = LogLevel.DEBUG,
                tag = null,
                message = "Test exception",
                throwable = exception,
                timestamp = TIMESTAMP,
            ),
        )

        // Verify info events
        assertThat(events[6]).isEqualTo(
            LogEvent(
                level = LogLevel.INFO,
                tag = null,
                message = "Info message arg1",
                throwable = null,
                timestamp = TIMESTAMP,
            ),
        )
        assertThat(events[7]).isEqualTo(
            LogEvent(
                level = LogLevel.INFO,
                tag = null,
                message = "Info message with exception arg1",
                throwable = exception,
                timestamp = TIMESTAMP,
            ),
        )
        assertThat(events[8]).isEqualTo(
            LogEvent(
                level = LogLevel.INFO,
                tag = null,
                message = "Test exception",
                throwable = exception,
                timestamp = TIMESTAMP,
            ),
        )

        // Verify warn events
        assertThat(events[9]).isEqualTo(
            LogEvent(
                level = LogLevel.WARN,
                tag = null,
                message = "Warn message arg1",
                throwable = null,
                timestamp = TIMESTAMP,
            ),
        )
        assertThat(events[10]).isEqualTo(
            LogEvent(
                level = LogLevel.WARN,
                tag = null,
                message = "Warn message with exception arg1",
                throwable = exception,
                timestamp = TIMESTAMP,
            ),
        )
        assertThat(events[11]).isEqualTo(
            LogEvent(
                level = LogLevel.WARN,
                tag = null,
                message = "Test exception",
                throwable = exception,
                timestamp = TIMESTAMP,
            ),
        )

        // Verify error events
        assertThat(events[12]).isEqualTo(
            LogEvent(
                level = LogLevel.ERROR,
                tag = null,
                message = "Error message arg1",
                throwable = null,
                timestamp = TIMESTAMP,
            ),
        )
        assertThat(events[13]).isEqualTo(
            LogEvent(
                level = LogLevel.ERROR,
                tag = null,
                message = "Error message with exception arg1",
                throwable = exception,
                timestamp = TIMESTAMP,
            ),
        )
        assertThat(events[14]).isEqualTo(
            LogEvent(
                level = LogLevel.ERROR,
                tag = null,
                message = "Test exception",
                throwable = exception,
                timestamp = TIMESTAMP,
            ),
        )
    }
}
