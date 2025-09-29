package net.thunderbird.core.common.cache

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.ExperimentalTime
import net.thunderbird.core.testing.TestClock

@OptIn(ExperimentalTime::class)
class TimeLimitedCacheTest {

    private val clock = TestClock()
    private val cache = TimeLimitedCache<String, String>(clock = clock)

    @Test
    fun `getValue should return null when entry present and expired`() {
        // Arrange
        cache.set(KEY, VALUE, expiresIn = EXPIRES_IN)
        clock.advanceTimeBy(EXPIRES_IN + 1.milliseconds)

        // Act
        val result = cache.getValue(KEY)

        // Assert
        assertThat(result).isNull()
    }

    @Test
    fun `hasKey should answer false when cache has entry and validity expired`() {
        // Arrange
        cache.set(KEY, VALUE, expiresIn = EXPIRES_IN)
        clock.advanceTimeBy(EXPIRES_IN + 1.milliseconds)

        // Act
        val result = cache.hasKey(KEY)

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `should keep cache when time progresses within expiration`() {
        // Arrange
        cache.set(KEY, VALUE, expiresIn = EXPIRES_IN)
        clock.advanceTimeBy(EXPIRES_IN - 1.milliseconds)

        // Act
        val result = cache.getValue(KEY)

        // Assert
        assertThat(result).isEqualTo(VALUE)
    }

    @Test
    fun `clearExpired should remove only expired entries`() {
        // Arrange
        cache.set(KEY, VALUE, expiresIn = EXPIRES_IN)
        cache.set(KEY_2, VALUE_2, expiresIn = EXPIRES_IN * 2)
        clock.advanceTimeBy(EXPIRES_IN + 1.milliseconds)

        // Act
        cache.clearExpired()

        // Assert
        assertThat(cache.getValue(KEY)).isNull()
        assertThat(cache.getValue(KEY_2)).isEqualTo(VALUE_2)
    }

    @Test
    fun `get should return Entry with correct metadata when not expired`() {
        // Arrange
        cache.set(KEY, VALUE, expiresIn = EXPIRES_IN)

        // Act
        val entry = cache[KEY]

        // Assert
        assertThat(entry).isNotNull()
        entry!!
        assertThat(entry.value).isEqualTo(VALUE)
        assertThat(entry.expiresIn).isEqualTo(EXPIRES_IN)
        assertThat(entry.expiresAt).isEqualTo(entry.creationTime + EXPIRES_IN)
    }

    private companion object {
        const val KEY = "key"
        const val KEY_2 = "key2"
        const val VALUE = "value"
        const val VALUE_2 = "value2"
        val EXPIRES_IN: Duration = 500.milliseconds
    }
}
