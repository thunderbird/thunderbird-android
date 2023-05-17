package app.k9mail.autodiscovery.autoconfig

import assertk.assertThat
import assertk.assertions.isEqualTo
import org.junit.Test

class OkHttpBaseDomainExtractorTest {
    private val baseDomainExtractor = OkHttpBaseDomainExtractor()

    @Test
    fun `basic domain`() {
        assertThat(baseDomainExtractor.extractBaseDomain("domain.example")).isEqualTo("domain.example")
    }

    @Test
    fun `basic subdomain`() {
        assertThat(baseDomainExtractor.extractBaseDomain("subdomain.domain.example")).isEqualTo("domain.example")
    }

    @Test
    fun `domain with public suffix`() {
        assertThat(baseDomainExtractor.extractBaseDomain("example.co.uk")).isEqualTo("example.co.uk")
    }

    @Test
    fun `subdomain with public suffix`() {
        assertThat(baseDomainExtractor.extractBaseDomain("subdomain.example.co.uk")).isEqualTo("example.co.uk")
    }
}
