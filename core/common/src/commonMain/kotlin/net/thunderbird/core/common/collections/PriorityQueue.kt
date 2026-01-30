package net.thunderbird.core.common.collections

/**
 * An unbounded priority queue based on a binary heap.
 *
 * The elements of the priority queue are ordered according to the provided [Comparator].
 * The head of this queue is the *least* element with respect to the specified ordering.
 * If multiple elements are tied for least value, the head is one of those elements — ties
 * are broken arbitrarily.
 *
 * The queue does not permit `null` elements.
 *
 * This class and its iterator implement all of the *optional* methods of the [Collection] and
 * [Iterator] interfaces. The iterator returned by the `iterator()` method is *not* guaranteed
 * to traverse the elements of the priority queue in any particular order.
 * If you need ordered traversal, consider draining the queue into a list, for example:
 *
 * ```kotlin
 * val list = mutableListOf<T>()
 * while (queue.isNotEmpty()) {
 *     list.add(queue.poll())
 * }
 * ```
 *
 * This implementation is not thread-safe.
 *
 * @param T The type of elements held in this collection.
 * @param comparator The comparator that will be used to order this priority queue.
 * @param elements An optional list of initial elements to be added to the queue.
 */
class PriorityQueue<T>(
    private val comparator: Comparator<in T>,
    elements: List<T> = emptyList(),
) : BaseQueue<T>() {
    private val heap = elements.toMutableList()

    init {
        if (heap.isNotEmpty()) {
            heapify()
        }
    }

    override val size: Int get() = heap.size
    override fun isEmpty(): Boolean = heap.isEmpty()

    override fun iterator(): MutableIterator<T> = HeapIterator()

    override fun offer(element: T): Boolean {
        heap.add(element)
        siftUp(heap.lastIndex)
        return true
    }

    override fun poll(): T? {
        if (heap.isEmpty()) {
            return null
        }
        val result = heap.first()
        val last = heap.removeAt(heap.lastIndex)
        if (heap.isNotEmpty()) {
            heap[0] = last
            siftDown(0)
        }
        return result
    }

    override fun peek(): T? = heap.firstOrNull()

    private fun siftUp(index: Int) {
        var childIndex = index
        fun parentIndex(childIndex: Int) = (childIndex - 1) / 2

        while (childIndex > 0 && comparator.compare(heap[childIndex], heap[parentIndex(childIndex)]) < 0) {
            swap(childIndex, parentIndex(childIndex))
            childIndex = parentIndex(childIndex)
        }
    }

    private fun siftDown(index: Int) {
        var parentIndex = index
        fun leftChildIndex(parentIndex: Int) = (parentIndex * 2) + 1
        val half = heap.size / 2
        while (parentIndex < half) {
            var childIndex = leftChildIndex(parentIndex)
            val rightChildIndex = childIndex + 1
            if (rightChildIndex < heap.size &&
                comparator.compare(heap[childIndex], heap[rightChildIndex]) > 0
            ) {
                childIndex = rightChildIndex
            }

            if (comparator.compare(heap[parentIndex], heap[childIndex]) <= 0) {
                break
            }

            swap(parentIndex, childIndex)
            parentIndex = childIndex
        }
    }

    private fun heapify() {
        for (index in ((heap.lastIndex - 1) / 2) downTo 0) {
            siftDown(index)
        }
    }

    private fun swap(first: Int, second: Int) {
        val temp = heap[first]
        heap[first] = heap[second]
        heap[second] = temp
    }

    private fun removeAt(i: Int) {
        val lastIndex = heap.lastIndex
        if (i == lastIndex) {
            heap.removeAt(lastIndex)
            return
        }
        val moved = heap.removeAt(lastIndex)
        heap[i] = moved
        siftDown(i)
        siftUp(i)
    }

    private inner class HeapIterator : MutableIterator<T> {
        private var index = 0
        private var lastReturned = -1

        override fun hasNext(): Boolean = index < heap.size

        override fun next(): T {
            if (!hasNext()) throw NoSuchElementException("Queue is empty")
            lastReturned = index++
            return heap[lastReturned]
        }

        override fun remove() {
            require(lastReturned >= 0) { "next() has not been called" }
            removeAt(lastReturned)
            index = lastReturned--
        }
    }
}

/**
 * Creates a [PriorityQueue] that orders its elements according to their natural-order,
 * resulting in a min-priority queue (the smallest element is at the head).
 *
 * The elements must be [Comparable].
 *
 * @param T the type of elements in the priority queue.
 * @param elements an optional [Iterable] of initial elements to be added to the queue.
 * @return a [PriorityQueue] containing the specified elements, ordered by their natural ascending order.
 */
inline fun <reified T> minPriorityQueueOf(
    elements: Iterable<T> = emptyList(),
): PriorityQueue<T> where T : Comparable<T> = PriorityQueue(
    comparator = { a, b -> a.compareTo(b) },
    elements = elements.toList(),
)

/**
 * Creates a [PriorityQueue] that orders its elements according to their reverse-order,
 * resulting in a max-priority queue (the largest element is at the head).
 *
 * The elements must be [Comparable].
 *
 * @param T the type of elements in the priority queue.
 * @param elements an optional [Iterable] of initial elements to be added to the queue.
 * @return a [PriorityQueue] containing the specified elements, ordered by their natural descending order.
 */
inline fun <reified T> maxPriorityQueueOf(
    elements: Iterable<T> = emptyList(),
): PriorityQueue<T> where T : Comparable<T> = PriorityQueue(
    comparator = { a, b -> b.compareTo(a) },
    elements = elements.toList(),
)
