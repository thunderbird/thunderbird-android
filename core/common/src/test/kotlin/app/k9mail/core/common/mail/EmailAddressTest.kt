package app.k9mail.core.common.mail

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isFailure
import assertk.assertions.isInstanceOf
import kotlin.test.Test

internal class EmailAddressTest {

    @Test
    fun `should reject blank email address`() {
        assertThat {
            EmailAddress("")
        }.isFailure()
            .isInstanceOf(IllegalArgumentException::class)
            .hasMessage("Email address must not be blank")
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
