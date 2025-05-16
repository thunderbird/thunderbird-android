package app.k9mail.autodiscovery.autoconfig

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test
import net.thunderbird.core.common.net.Domain
import net.thunderbird.core.common.net.toDomain

class RealSubDomainExtractorTest {
    private val testBaseDomainExtractor = TestBaseDomainExtractor(baseDomain = "domain.example")
    private val baseSubDomainExtractor = RealSubDomainExtractor(testBaseDomainExtractor)

    @Test
    fun `input has one more label than the base domain`() {
        val domain = "subdomain.domain.example".toDomain()

        val result = baseSubDomainExtractor.extractSubDomain(domain)

        assertThat(result).isEqualTo("domain.example".toDomain())
    }

    @Test
    fun `input has two more labels than the base domain`() {
        val domain = "more.subdomain.domain.example".toDomain()

        val result = baseSubDomainExtractor.extractSubDomain(domain)

        assertThat(result).isEqualTo("subdomain.domain.example".toDomain())
    }

    @Test
    fun `input has three more labels than the base domain`() {
        val domain = "three.two.one.domain.example".toDomain()

        val result = baseSubDomainExtractor.extractSubDomain(domain)

        assertThat(result).isEqualTo("two.one.domain.example".toDomain())
    }

    @Test
    fun `no sub domain available`() {
        val domain = "domain.example".toDomain()

        val result = baseSubDomainExtractor.extractSubDomain(domain)

        assertThat(result).isNull()
    }

    @Test
    fun `input has one more label than the base domain with public suffix`() {
        val domain = "subdomain.example.co.uk".toDomain()
        testBaseDomainExtractor.baseDomain = "example.co.uk"

        val result = baseSubDomainExtractor.extractSubDomain(domain)

        assertThat(result).isEqualTo("example.co.uk".toDomain())
    }

    @Test
    fun `input has two more labels than the base domain with public suffix`() {
        val domain = "more.subdomain.example.co.uk".toDomain()
        testBaseDomainExtractor.baseDomain = "example.co.uk"

        val result = baseSubDomainExtractor.extractSubDomain(domain)

        assertThat(result).isEqualTo("subdomain.example.co.uk".toDomain())
    }

    @Test
    fun `input has three more labels than the base domain with public suffix`() {
        val domain = "three.two.one.example.co.uk".toDomain()
        testBaseDomainExtractor.baseDomain = "example.co.uk"

        val result = baseSubDomainExtractor.extractSubDomain(domain)

        assertThat(result).isEqualTo("two.one.example.co.uk".toDomain())
    }

    @Test
    fun `no sub domain available with public suffix`() {
        val domain = "example.co.uk".toDomain()
        testBaseDomainExtractor.baseDomain = "example.co.uk"

        val result = baseSubDomainExtractor.extractSubDomain(domain)

        assertThat(result).isNull()
    }
}

private class TestBaseDomainExtractor(var baseDomain: String) : BaseDomainExtractor {
    override fun extractBaseDomain(domain: Domain): Domain {
        return Domain(baseDomain)
    }
}
