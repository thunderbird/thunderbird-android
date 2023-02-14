package com.fsck.k9.cache

import com.fsck.k9.TestClock
import com.google.common.truth.Truth.assertThat
import kotlin.test.Test

class ExpiringCacheTest {

    private val clock = TestClock()

    private val testSubject: Cache<String, String> = ExpiringCache(clock, InMemoryCache())

    @Test
    fun `get should return null when entry present and cache expired`() {
        testSubject[KEY] = VALUE
        advanceClockBy(CACHE_TIME_VALIDITY_IN_MILLIS)

        val result = testSubject[KEY]

        assertThat(result).isNull()
    }

    @Test
    fun `set should clear cache and add new entry when cache expired`() {
        testSubject[KEY] = VALUE
        advanceClockBy(CACHE_TIME_VALIDITY_IN_MILLIS)

        testSubject[KEY + 1] = "$VALUE changed"

        assertThat(testSubject[KEY]).isNull()
        assertThat(testSubject[KEY + 1]).isEqualTo("$VALUE changed")
    }

    @Test
    fun `hasKey should answer no when cache has entry and validity expired`() {
        testSubject[KEY] = VALUE
        advanceClockBy(CACHE_TIME_VALIDITY_IN_MILLIS)

        assertThat(testSubject.hasKey(KEY)).isFalse()
    }

    @Test
    fun `should keep cache when time progresses within expiration`() {
        testSubject[KEY] = VALUE
        advanceClockBy(CACHE_TIME_VALIDITY_IN_MILLIS - 1)

        assertThat(testSubject[KEY]).isEqualTo(VALUE)
    }

    @Test
    fun `should empty cache after time progresses to expiration`() {
        testSubject[KEY] = VALUE

        advanceClockBy(CACHE_TIME_VALIDITY_IN_MILLIS)

        assertThat(testSubject[KEY]).isNull()
    }

    private fun advanceClockBy(timeInMillis: Long) {
        clock.time = clock.time + timeInMillis
    }

    private companion object {
        const val KEY = "key"
        const val VALUE = "value"
        const val CACHE_TIME_VALIDITY_IN_MILLIS = 30_000L
    }
}
