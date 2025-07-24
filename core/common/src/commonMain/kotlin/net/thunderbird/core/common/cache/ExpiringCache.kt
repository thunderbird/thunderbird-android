package net.thunderbird.core.common.cache

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class ExpiringCache<KEY : Any, VALUE : Any?>
@OptIn(ExperimentalTime::class)
constructor(
    private val clock: Clock,
    private val delegateCache: Cache<KEY, VALUE> = InMemoryCache(),
    private var lastClearTime: Instant = clock.now(),
    private val cacheTimeValidity: Long = CACHE_TIME_VALIDITY_IN_MILLIS,
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
        @OptIn(ExperimentalTime::class)
        lastClearTime = clock.now()
        delegateCache.clear()
    }

    private fun recycle() {
        if (isExpired()) {
            clear()
        }
    }

    private fun isExpired(): Boolean {
        @OptIn(ExperimentalTime::class)
        return (clock.now() - lastClearTime).inWholeMilliseconds >= cacheTimeValidity
    }

    private companion object {
        const val CACHE_TIME_VALIDITY_IN_MILLIS = 30_000L
    }
}
