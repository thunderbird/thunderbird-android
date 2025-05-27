package net.thunderbird.core.common.net

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import org.junit.Test

class DomainTest {
    @Test
    fun `valid domain`() {
        val domain = Domain("domain.example")

        assertThat(domain.value).isEqualTo("domain.example")
    }

    @Test
    fun `invalid domain should throw`() {
        assertFailure {
            Domain("invalid domain")
        }.isInstanceOf<IllegalArgumentException>()
            .hasMessage("Not a valid domain name: 'invalid domain'")
    }
}
