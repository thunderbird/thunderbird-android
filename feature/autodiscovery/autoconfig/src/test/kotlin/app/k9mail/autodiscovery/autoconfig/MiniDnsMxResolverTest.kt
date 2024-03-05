package app.k9mail.autodiscovery.autoconfig

import app.k9mail.core.common.net.toDomain
import assertk.all
import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.extracting
import assertk.assertions.index
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Ignore
import kotlin.test.Test

class MiniDnsMxResolverTest {
    private val resolver = MiniDnsMxResolver()

    @Test
    @Ignore("Requires internet")
    fun `MX lookup for known domain`() {
        val domain = "thunderbird.net".toDomain()

        val result = resolver.lookup(domain)

        assertThat(result.mxNames).extracting { it.value }.all {
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

    @Test
    @Ignore("Requires internet")
    fun `MX lookup for known domain using DNSSEC`() {
        val domain = "posteo.de".toDomain()

        val result = resolver.lookup(domain)

        assertThat(result.isTrusted).isTrue()
    }

    @Test
    @Ignore("Requires internet")
    fun `MX lookup for non-existent domain`() {
        val domain = "test.invalid".toDomain()

        val result = resolver.lookup(domain)

        assertThat(result.mxNames).isEmpty()
        assertThat(result.isTrusted).isFalse()
    }
}
