package app.k9mail.core.common.cache

class SynchronizedCache<KEY : Any, VALUE : Any?>(
    private val delegateCache: Cache<KEY, VALUE>,
) : Cache<KEY, VALUE> {

    override fun get(key: KEY): VALUE? {
        synchronized(delegateCache) {
            return delegateCache[key]
        }
    }

    override fun set(key: KEY, value: VALUE) {
        synchronized(delegateCache) {
            delegateCache[key] = value
        }
    }

    override fun hasKey(key: KEY): Boolean {
        synchronized(delegateCache) {
            return delegateCache.hasKey(key)
        }
    }

    override fun clear() {
        synchronized(delegateCache) {
            delegateCache.clear()
        }
    }
}
