package app.k9mail.core.common.cache

import app.k9mail.core.testing.TestClock
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

data class CacheTestData<KEY : Any, VALUE : Any?>(
    val name: String,
    val createCache: () -> Cache<KEY, VALUE>,
) {
    override fun toString(): String = name
}

@RunWith(Parameterized::class)
class CacheTest(data: CacheTestData<Any, Any?>) {

    private val testSubject = data.createCache()

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "{0}")
        fun data(): Collection<CacheTestData<Any, Any?>> {
            return listOf(
                CacheTestData("InMemoryCache") { InMemoryCache() },
                CacheTestData("ExpiringCache") { ExpiringCache(TestClock(), InMemoryCache()) },
                CacheTestData("SynchronizedCache") { SynchronizedCache(InMemoryCache()) },
            )
        }

        const val KEY = "key"
        const val VALUE = "value"
    }

    @Test
    fun `get should return null with empty cache`() {
        assertThat(testSubject[KEY]).isNull()
    }

    @Test
    fun `set should add entry with empty cache`() {
        testSubject[KEY] = VALUE

        assertThat(testSubject[KEY]).isEqualTo(VALUE)
    }

    @Test
    fun `set should overwrite entry when already present`() {
        testSubject[KEY] = VALUE

        testSubject[KEY] = "$VALUE changed"

        assertThat(testSubject[KEY]).isEqualTo("$VALUE changed")
    }

    @Test
    fun `hasKey should answer no with empty cache`() {
        assertThat(testSubject.hasKey(KEY)).isFalse()
    }

    @Test
    fun `hasKey should answer yes when cache has entry`() {
        testSubject[KEY] = VALUE

        assertThat(testSubject.hasKey(KEY)).isTrue()
    }

    @Test
    fun `clear should empty cache`() {
        testSubject[KEY] = VALUE

        testSubject.clear()

        assertThat(testSubject[KEY]).isNull()
    }
}
