package net.thunderbird.core.common.collections

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test

class PriorityQueueTests {
    @Test
    fun `minPriorityQueueOf - isEmpty returns true for new queue`() {
        // Arrange
        val queue = minPriorityQueueOf<Int>()

        // Act
        val isEmpty = queue.isEmpty()

        // Assert
        assertThat(isEmpty).isTrue()
    }

    @Test
    fun `isEmpty returns false for non-empty queue`() {
        // Arrange
        val queue = minPriorityQueueOf<Int>()
        queue.add(1)

        // Act
        val isEmpty = queue.isEmpty()

        // Assert
        assertThat(isEmpty).isFalse()
    }

    @Test
    fun `minPriorityQueueOf - size returns correct number of elements`() {
        // Arrange
        val queue = minPriorityQueueOf<String>()

        // Pre-Act Assert
        assertThat(queue.size).isEqualTo(0)

        // Act
        queue.add("A")

        // Assert
        assertThat(queue.size).isEqualTo(1)

        // Act (Phase 2)
        queue.add("B")

        // Assert (Phase 2)
        assertThat(queue.size).isEqualTo(2)

        // Act (Phase 3)
        queue.poll()

        // Assert (Phase 3)
        assertThat(queue.size).isEqualTo(1)
    }

    @Test
    fun `minPriorityQueueOf - poll returns null for empty queue`() {
        // Arrange
        val queue = minPriorityQueueOf<Double>()

        // Act
        val element = queue.poll()

        // Assert
        assertThat(element).isNull()
    }

    @Test
    fun `minPriorityQueueOf - peek returns null for empty queue`() {
        // Arrange
        val queue = minPriorityQueueOf<Int>()

        // Act
        val element = queue.peek()

        // Assert
        assertThat(element).isNull()
    }

    @Test
    fun `minPriorityQueueOf - peek returns the smallest element without removing it`() {
        // Arrange
        val queue = minPriorityQueueOf<Char>()
        queue.add('C')
        queue.add('A')
        queue.add('B')

        // Act
        val element1 = queue.peek()
        val element2 = queue.peek()

        // Assert
        assertThat(queue.size).isEqualTo(3)
        assertThat(element1).isEqualTo('A')
        assertThat(element2).isEqualTo('A') // Subsequent peeks return the same element
        assertThat(queue.size).isEqualTo(3)
    }

    @Test
    fun `minPriorityQueueOf - add and poll elements in natural order`() {
        // Arrange
        val queue = minPriorityQueueOf<Int>()
        queue.add(5)
        queue.add(1)
        queue.add(10)
        queue.add(3)

        // Act & Assert
        assertThat(queue.poll()).isEqualTo(1)
        assertThat(queue.poll()).isEqualTo(3)
        assertThat(queue.poll()).isEqualTo(5)
        assertThat(queue.poll()).isEqualTo(10)
        assertThat(queue.poll()).isNull()
    }

    @Test
    fun `minPriorityQueueOf - clear removes all elements from the queue`() {
        // Arrange
        val queue = minPriorityQueueOf<Int>()
        queue.add(10)
        queue.add(20)
        queue.add(5)

        // Act
        queue.clear()

        // Assert
        assertThat(queue.isEmpty()).isTrue()
        assertThat(queue.size).isEqualTo(0)
        assertThat(queue.peek()).isNull()
        assertThat(queue.poll()).isNull()
    }

    // maxPriorityQueueOf tests
    @Test
    fun `maxPriorityQueueOf - isEmpty returns true for new queue`() {
        // Arrange
        val queue = maxPriorityQueueOf<Int>()

        // Act
        val isEmpty = queue.isEmpty()

        // Assert
        assertThat(isEmpty).isTrue()
    }

    @Test
    fun `maxPriorityQueueOf - isEmpty returns false for non-empty queue`() {
        // Arrange
        val queue = maxPriorityQueueOf<Int>()
        queue.add(1)

        // Act
        val isEmpty = queue.isEmpty()

        // Assert
        assertThat(isEmpty).isFalse()
    }

    @Test
    fun `maxPriorityQueueOf - size returns correct number of elements`() {
        // Arrange
        val queue = maxPriorityQueueOf<String>()

        // Pre-Act Assert
        assertThat(queue.size).isEqualTo(0)

        // Act
        queue.add("A")

        // Assert
        assertThat(queue.size).isEqualTo(1)

        // Act (Phase 2)
        queue.add("B")

        // Assert (Phase 2)
        assertThat(queue.size).isEqualTo(2)

        // Act (Phase 3)
        queue.poll()

        // Assert (Phase 3)
        assertThat(queue.size).isEqualTo(1)
    }

    @Test
    fun `maxPriorityQueueOf - poll returns null for empty queue`() {
        // Arrange
        val queue = maxPriorityQueueOf<Double>()

        // Act
        val element = queue.poll()

        // Assert
        assertThat(element).isNull()
    }

    @Test
    fun `maxPriorityQueueOf - peek returns null for empty queue`() {
        // Arrange
        val queue = maxPriorityQueueOf<Int>()

        // Act
        val element = queue.peek()

        // Assert
        assertThat(element).isNull()
    }

    @Test
    fun `maxPriorityQueueOf - peek returns the largest element without removing it`() {
        // Arrange
        val queue = maxPriorityQueueOf<Char>()
        queue.add('C')
        queue.add('A')
        queue.add('D')
        queue.add('B')

        // Act
        val element1 = queue.peek()
        val element2 = queue.peek()

        // Assert
        assertThat(queue.size).isEqualTo(4)
        assertThat(element1).isEqualTo('D')
        assertThat(element2).isEqualTo('D') // Subsequent peeks return the same element
        assertThat(queue.size).isEqualTo(4)
    }

    @Test
    fun `maxPriorityQueueOf - add and poll elements in reverse natural order`() {
        // Arrange
        val queue = maxPriorityQueueOf<Int>()
        queue.add(5)
        queue.add(1)
        queue.add(10)
        queue.add(3)

        // Act & Assert
        assertThat(queue.poll()).isEqualTo(10)
        assertThat(queue.poll()).isEqualTo(5)
        assertThat(queue.poll()).isEqualTo(3)
        assertThat(queue.poll()).isEqualTo(1)
        assertThat(queue.poll()).isNull()
    }

    @Test
    fun `maxPriorityQueueOf - clear removes all elements from the queue`() {
        // Arrange
        val queue = maxPriorityQueueOf<Int>()
        queue.add(10)
        queue.add(20)
        queue.add(5)

        // Act
        queue.clear()

        // Assert
        assertThat(queue.isEmpty()).isTrue()
        assertThat(queue.size).isEqualTo(0)
        assertThat(queue.peek()).isNull()
        assertThat(queue.poll()).isNull()
    }
}
