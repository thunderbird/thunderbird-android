package com.fsck.k9

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import org.junit.Test

class EmailAddressValidatorTest {
    private val validator = EmailAddressValidator()

    @Test
    fun testEmailValidation() {
        // Most of the tests based on https://en.wikipedia.org/wiki/Email_address#Examples
        assertValidAddress("simple@example.com")
        assertValidAddress("very.common@example.com")
        assertValidAddress("disposable.style.email.with+symbol@example.com")
        assertValidAddress("other.email-with-hyphen@example.com")
        assertValidAddress("fully-qualified-domain@example.com")
        assertValidAddress("user.name+tag+sorting@example.com")
        assertValidAddress("example-indeed@strange-example.com")
        assertValidAddress("example-indeed@strange_example.com")
        assertValidAddress("example@1.com")
        assertValidAddress("admin@mailserver1")
        assertValidAddress("user@localserver")
        assertValidAddress("\"very.(),:;<>[]\\\".VERY.\\\"very@\\\\ \\\"very\\\".unusual\"@strange.example.com")
        assertValidAddress("\"()<>[]:,;@\\\\\\\"!#$%&'-/=?^_`{}| ~.a\"@example.org")
        assertValidAddress("\" \"@example.org")
        assertValidAddress("x@example.com")

        assertInvalidAddress("Abc.example.com")
        assertInvalidAddress("\"not\"right@example.com")
        assertInvalidAddress("john.doe@example..com")
        assertInvalidAddress("example@c.2")
        assertInvalidAddress("this\\ still\\\"not\\\\allowed@example.com")
        assertInvalidAddress("john..doe@example.com")
        assertInvalidAddress("invalidperiod.@example.com")
    }

    private fun assertValidAddress(input: String) {
        assertThat(input).transform { validator.isValidAddressOnly(it) }.isTrue()
    }

    private fun assertInvalidAddress(input: String) {
        assertThat(input).transform { validator.isValidAddressOnly(it) }.isFalse()
    }
}
