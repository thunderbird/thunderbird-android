package com.fsck.k9

import org.junit.Assert
import org.junit.Test

class EmailAddressValidatorTest {

    @Test
    fun testEmailValidation() {
        // Most of the tests based on https://en.wikipedia.org/wiki/Email_address#Examples
        val validator = EmailAddressValidator()
        Assert.assertTrue(validator.isValidAddressOnly("simple@example.com"))
        Assert.assertTrue(validator.isValidAddressOnly("very.common@example.com"))
        Assert.assertTrue(validator.isValidAddressOnly("disposable.style.email.with+symbol@example.com"))
        Assert.assertTrue(validator.isValidAddressOnly("other.email-with-hyphen@example.com"))
        Assert.assertTrue(validator.isValidAddressOnly("fully-qualified-domain@example.com"))
        Assert.assertTrue(validator.isValidAddressOnly("user.name+tag+sorting@example.com"))
        Assert.assertTrue(validator.isValidAddressOnly("example-indeed@strange-example.com"))
        Assert.assertTrue(validator.isValidAddressOnly("example-indeed@strange_example.com"))
        Assert.assertTrue(validator.isValidAddressOnly("example@1.com"))
        Assert.assertTrue(validator.isValidAddressOnly("admin@mailserver1"))
        Assert.assertTrue(validator.isValidAddressOnly("user@localserver"))
        Assert.assertTrue(validator.isValidAddressOnly("\"very.(),:;<>[]\\\".VERY.\\\"very@\\\\ \\\"very\\\".unusual\"@strange.example.com"))
        Assert.assertTrue(validator.isValidAddressOnly("\"()<>[]:,;@\\\\\\\"!#$%&'-/=?^_`{}| ~.a\"@example.org"))
        Assert.assertTrue(validator.isValidAddressOnly("\" \"@example.org"))
        Assert.assertTrue(validator.isValidAddressOnly("x@example.com"))

        Assert.assertFalse(validator.isValidAddressOnly("Abc.example.com"))
        Assert.assertFalse(validator.isValidAddressOnly("\"not\"right@example.com"))
        Assert.assertFalse(validator.isValidAddressOnly("john.doe@example..com"))
        Assert.assertFalse(validator.isValidAddressOnly("example@c.2"))
        Assert.assertFalse(validator.isValidAddressOnly("this\\ still\\\"not\\\\allowed@example.com"))
        Assert.assertFalse(validator.isValidAddressOnly("john..doe@example.com"))
        Assert.assertFalse(validator.isValidAddressOnly("invalidperiod.@example.com"))
    }
}
