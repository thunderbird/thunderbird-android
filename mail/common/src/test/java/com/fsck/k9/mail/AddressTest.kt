package com.fsck.k9.mail

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import com.fsck.k9.mail.Address.Companion.parse
import com.fsck.k9.mail.Address.Companion.quoteString
import net.thunderbird.core.logging.legacy.Log
import net.thunderbird.core.logging.testing.TestLogger
import org.junit.Before
import org.junit.Test

class AddressTest {

    @Before
    fun setUp() {
        Log.logger = TestLogger()
    }

    /**
     * test the possibility to parse "From:" fields with no email.
     * for example: From: News for Vector Limited - Google Finance
     * http://code.google.com/p/k9mail/issues/detail?id=3814
     */
    @Test
    @Throws(Exception::class)
    fun `parse with Missing Email should return empty addresses`() {
        // Arrange
        val addressList = "NAME ONLY"

        // Act
        val addresses = parse(addressList)

        // Assert
        assertThat(addresses.size).isEqualTo(0)
    }

    @Test
    @Throws(Exception::class)
    fun `hostname when address without is provided without domain should return null`() {
        // Arrange
        val nameOnly = "Max"
        val domain = "domain.com"
        val address = "$nameOnly@$domain"

        // Act
        val withoutAddress = Address(nameOnly)
        val withAddress = Address(address = address, personal = nameOnly)

        // Assert
        assertThat(withoutAddress.hostname).isNull()
        assertThat(withAddress.hostname).isEqualTo(domain)
    }

    @Test
    fun `toString without missing addresses returns empty string`() {
        // Arrange
        val emptyAddressList = emptyArray<Address>()
        val actualAccounts = parse("prettyandsimple@example.com")

        // Act
        val emptyString = Address.toString(emptyAddressList)
        val addresses = Address.toString(actualAccounts)

        // Assert
        assertThat(emptyString).isEqualTo("")
        assertThat(addresses).isNotEqualTo("")
    }

    @Test
    fun `pack with missing addresses returns empty string`() {
        // Arrange
        val addressList1 = parse("cc1@domain.example, cc2@domain.example")
        val addressList2 = parse("Bob <bob@domain.example>, Max <Max@domain.example>")
        val addressList3 = parse("NAME ONLY")

        // Act
        val result1 = Address.pack(addressList1)
        val result2 = Address.pack(addressList2)
        val result3 = Address.pack(addressList3)

        // Assert
        assertThat(result1).isNotEqualTo("")
        assertThat(result2).isNotEqualTo("")
        assertThat(result3).isEqualTo("")
    }

    /**
     * test name + valid email
     */
    @Test
    fun `parse valid Email and Personal should set both`() {
        // Arrange
        val address = "Max Mustermann <maxmuster@mann.com>"

        // Act
        val addresses = parse(address)

        // Assert
        assertThat(addresses.size).isEqualTo(1)
        assertThat(addresses[0].address).isEqualTo("maxmuster@mann.com")
        assertThat(addresses[0].personal).isEqualTo("Max Mustermann")
    }

    @Test
    fun `parse with unusual Emails should set address`() {
        // Arrange
        val testEmails = arrayOf(
            "prettyandsimple@example.com",
            "very.common@example.com",
            "disposable.style.email.with+symbol@example.com",
            "other.email-with-dash@example.com",
            // TODO: Handle addresses with quotes
            /*
            "\"much.more unusual\"@example.com",
            "\"very.unusual.@.unusual.com\"@example.com",
            //"very.(),:;<>[]\".VERY.\"very@\\ \"very\".unusual"@strange.example.com
            "\"very.(),:;<>[]\\\".VERY.\\\"very@\\\\ \\\"very\\\".unusual\"@strange.example.com",
            "\"()<>[]:,;@\\\\\\\"!#$%&'*+-/=?^_`{}| ~.a\"@example.org",
            "\" \"@example.org",
             */
            "admin@mailserver1",
            "#!$%&'*+-/=?^_`{}|~@example.org",
            "example@localhost",
            "example@s.solutions",
            "user@com",
            "user@localserver",
            "user@[IPv6:2001:db8::1]",
        )

        testEmails.forEach { mail ->
            // Act
            val addresses = parse("Anonymous <$mail>")

            // Assert
            assertThat(addresses.size).isEqualTo(1)
            assertThat(addresses[0].address).isEqualTo(mail)
        }
    }

