package com.fsck.k9.helper

/**
 * Returns a [Set] containing the results of applying the given [transform] function to each element in the original
 * collection.
 *
 * If you know the size of the output or can make an educated guess, specify [expectedSize] as an optimization.
 * The initial capacity of the `Set` will be derived from this value.
 */
inline fun <T, R> Iterable<T>.mapToSet(expectedSize: Int? = null, transform: (T) -> R): Set<R> {
    return if (expectedSize != null) {
        mapTo(LinkedHashSet(setCapacity(expectedSize)), transform)
    } else {
        mapTo(mutableSetOf(), transform)
    }
}

/**
 * Returns a [Set] containing the results of applying the given [transform] function to each element in the original
 * collection.
 *
 * The size of the output is expected to be equal to the size of the input. If that's not the case, please use
 * [mapToSet] instead.
 */
inline fun <T, R> Collection<T>.mapCollectionToSet(transform: (T) -> R): Set<R> {
    return mapToSet(expectedSize = size, transform)
}

// A copy of Kotlin's internal mapCapacity() for the JVM
fun setCapacity(expectedSize: Int): Int = when {
    // We are not coercing the value to a valid one and not throwing an exception. It is up to the caller to
    // properly handle negative values.
    expectedSize < 0 -> expectedSize
    expectedSize < 3 -> expectedSize + 1
    expectedSize < INT_MAX_POWER_OF_TWO -> ((expectedSize / 0.75F) + 1.0F).toInt()
    // any large value
    else -> Int.MAX_VALUE
}

private const val INT_MAX_POWER_OF_TWO: Int = 1 shl (Int.SIZE_BITS - 2)
