package app.k9mail.core.common.mail

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlin.test.assertFails

internal class EmailAddressTest {

    @Test
    fun `should reject blank email address`() {
        assertFails("Email address must not be blank") {
            EmailAddress("")
        }
    }

    @Test
    fun `should return email address`() {
        val emailAddress = EmailAddress(EMAIL_ADDRESS)

        val address = emailAddress.address

        assertThat(address).isEqualTo(EMAIL_ADDRESS)
    }

    private companion object {
        private const val EMAIL_ADDRESS = "email@example.com"
    }
}
