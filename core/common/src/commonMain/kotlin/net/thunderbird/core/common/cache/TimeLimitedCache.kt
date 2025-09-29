@file:OptIn(ExperimentalTime::class)

package net.thunderbird.core.common.cache

import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

class TimeLimitedCache<TKey : Any, TValue : Any?>(
    private val clock: Clock = Clock.System,
    private val cache: MutableMap<TKey, Entry<TValue>> = mutableMapOf(),
) : Cache<TKey, TimeLimitedCache.Entry<TValue>> {
    companion object {
        private val DEFAULT_EXPIRATION_TIME = 1.hours
    }

    override fun get(key: TKey): Entry<TValue>? {
        recycle(key)
        return cache[key]
    }

    fun getValue(key: TKey): TValue? = get(key)?.value

    fun set(key: TKey, value: TValue, expiresIn: Duration = DEFAULT_EXPIRATION_TIME) {
        set(key, Entry(value, creationTime = clock.now(), expiresIn))
    }

    override fun set(key: TKey, value: Entry<TValue>) {
        cache[key] = value
    }

    override fun hasKey(key: TKey): Boolean {
        recycle(key)
        return key in cache
    }

    override fun clear() {
        cache.clear()
    }

    fun clearExpired() {
        cache.entries.removeAll { (_, entry) ->
            entry.expiresAt < clock.now()
        }
    }

    private fun recycle(key: TKey) {
        val entry = cache[key] ?: return
        if (entry.expiresAt < clock.now()) {
            cache.remove(key)
        }
    }

    data class Entry<TValue : Any?>(
        val value: TValue,
        val creationTime: Instant,
        val expiresIn: Duration,
        val expiresAt: Instant = creationTime + expiresIn,
    )
}
