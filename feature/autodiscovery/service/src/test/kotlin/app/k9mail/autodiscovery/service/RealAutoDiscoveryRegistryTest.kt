package app.k9mail.autodiscovery.service

import app.k9mail.autodiscovery.api.AutoDiscovery
import app.k9mail.autodiscovery.api.AutoDiscoveryRunnable
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import net.thunderbird.core.common.mail.EmailAddress

class RealAutoDiscoveryRegistryTest {

    @Test
    fun `getAutoDiscoveries should return given discoveries`() {
        val autoDiscoveries = listOf(
            TestAutoDiscovery(),
            TestAutoDiscovery(),
        )
        val testSubject = RealAutoDiscoveryRegistry(autoDiscoveries)

        val result = testSubject.getAutoDiscoveries()

        assertThat(result).isEqualTo(autoDiscoveries)
    }

    private class TestAutoDiscovery : AutoDiscovery {
        override fun initDiscovery(email: EmailAddress): List<AutoDiscoveryRunnable> {
            return emptyList()
        }
    }
}
