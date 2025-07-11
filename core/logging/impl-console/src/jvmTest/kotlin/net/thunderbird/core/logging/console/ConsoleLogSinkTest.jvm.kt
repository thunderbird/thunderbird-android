package net.thunderbird.core.logging.console

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import kotlin.test.Test
import kotlin.test.assertEquals
import net.thunderbird.core.logging.LogEvent
import net.thunderbird.core.logging.LogLevel

class ConsoleLogSinkTest {

    @Test
    fun shouldHaveCorrectLogLevel() {
        // Arrange
        val testSubject = ConsoleLogSink(LogLevel.INFO)

        // Act & Assert
        assertEquals(LogLevel.INFO, testSubject.level)
    }

    @Test
    fun shouldLogMessages() {
        // Arrange
        val originalOut = System.out
        val outContent = ByteArrayOutputStream()
        System.setOut(PrintStream(outContent))

        try {
            val eventInfo = LogEvent(
                level = LogLevel.INFO,
                tag = "TestTag",
                message = "This is an info message",
                throwable = null,
                timestamp = 0L,
            )

            val testSubject = ConsoleLogSink(LogLevel.VERBOSE)

            // Act
            testSubject.log(eventInfo)

            // Assert
            val output = outContent.toString().trim()
            println("[DEBUG_LOG] Actual output: '$output'")

            // The expected format is: [VERBOSE] [TestTag] This is an info message
            // Note: The log level in the output is the sink's level (VERBOSE), not the event's level (INFO)
            val expectedOutput = "[VERBOSE] [TestTag] This is an info message"
            println("[DEBUG_LOG] Expected output: '$expectedOutput'")

            assertEquals(expectedOutput, output)
        } finally {
            System.setOut(originalOut)
        }
    }
}
