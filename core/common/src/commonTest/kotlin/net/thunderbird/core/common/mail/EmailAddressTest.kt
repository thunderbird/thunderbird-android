package net.thunderbird.core.common.mail

import assertk.assertThat
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import kotlin.test.Test
import net.thunderbird.core.common.mail.EmailAddress.Warning

class EmailAddressTest {
    @Test
    fun `simple email address`() {
        val domain = EmailDomain("DOMAIN.example")
        val emailAddress = EmailAddress(localPart = "user", domain = domain)

        assertThat(emailAddress.localPart).isEqualTo("user")
        assertThat(emailAddress.encodedLocalPart).isEqualTo("user")
        assertThat(emailAddress.domain).isSameInstanceAs(domain)
        assertThat(emailAddress.address).isEqualTo("user@DOMAIN.example")
        assertThat(emailAddress.normalizedAddress).isEqualTo("user@domain.example")
        assertThat(emailAddress.toString()).isEqualTo("user@DOMAIN.example")
        assertThat(emailAddress.warnings).isEmpty()
    }

    @Test
    fun `local part that requires use of quoted string`() {
        val emailAddress = EmailAddress(localPart = "foo bar", domain = EmailDomain("domain.example"))

        assertThat(emailAddress.localPart).isEqualTo("foo bar")
        assertThat(emailAddress.encodedLocalPart).isEqualTo("\"foo bar\"")
        assertThat(emailAddress.address).isEqualTo("\"foo bar\"@domain.example")
        assertThat(emailAddress.normalizedAddress).isEqualTo("\"foo bar\"@domain.example")
        assertThat(emailAddress.toString()).isEqualTo("\"foo bar\"@domain.example")
        assertThat(emailAddress.warnings).containsExactlyInAnyOrder(Warning.QuotedStringInLocalPart)
    }

    @Test
    fun `empty local part`() {
        val emailAddress = EmailAddress(localPart = "", domain = EmailDomain("domain.example"))

        assertThat(emailAddress.localPart).isEqualTo("")
        assertThat(emailAddress.encodedLocalPart).isEqualTo("\"\"")
        assertThat(emailAddress.address).isEqualTo("\"\"@domain.example")
        assertThat(emailAddress.normalizedAddress).isEqualTo("\"\"@domain.example")
        assertThat(emailAddress.toString()).isEqualTo("\"\"@domain.example")
        assertThat(emailAddress.warnings).containsExactlyInAnyOrder(
            Warning.QuotedStringInLocalPart,
            Warning.EmptyLocalPart,
        )
    }

    @Test
    fun `equals() does case-insensitive domain comparison`() {
        val emailAddress1 = EmailAddress(localPart = "user", domain = EmailDomain("domain.example"))
        val emailAddress2 = EmailAddress(localPart = "user", domain = EmailDomain("DOMAIN.example"))

        assertThat(emailAddress2).isEqualTo(emailAddress1)
    }
}
