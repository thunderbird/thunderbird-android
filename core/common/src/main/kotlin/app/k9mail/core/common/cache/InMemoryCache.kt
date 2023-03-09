package app.k9mail.core.common.cache

class InMemoryCache<KEY : Any, VALUE : Any?>(
    private val cache: MutableMap<KEY, VALUE> = mutableMapOf(),
) : Cache<KEY, VALUE> {
    override fun get(key: KEY): VALUE? {
        return cache[key]
    }

    override fun set(key: KEY, value: VALUE) {
        cache[key] = value
    }

    override fun hasKey(key: KEY): Boolean {
        return cache.containsKey(key)
    }

    override fun clear() {
        cache.clear()
    }
}
