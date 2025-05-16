package net.thunderbird.core.common.cache

interface Cache<KEY : Any, VALUE : Any?> {

    operator fun get(key: KEY): VALUE?

    operator fun set(key: KEY, value: VALUE)

    fun hasKey(key: KEY): Boolean

    fun clear()
}
