package app.k9mail.autodiscovery.autoconfig

import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.index
import assertk.assertions.isEqualTo
import kotlin.test.Ignore
import kotlin.test.Test

class MiniDnsMxResolverTest {
    private val resolver = MiniDnsMxResolver()

    @Test
    @Ignore("Requires internet")
    fun `MX lookup for known domain`() {
        val result = resolver.lookup("thunderbird.net")

        assertThat(result).all {
            index(0).isEqualTo("aspmx.l.google.com")
            containsExactlyInAnyOrder(
                "aspmx.l.google.com",
                "alt1.aspmx.l.google.com",
                "alt2.aspmx.l.google.com",
                "alt4.aspmx.l.google.com",
                "alt3.aspmx.l.google.com",
            )
        }
    }
}
