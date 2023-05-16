package app.k9mail.autodiscovery.autoconfig

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class RealSubDomainExtractorTest {
    private val testBaseDomainExtractor = TestBaseDomainExtractor(baseDomain = "domain.example")
    private val baseSubDomainExtractor = RealSubDomainExtractor(testBaseDomainExtractor)

    @Test
    fun `input has one more label than the base domain`() {
        val result = baseSubDomainExtractor.extractSubDomain("subdomain.domain.example")

        assertThat(result).isEqualTo("domain.example")
    }

    @Test
    fun `input has two more labels than the base domain`() {
        val result = baseSubDomainExtractor.extractSubDomain("more.subdomain.domain.example")

        assertThat(result).isEqualTo("subdomain.domain.example")
    }

    @Test
    fun `input has three more labels than the base domain`() {
        val result = baseSubDomainExtractor.extractSubDomain("three.two.one.domain.example")

        assertThat(result).isEqualTo("two.one.domain.example")
    }

    @Test
    fun `no sub domain available`() {
        val result = baseSubDomainExtractor.extractSubDomain("domain.example")

        assertThat(result).isNull()
    }

    @Test
    fun `input has one more label than the base domain with public suffix`() {
        testBaseDomainExtractor.baseDomain = "example.co.uk"

        val result = baseSubDomainExtractor.extractSubDomain("subdomain.example.co.uk")

        assertThat(result).isEqualTo("example.co.uk")
    }

    @Test
    fun `input has two more labels than the base domain with public suffix`() {
        testBaseDomainExtractor.baseDomain = "example.co.uk"

        val result = baseSubDomainExtractor.extractSubDomain("more.subdomain.example.co.uk")

        assertThat(result).isEqualTo("subdomain.example.co.uk")
    }

    @Test
    fun `input has three more labels than the base domain with public suffix`() {
        testBaseDomainExtractor.baseDomain = "example.co.uk"

        val result = baseSubDomainExtractor.extractSubDomain("three.two.one.example.co.uk")

        assertThat(result).isEqualTo("two.one.example.co.uk")
    }

    @Test
    fun `no sub domain available with public suffix`() {
        testBaseDomainExtractor.baseDomain = "example.co.uk"

        val result = baseSubDomainExtractor.extractSubDomain("example.co.uk")

        assertThat(result).isNull()
    }
}

private class TestBaseDomainExtractor(var baseDomain: String) : BaseDomainExtractor {
    override fun extractBaseDomain(domain: String): String {
        return baseDomain
    }
}
