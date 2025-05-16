package app.k9mail.autodiscovery.autoconfig

import assertk.assertThat
import assertk.assertions.isEqualTo
import net.thunderbird.core.common.net.toDomain
import org.junit.Test

class OkHttpBaseDomainExtractorTest {
    private val baseDomainExtractor = OkHttpBaseDomainExtractor()

    @Test
    fun `basic domain`() {
        val domain = "domain.example".toDomain()

        val result = baseDomainExtractor.extractBaseDomain(domain)

        assertThat(result).isEqualTo(domain)
    }

    @Test
    fun `basic subdomain`() {
        val domain = "subdomain.domain.example".toDomain()

        val result = baseDomainExtractor.extractBaseDomain(domain)

        assertThat(result).isEqualTo("domain.example".toDomain())
    }

    @Test
    fun `domain with public suffix`() {
        val domain = "example.co.uk".toDomain()

        val result = baseDomainExtractor.extractBaseDomain(domain)

        assertThat(result).isEqualTo(domain)
    }

    @Test
    fun `subdomain with public suffix`() {
        val domain = "subdomain.example.co.uk".toDomain()

        val result = baseDomainExtractor.extractBaseDomain(domain)

        assertThat(result).isEqualTo("example.co.uk".toDomain())
    }
}