    @Test
    fun `parse with encoded Personal should Decode`() {
        // Arrange
        val address = "=?UTF-8?B?WWFob28h44OA44Kk44Os44Kv44OI44Kq44OV44Kh44O8?= <directoffer-master@mail.yahoo.co.jp>"

        // Act
        val addresses = parse(address)

        // Assert
        assertThat(addresses[0].personal).isEqualTo("Yahoo!ダイレクトオファー")
        assertThat(addresses[0].address).isEqualTo("directoffer-master@mail.yahoo.co.jp")
    }

    @Test
    @Suppress("ktlint:standard:max-line-length", "MaxLineLength")
    fun `parse with Quoted encoded Personal should decode`() {
        // Arrange
        val address = "\"=?UTF-8?B?WWFob28h44OA44Kk44Os44Kv44OI44Kq44OV44Kh44O8?=\"<directoffer-master@mail.yahoo.co.jp>"

        // Act
        val addresses = parse(address)

        // Assert
        assertThat(addresses[0].personal).isEqualTo("Yahoo!ダイレクトオファー")
        assertThat(addresses[0].address).isEqualTo("directoffer-master@mail.yahoo.co.jp")
    }

    /**
     * test with multi email addresses
     */
    @Test
    fun `parse with multiple Emails should decode both`() {
        // Arrange
        val listOfAddresses = "lorem@ipsum.us,mark@twain.com"

        // Act
        val addresses = parse(listOfAddresses)

        // Assert
        assertThat(addresses.size).isEqualTo(2)

        assertThat(addresses[0].address).isEqualTo("lorem@ipsum.us")
        assertThat(addresses[0].personal).isEqualTo(null)

        assertThat(addresses[1].address).isEqualTo("mark@twain.com")
        assertThat(addresses[1].personal).isEqualTo(null)
    }

    @Test
    fun `string quotation should correctly Quote`() {
        // Arrange
        val testStrings = listOf(
            "sample" to "\"sample\"",
            "\"\"sample\"\"" to "\"\"sample\"\"",
            "\"sample\"" to "\"sample\"",
            "sa\"mp\"le" to "\"sa\"mp\"le\"",
            "\"sa\"mp\"le\"" to "\"sa\"mp\"le\"",
            "\"" to "\"\"\"",
        )

        testStrings.forEach { (input, expected) ->
            // Act
            val result = quoteString(input)

            // Assert
            assertThat(result).isEqualTo(expected)
        }
    }

    @Test
    fun `hashCode without Address`() {
        // Arrange
        val address = "name only"

        // Act
        val addresses = parse(address)

        // Assert
        assertThat(addresses.size).isEqualTo(0)
    }

    @Test
    fun `hashCode without Personal`() {
        // Arrange
        val address = "alice@example.org"

        // Act
        val addressList = parse(address)

        // Assert
        assertThat(addressList[0].personal).isNull()
        addressList.hashCode()
    }

    @Test
    fun `equals without Personal matches same`() {
        // Arrange
        val address = parse("alice@example.org")[0]
        val address2: Address = parse("alice@example.org")[0]

        // Act
        val result = address.equals(address2)

        // Assert
        assertThat(address.personal).isNull()
        assertThat(result).isTrue()
    }

    @Test
    fun `equals without Personal does not match with address`() {
        // Arrange
        val address = parse("alice@example.org")[0]
        val address2: Address = parse("Alice <alice@example.org>")[0]

        // Act
        val result = address.equals(address2)

        // Assert
        assertThat(result).isFalse()
    }

    @Test
    fun `handles invalid Base64Encoding`() {
        // Arrange
        val base64Encoding = "=?utf-8?b?invalid#?= <oops@example.com>"

        // Act
        val address = parse(base64Encoding)[0]

        // Assert
        assertThat(address.address).isEqualTo("oops@example.com")
    }
}
