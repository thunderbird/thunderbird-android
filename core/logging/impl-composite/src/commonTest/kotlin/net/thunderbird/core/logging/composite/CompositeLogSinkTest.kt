package net.thunderbird.core.logging.composite

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import net.thunderbird.core.logging.LogEvent
import net.thunderbird.core.logging.LogLevel
import org.junit.Test

class CompositeLogSinkTest {

    @Test
    fun `init should set initial sinks`() {
        // Arrange
        val sink1 = FakeLogSink(LogLevel.INFO)
        val sink2 = FakeLogSink(LogLevel.INFO)
        val sinkManager = FakeLogSinkManager()

        // Act
        CompositeLogSink(
            level = LogLevel.INFO,
            manager = sinkManager,
            sinks = listOf(sink1, sink2),
        )

        // Assert
        assertThat(sinkManager.sinks).hasSize(2)
        assertThat(sinkManager.sinks[0]).isEqualTo(sink1)
        assertThat(sinkManager.sinks[1]).isEqualTo(sink2)
    }

    @Test
    fun `log should log to all sinks`() {
        // Arrange
        val sink1 = FakeLogSink(LogLevel.INFO)
        val sink2 = FakeLogSink(LogLevel.INFO)
        val sinkManager = FakeLogSinkManager(mutableListOf(sink1, sink2))

        val testSubject = CompositeLogSink(
            level = LogLevel.INFO,
            manager = sinkManager,
        )

        // Act
        testSubject.log(LOG_EVENT)

        // Assert
        assertThat(sink1.events).hasSize(1)
        assertThat(sink2.events).hasSize(1)
        assertThat(sink1.events[0]).isEqualTo(LOG_EVENT)
        assertThat(sink2.events[0]).isEqualTo(LOG_EVENT)
    }

    @Test
    fun `log should not log if level is below threshold`() {
        // Arrange
        val sink1 = FakeLogSink(LogLevel.INFO)
        val sink2 = FakeLogSink(LogLevel.INFO)
        val sinkManager = FakeLogSinkManager(mutableListOf(sink1, sink2))

        val testSubject = CompositeLogSink(
            level = LogLevel.WARN,
            manager = sinkManager,
        )

        // Act
        testSubject.log(LOG_EVENT)

        // Assert
        assertThat(sink1.events).isEmpty()
        assertThat(sink2.events).isEmpty()
    }

    @Test
    fun `log should not log if sink level is below threshold`() {
        // Arrange
        val sink1 = FakeLogSink(LogLevel.WARN)
        val sink2 = FakeLogSink(LogLevel.INFO)
        val sinkManager = FakeLogSinkManager(mutableListOf(sink1, sink2))

        val testSubject = CompositeLogSink(
            level = LogLevel.INFO,
            manager = sinkManager,
        )

        // Act
        testSubject.log(LOG_EVENT)

        // Assert
        assertThat(sink1.events).isEmpty()
        assertThat(sink2.events).hasSize(1)
        assertThat(sink2.events[0]).isEqualTo(LOG_EVENT)
    }

    private companion object {
        const val TIMESTAMP = 0L

        val LOG_EVENT = LogEvent(
            level = LogLevel.INFO,
            tag = "TestTag",
            message = "Test message",
            timestamp = TIMESTAMP,
        )
    }
}
