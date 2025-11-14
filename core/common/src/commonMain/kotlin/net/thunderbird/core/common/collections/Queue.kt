package net.thunderbird.core.common.collections

/**
 * A collection designed for holding elements prior to processing.
 * Besides basic [Collection] operations, queues provide additional insertion, extraction, and inspection operations.
 *
 * Queues typically, but do not necessarily, order elements in a FIFO (first-in-first-out) manner.
 */
interface Queue<T> : Collection<T> {
    /**
     * Inserts the specified element into this queue.
     *
     * @return `true` if the element was added to this queue.
     * @throws IllegalStateException if the element cannot be added at this time due to capacity restrictions.
     */
    fun add(element: T): Boolean

    /**
     * Inserts the specified element into this queue.
     *
     * @return `true` if the element was added to this queue, else `false`.
     */
    fun offer(element: T): Boolean

    /**
     * Retrieves and removes the head of this queue.
     *
     * @return the head of this queue.
     * @throws NoSuchElementException if this queue is empty.
     */
    fun remove(): T

    /**
     * @return Retrieves and removes the head of this queue, or returns `null` if this queue is empty.
     */
    fun poll(): T?

    /**
     * Retrieves, but does not remove, the head of this queue.
     *
     * @return the head of this queue.
     * @throws NoSuchElementException if this queue is empty.
     */
    fun element(): T

    /**
     * @return Retrieves, but does not remove, the head of this queue, or returns `null` if this queue is empty.
     */
    fun peek(): T?
}

abstract class AbstractQueue<T> : AbstractMutableCollection<T>(), Queue<T> {

    override fun element(): T {
        val head = peek()
        if (head != null) {
            return head
        } else {
            error("Collection empty")
        }
    }

    override fun add(element: T): Boolean {
        if (offer(element)) {
            return true
        } else {
            error("Collection full")
        }
    }

    override fun addAll(elements: Collection<T>): Boolean {
        require(elements != this) { "Collection can't addAll itself" }
        var modified = false
        for (element in elements) {
            if (add(element)) {
                modified = true
            }
        }
        return modified
    }

    override fun remove(): T {
        val head = poll()
        if (head != null) {
            return head
        } else {
            throw NoSuchElementException("Queue is empty")
        }
    }

    override fun clear() {
        while (!isEmpty()) {
            remove()
        }
    }
}
