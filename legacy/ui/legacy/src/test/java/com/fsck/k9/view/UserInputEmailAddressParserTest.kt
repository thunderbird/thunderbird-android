package com.fsck.k9.view

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEmpty
import assertk.assertions.isInstanceOf
import com.fsck.k9.mail.Address
import kotlin.test.Ignore
import kotlin.test.Test

class UserInputEmailAddressParserTest {
    private val parser = UserInputEmailAddressParser()

    @Test
    fun `plain email address`() {
        val addresses = parser.parse("user@domain.example")

        assertThat(addresses).containsExactly(Address("user@domain.example"))
    }

    @Test
    fun `email address followed by space`() {
        val addresses = parser.parse("user@domain.example ")

        assertThat(addresses).containsExactly(Address("user@domain.example"))
    }

    @Test
    fun `email address in angle brackets`() {
        val addresses = parser.parse("<user@domain.example>")

        assertThat(addresses).containsExactly(Address("user@domain.example"))
    }

    @Test
    fun `simple name and address`() {
        val addresses = parser.parse("Name <user@domain.example>")

        assertThat(addresses).containsExactly(Address("user@domain.example", "Name"))
    }

    @Test
    fun `name with quoted string and address`() {
        val addresses = parser.parse("\"Name\" <user@domain.example>")

        assertThat(addresses).containsExactly(Address("user@domain.example", "Name"))
    }

    @Test
    fun `name with multiple words and address`() {
        val addresses = parser.parse("Firstname Lastname <user@domain.example>")

        assertThat(addresses).containsExactly(Address("user@domain.example", "Firstname Lastname"))
    }

    @Test
    fun `name with non-ASCII characters and address`() {
        val addresses = parser.parse("Käthe Gehäusegröße <user@domain.example>")

        assertThat(addresses).containsExactly(Address("user@domain.example", "Käthe Gehäusegröße"))
    }

    @Test
    fun `address with non-ASCII character in local part`() {
        assertFailure {
            parser.parse("müller@domain.example")
        }.isInstanceOf<NonAsciiEmailAddressException>()
    }

    @Test
    fun `address with non-ASCII character in domain part`() {
        assertFailure {
            parser.parse("user@dömain.example")
        }.isInstanceOf<NonAsciiEmailAddressException>()
    }

    @Test
    fun `multiple addresses separated by comma`() {
        val addresses = parser.parse("one@domain.example,<two@domain.example>")

        assertThat(addresses).containsExactly(
            Address("one@domain.example"),
            Address("two@domain.example"),
        )
    }

    @Test
    @Ignore("Currently not supported")
    fun `multiple addresses separated by space`() {
        val addresses = parser.parse("one@domain.example two@domain.example")

        assertThat(addresses).containsExactly(
            Address("one@domain.example"),
            Address("two@domain.example"),
        )
    }

    @Test
    fun `multiple addresses in angle brackets separated by space`() {
        val addresses = parser.parse("<one@domain.example>, <two@domain.example>")

        assertThat(addresses).containsExactly(
            Address("one@domain.example"),
            Address("two@domain.example"),
        )
    }

    @Test
    fun `incomplete address should not return a result`() {
        val addresses = parser.parse("user")

        assertThat(addresses).isEmpty()
    }

    @Test
    fun `incomplete address ending in @ should not return a result`() {
        val addresses = parser.parse("user@")

        assertThat(addresses).isEmpty()
    }

    @Test
    fun `name and incomplete address should not return a result`() {
        val addresses = parser.parse("Name <user")

        assertThat(addresses).isEmpty()
    }

    @Test
    @Ignore("Currently not supported")
    fun `name followed by address not in angle brackets`() {
        val addresses = parser.parse("Firstname Lastname user@domain.example")

        assertThat(addresses).containsExactly(Address("user@domain.example", "Firstname LastName"))
    }

    @Test
    @Ignore("Currently not supported")
    fun `name containing parenthesis`() {
        val addresses = parser.parse("Firstname (Nickname) Lastname <user@domain.example>")

        assertThat(addresses).containsExactly(Address("user@domain.example", "Firstname (Nickname) LastName"))
    }

    @Test
    @Ignore("Currently not supported")
    fun `name containing double quotes in the middle`() {
        val addresses = parser.parse("Firstname \"Nickname\" Lastname <user@domain.example>")

        assertThat(addresses).containsExactly(Address("user@domain.example", "Firstname \"Nickname\" LastName"))
    }

    // for invalid email addresses
    @Test
    fun `address with invalid ending character`() {
        val addresses = parser.parse("user@domain.example/")

        assertThat(addresses).isEmpty()
    }

    @Test
    fun `address with invalid character in domain part`() {
        val addresses = parser.parse("user@domain/example")

        assertThat(addresses).isEmpty()
    }
}
