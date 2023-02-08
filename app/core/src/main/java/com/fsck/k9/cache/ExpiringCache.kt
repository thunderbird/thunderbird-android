package com.fsck.k9.cache

import com.fsck.k9.Clock

internal class ExpiringCache<KEY : Any, VALUE : Any>(
    private val clock: Clock,
    private val delegateCache: Cache<KEY, VALUE> = InMemoryCache(),
    private var lastClearTime: Long = clock.time,
    private val cacheTimeValidity: Long = CACHE_TIME_VALIDITY_IN_MILLIS
) : Cache<KEY, VALUE> {

    override fun get(key: KEY): VALUE? {
        recycle()
        return delegateCache[key]
    }

    override fun set(key: KEY, value: VALUE) {
        recycle()
        delegateCache[key] = value
    }

    override fun hasKey(key: KEY): Boolean {
        recycle()
        return delegateCache.hasKey(key)
    }

    override fun clear() {
        lastClearTime = clock.time
        delegateCache.clear()
    }

    private fun recycle() {
        if (isExpired()) {
            clear()
        }
    }

    private fun isExpired(): Boolean {
        return (clock.time - lastClearTime) >= cacheTimeValidity
    }

    private companion object {
        const val CACHE_TIME_VALIDITY_IN_MILLIS = 30_000L
    }
}
