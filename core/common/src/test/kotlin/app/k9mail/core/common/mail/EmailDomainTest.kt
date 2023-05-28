package app.k9mail.core.common.mail

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class EmailDomainTest {
    @Test
    fun `simple domain`() {
        val domain = EmailDomain("DOMAIN.example")

        assertThat(domain.value).isEqualTo("DOMAIN.example")
        assertThat(domain.normalized).isEqualTo("domain.example")
        assertThat(domain.toString()).isEqualTo("DOMAIN.example")
    }

    @Test
    fun `equals() does case-insensitive comparison`() {
        val domain1 = EmailDomain("domain.example")
        val domain2 = EmailDomain("DOMAIN.example")

        assertThat(domain2).isEqualTo(domain1)
    }
}
