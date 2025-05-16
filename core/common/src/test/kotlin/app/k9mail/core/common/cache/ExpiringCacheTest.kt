package app.k9mail.core.common.cache

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.time.Duration.Companion.milliseconds
import net.thunderbird.core.testing.TestClock

class ExpiringCacheTest {

    private val clock = TestClock()

    private val testSubject: Cache<String, String> = ExpiringCache(clock, InMemoryCache())

    @Test
    fun `get should return null when entry present and cache expired`() {
        testSubject[KEY] = VALUE
        clock.advanceTimeBy(CACHE_TIME_VALIDITY_DURATION)

        val result = testSubject[KEY]

        assertThat(result).isNull()
    }

    @Test
    fun `set should clear cache and add new entry when cache expired`() {
        testSubject[KEY] = VALUE
        clock.advanceTimeBy(CACHE_TIME_VALIDITY_DURATION)

        testSubject[KEY + 1] = "$VALUE changed"

        assertThat(testSubject[KEY]).isNull()
        assertThat(testSubject[KEY + 1]).isEqualTo("$VALUE changed")
    }

    @Test
    fun `hasKey should answer no when cache has entry and validity expired`() {
        testSubject[KEY] = VALUE
        clock.advanceTimeBy(CACHE_TIME_VALIDITY_DURATION)

        assertThat(testSubject.hasKey(KEY)).isFalse()
    }

    @Test
    fun `should keep cache when time progresses within expiration`() {
        testSubject[KEY] = VALUE
        clock.advanceTimeBy(CACHE_TIME_VALIDITY_DURATION.minus(1L.milliseconds))

        assertThat(testSubject[KEY]).isEqualTo(VALUE)
    }

    @Test
    fun `should empty cache after time progresses to expiration`() {
        testSubject[KEY] = VALUE

        clock.advanceTimeBy(CACHE_TIME_VALIDITY_DURATION)

        assertThat(testSubject[KEY]).isNull()
    }

    private companion object {
        const val KEY = "key"
        const val VALUE = "value"
        val CACHE_TIME_VALIDITY_DURATION = 30_000L.milliseconds
    }
}
