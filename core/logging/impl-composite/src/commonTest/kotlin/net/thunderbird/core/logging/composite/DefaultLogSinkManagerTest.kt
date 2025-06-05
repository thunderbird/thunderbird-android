package net.thunderbird.core.logging.composite

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEmpty
import kotlin.test.Test
import net.thunderbird.core.logging.LogLevel

class DefaultLogSinkManagerTest {

    @Test
    fun `should have no sinks initially`() {
        // Arrange
        val sinkManager = DefaultLogSinkManager()

        // Act
        val sinks = sinkManager.getAll()

        // Assert
        assertThat(sinks).isEmpty()
    }

    @Test
    fun `should add and retrieve sinks`() {
        // Arrange
        val sinkManager = DefaultLogSinkManager()
        val sink = FakeLogSink(LogLevel.INFO)
        sinkManager.add(sink)

        // Act
        val sinks = sinkManager.getAll()

        // Assert
        assertThat(sinks.contains(sink))
    }

    @Test
    fun `should add multiple sinks`() {
        // Arrange
        val sinkManager = DefaultLogSinkManager()
        val sink1 = FakeLogSink(LogLevel.INFO)
        val sink2 = FakeLogSink(LogLevel.DEBUG)
        sinkManager.addAll(listOf(sink1, sink2))

        // Act
        val sinks = sinkManager.getAll()

        // Assert
        assertThat(sinks).hasSize(2)
        assertThat(sinks).contains(sink1)
        assertThat(sinks).contains(sink2)
    }

    @Test
    fun `should remove sink`() {
        // Arrange
        val sinkManager = DefaultLogSinkManager()
        val sink = FakeLogSink(LogLevel.INFO)
        sinkManager.add(sink)

        // Act
        sinkManager.remove(sink)
        val sinks = sinkManager.getAll()

        // Assert
        assertThat(sinks).isEmpty()
    }

    @Test
    fun `should clear all sinks`() {
        // Arrange
        val sinkManager = DefaultLogSinkManager()
        val sink1 = FakeLogSink(LogLevel.INFO)
        val sink2 = FakeLogSink(LogLevel.DEBUG)
        sinkManager.add(sink1)
        sinkManager.add(sink2)

        // Act
        sinkManager.removeAll()
        val sinks = sinkManager.getAll()

        // Assert
        assertThat(sinks).isEmpty()
    }

    @Test
    fun `should not add duplicate sinks`() {
        // Arrange
        val sinkManager = DefaultLogSinkManager()
        val sink = FakeLogSink(LogLevel.INFO)
        sinkManager.add(sink)

        // Act
        sinkManager.add(sink)
        val sinks = sinkManager.getAll()

        // Assert
        assertThat(sinks).hasSize(1)
    }

    @Test
    fun `should not remove non-existent sinks`() {
        // Arrange
        val sinkManager = DefaultLogSinkManager()
        val sink = FakeLogSink(LogLevel.INFO)

        // Act
        sinkManager.remove(sink)
        val sinks = sinkManager.getAll()

        // Assert
        assertThat(sinks).isEmpty()
    }
}
