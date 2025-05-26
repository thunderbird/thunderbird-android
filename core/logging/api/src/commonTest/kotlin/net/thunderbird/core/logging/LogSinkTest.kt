package net.thunderbird.core.logging

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class LogSinkTest {

    @Test
    fun `canLog should return true for same level`() {
        // Arrange
        val testSubject = TestLogSink(LogLevel.INFO)

        // Act && Assert
        assertTrue { testSubject.canLog(LogLevel.INFO) }
    }

    @Test
    fun `canLog should return false for level below sink level`() {
        // Arrange
        val testSubject = TestLogSink(LogLevel.INFO)

        // Act && Assert
        assertFalse { testSubject.canLog(LogLevel.DEBUG) }
    }

    @Test
    fun `canLog should return true for level above sink level`() {
        // Arrange
        val testSubject = TestLogSink(LogLevel.INFO)

        // Act && Assert
        assertTrue { testSubject.canLog(LogLevel.WARN) }
    }

    private class TestLogSink(
        override val level: LogLevel,
    ) : LogSink {
        override fun log(event: LogEvent) = Unit
    }
}
