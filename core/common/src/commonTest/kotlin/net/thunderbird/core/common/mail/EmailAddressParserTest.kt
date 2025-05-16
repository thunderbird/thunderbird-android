package net.thunderbird.core.common.mail

import assertk.all
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import kotlin.test.Test
import net.thunderbird.core.common.mail.EmailAddressParserError.AddressLiteralsNotSupported
import net.thunderbird.core.common.mail.EmailAddressParserError.EmptyLocalPart
import net.thunderbird.core.common.mail.EmailAddressParserError.ExpectedEndOfInput
import net.thunderbird.core.common.mail.EmailAddressParserError.InvalidDomainPart
import net.thunderbird.core.common.mail.EmailAddressParserError.InvalidDotString
import net.thunderbird.core.common.mail.EmailAddressParserError.InvalidLocalPart
import net.thunderbird.core.common.mail.EmailAddressParserError.InvalidQuotedString
import net.thunderbird.core.common.mail.EmailAddressParserError.LocalPartLengthExceeded
import net.thunderbird.core.common.mail.EmailAddressParserError.LocalPartRequiresQuotedString
import net.thunderbird.core.common.mail.EmailAddressParserError.QuotedStringInLocalPart
import net.thunderbird.core.common.mail.EmailAddressParserError.TotalLengthExceeded
import net.thunderbird.core.common.mail.EmailAddressParserError.UnexpectedCharacter

class EmailAddressParserTest {
    @Test
    fun `simple address`() {
        val emailAddress = parseEmailAddress("alice@domain.example")

        assertThat(emailAddress.localPart).isEqualTo("alice")
        assertThat(emailAddress.domain).isEqualTo(EmailDomain("domain.example"))
    }

    @Test
    fun `local part containing dot`() {
        val emailAddress = parseEmailAddress("alice.lastname@domain.example")

        assertThat(emailAddress.localPart).isEqualTo("alice.lastname")
        assertThat(emailAddress.domain).isEqualTo(EmailDomain("domain.example"))
    }

    @Test
    fun `quoted local part`() {
        val emailAddress = parseEmailAddress(
            address = "\"one two\"@domain.example",
            isLocalPartRequiringQuotedStringAllowed = true,
        )

        assertThat(emailAddress.localPart).isEqualTo("one two")
        assertThat(emailAddress.domain).isEqualTo(EmailDomain("domain.example"))
    }

    @Test
    fun `quoted local part not allowed`() {
        assertFailure {
            parseEmailAddress(
                address = "\"one two\"@domain.example",
                isQuotedLocalPartAllowed = true,
                isLocalPartRequiringQuotedStringAllowed = false,
            )
        }.isInstanceOf<EmailAddressParserException>().all {
            prop(EmailAddressParserException::error).isEqualTo(LocalPartRequiresQuotedString)
            prop(EmailAddressParserException::position).isEqualTo(0)
            hasMessage("Local part requiring the use of a quoted string is not allowed by config")
        }
    }

    @Test
    fun `unnecessarily quoted local part`() {
        val emailAddress = parseEmailAddress(
            address = "\"user\"@domain.example",
            isQuotedLocalPartAllowed = true,
            isLocalPartRequiringQuotedStringAllowed = false,
        )

        assertThat(emailAddress.localPart).isEqualTo("user")
        assertThat(emailAddress.domain).isEqualTo(EmailDomain("domain.example"))
        assertThat(emailAddress.address).isEqualTo("user@domain.example")
    }

    @Test
    fun `unnecessarily quoted local part not allowed`() {
        assertFailure {
            parseEmailAddress("\"user\"@domain.example", isQuotedLocalPartAllowed = false)
        }.isInstanceOf<EmailAddressParserException>().all {
            prop(EmailAddressParserException::error).isEqualTo(QuotedStringInLocalPart)
            prop(EmailAddressParserException::position).isEqualTo(0)
            hasMessage("Quoted string in local part is not allowed by config")
        }
    }

    @Test
    fun `quoted local part containing double quote character`() {
        val emailAddress = parseEmailAddress(
            address = """"a\"b"@domain.example""",
            isLocalPartRequiringQuotedStringAllowed = true,
        )

        assertThat(emailAddress.localPart).isEqualTo("a\"b")
        assertThat(emailAddress.domain).isEqualTo(EmailDomain("domain.example"))
        assertThat(emailAddress.address).isEqualTo(""""a\"b"@domain.example""")
    }

    @Test
    fun `empty local part`() {
        val emailAddress = parseEmailAddress("\"\"@domain.example", isEmptyLocalPartAllowed = true)

        assertThat(emailAddress.localPart).isEqualTo("")
        assertThat(emailAddress.domain).isEqualTo(EmailDomain("domain.example"))
        assertThat(emailAddress.address).isEqualTo("\"\"@domain.example")
    }

    @Test
    fun `empty local part not allowed`() {
        assertFailure {
            parseEmailAddress(
                address = "\"\"@domain.example",
                isLocalPartRequiringQuotedStringAllowed = true,
                isEmptyLocalPartAllowed = false,
            )
        }.isInstanceOf<EmailAddressParserException>().all {
            prop(EmailAddressParserException::error).isEqualTo(EmptyLocalPart)
            prop(EmailAddressParserException::position).isEqualTo(1)
            hasMessage("Empty local part is not allowed by config")
        }
    }

    @Test
    fun `IPv4 address literal`() {
        assertFailure {
            parseEmailAddress("user@[255.0.100.23]")
        }.isInstanceOf<EmailAddressParserException>().all {
            prop(EmailAddressParserException::error).isEqualTo(AddressLiteralsNotSupported)
            prop(EmailAddressParserException::position).isEqualTo(5)
            hasMessage("Address literals are not supported")
        }
    }

    @Test
    fun `IPv6 address literal`() {
        assertFailure {
            parseEmailAddress("user@[IPv6:2001:0db8:0000:0000:0000:ff00:0042:8329]")
        }.isInstanceOf<EmailAddressParserException>().all {
            prop(EmailAddressParserException::error).isEqualTo(AddressLiteralsNotSupported)
            prop(EmailAddressParserException::position).isEqualTo(5)
            hasMessage("Address literals are not supported")
        }
    }

    @Test
    fun `domain part starts with unsupported value`() {
        assertFailure {
            parseEmailAddress("user@ä")
        }.isInstanceOf<EmailAddressParserException>().all {
            prop(EmailAddressParserException::error).isEqualTo(InvalidDomainPart)
            prop(EmailAddressParserException::position).isEqualTo(5)
            hasMessage("Expected 'Domain' or 'address-literal'")
        }
    }

    @Test
    fun `obsolete syntax`() {
        assertFailure {
            parseEmailAddress("\"quoted\".atom@domain.example", isLocalPartRequiringQuotedStringAllowed = true)
        }.isInstanceOf<EmailAddressParserException>().all {
            prop(EmailAddressParserException::error).isEqualTo(UnexpectedCharacter)
            prop(EmailAddressParserException::position).isEqualTo(8)
            hasMessage("Expected '@' (64)")
        }
    }

    @Test
    fun `local part starting with dot`() {
        assertFailure {
            parseEmailAddress(".invalid@domain.example")
        }.isInstanceOf<EmailAddressParserException>().all {
            prop(EmailAddressParserException::error).isEqualTo(InvalidLocalPart)
            prop(EmailAddressParserException::position).isEqualTo(0)
            hasMessage("Expected 'Dot-string' or 'Quoted-string'")
        }
    }

    @Test
    fun `local part ending with dot`() {
        assertFailure {
            parseEmailAddress("invalid.@domain.example")
        }.isInstanceOf<EmailAddressParserException>().all {
            prop(EmailAddressParserException::error).isEqualTo(InvalidDotString)
            prop(EmailAddressParserException::position).isEqualTo(8)
            hasMessage("Expected 'Dot-string'")
        }
    }

    @Test
    fun `quoted local part missing closing double quote`() {
        assertFailure {
            parseEmailAddress("\"invalid@domain.example", isLocalPartRequiringQuotedStringAllowed = true)
        }.isInstanceOf<EmailAddressParserException>().all {
            prop(EmailAddressParserException::error).isEqualTo(UnexpectedCharacter)
            prop(EmailAddressParserException::position).isEqualTo(23)
            hasMessage("Expected '\"' (34)")
        }
    }

    @Test
    fun `quoted text containing unsupported character`() {
        assertFailure {
            parseEmailAddress("\"ä\"@domain.example", isLocalPartRequiringQuotedStringAllowed = true)
        }.isInstanceOf<EmailAddressParserException>().all {
            prop(EmailAddressParserException::error).isEqualTo(InvalidQuotedString)
            prop(EmailAddressParserException::position).isEqualTo(1)
            hasMessage("Expected 'Quoted-string'")
        }
    }

    @Test
    fun `quoted text containing unsupported escaped character`() {
        assertFailure {
            parseEmailAddress(""""\ä"@domain.example""", isLocalPartRequiringQuotedStringAllowed = true)
        }.isInstanceOf<EmailAddressParserException>().all {
            prop(EmailAddressParserException::error).isEqualTo(InvalidQuotedString)
            prop(EmailAddressParserException::position).isEqualTo(3)
            hasMessage("Expected 'Quoted-string'")
        }
    }

    @Test
    fun `local part exceeds maximum size with length check enabled`() {
        assertFailure {
            parseEmailAddress(
                address = "1xxxxxxxxx2xxxxxxxxx3xxxxxxxxx4xxxxxxxxx5xxxxxxxxx6xxxxxxxxx12345@domain.example",
                isLocalPartLengthCheckEnabled = true,
            )
        }.isInstanceOf<EmailAddressParserException>().all {
            prop(EmailAddressParserException::error).isEqualTo(LocalPartLengthExceeded)
            prop(EmailAddressParserException::position).isEqualTo(65)
            hasMessage("Local part exceeds maximum length of 64 characters")
        }
    }

    @Test
    fun `local part exceeds maximum size with length check disabled`() {
        val input = "1xxxxxxxxx2xxxxxxxxx3xxxxxxxxx4xxxxxxxxx5xxxxxxxxx6xxxxxxxxx12345@domain.example"

        val emailAddress = parseEmailAddress(address = input, isLocalPartLengthCheckEnabled = false)

        assertThat(emailAddress.localPart)
            .isEqualTo("1xxxxxxxxx2xxxxxxxxx3xxxxxxxxx4xxxxxxxxx5xxxxxxxxx6xxxxxxxxx12345")
        assertThat(emailAddress.domain).isEqualTo(EmailDomain("domain.example"))
        assertThat(emailAddress.address).isEqualTo(input)
        assertThat(emailAddress.warnings).contains(EmailAddress.Warning.LocalPartExceedsLengthLimit)
    }

    @Test
    fun `email exceeds maximum size with length check enabled`() {
        assertFailure {
            parseEmailAddress(
                address = "1xxxxxxxxx2xxxxxxxxx3xxxxxxxxx4xxxxxxxxx5xxxxxxxxx6xxxxxxxxx1234@" +
                    "1xxxxxxxxx2xxxxxxxxx3xxxxxxxxx4xxxxxxxxx5xxxxxxxxx6xxxxxxxxx123." +
                    "1xxxxxxxxx2xxxxxxxxx3xxxxxxxxx4xxxxxxxxx5xxxxxxxxx6xxxxxxxxx123." +
                    "1xxxxxxxxx2xxxxxxxxx3xxxxxxxxx4xxxxxxxxx5xxxxxxxxx6xxxxxxxxx12",
                isEmailAddressLengthCheckEnabled = true,
            )
        }.isInstanceOf<EmailAddressParserException>().all {
            prop(EmailAddressParserException::error).isEqualTo(TotalLengthExceeded)
            prop(EmailAddressParserException::position).isEqualTo(255)
            hasMessage("The email address exceeds the maximum length of 254 characters")
        }
    }

    @Test
    fun `email exceeds maximum size with length check disabled`() {
        val input = "1xxxxxxxxx2xxxxxxxxx3xxxxxxxxx4xxxxxxxxx5xxxxxxxxx6xxxxxxxxx1234@" +
            "1xxxxxxxxx2xxxxxxxxx3xxxxxxxxx4xxxxxxxxx5xxxxxxxxx6xxxxxxxxx123." +
            "1xxxxxxxxx2xxxxxxxxx3xxxxxxxxx4xxxxxxxxx5xxxxxxxxx6xxxxxxxxx123." +
            "1xxxxxxxxx2xxxxxxxxx3xxxxxxxxx4xxxxxxxxx5xxxxxxxxx6xxxxxxxxx12"

        val emailAddress = parseEmailAddress(address = input, isEmailAddressLengthCheckEnabled = false)

        assertThat(emailAddress.localPart)
            .isEqualTo("1xxxxxxxxx2xxxxxxxxx3xxxxxxxxx4xxxxxxxxx5xxxxxxxxx6xxxxxxxxx1234")
        assertThat(emailAddress.domain).isEqualTo(
            EmailDomain(
                "1xxxxxxxxx2xxxxxxxxx3xxxxxxxxx4xxxxxxxxx5xxxxxxxxx6xxxxxxxxx123." +
                    "1xxxxxxxxx2xxxxxxxxx3xxxxxxxxx4xxxxxxxxx5xxxxxxxxx6xxxxxxxxx123." +
                    "1xxxxxxxxx2xxxxxxxxx3xxxxxxxxx4xxxxxxxxx5xxxxxxxxx6xxxxxxxxx12",
            ),
        )
        assertThat(emailAddress.address).isEqualTo(input)
        assertThat(emailAddress.warnings).contains(EmailAddress.Warning.EmailAddressExceedsLengthLimit)
    }

    @Test
    fun `input contains additional character`() {
        assertFailure {
            parseEmailAddress("test@domain.example#")
        }.isInstanceOf<EmailAddressParserException>().all {
            prop(EmailAddressParserException::error).isEqualTo(ExpectedEndOfInput)
            prop(EmailAddressParserException::position).isEqualTo(19)
            hasMessage("Expected end of input")
        }
    }

    private fun parseEmailAddress(
        address: String,
        isLocalPartLengthCheckEnabled: Boolean = false,
        isEmailAddressLengthCheckEnabled: Boolean = false,
        isEmptyLocalPartAllowed: Boolean = false,
        isLocalPartRequiringQuotedStringAllowed: Boolean = isEmptyLocalPartAllowed,
        isQuotedLocalPartAllowed: Boolean = isLocalPartRequiringQuotedStringAllowed,
    ): EmailAddress {
        val config = EmailAddressParserConfig(
            isLocalPartLengthCheckEnabled,
            isEmailAddressLengthCheckEnabled,
            isQuotedLocalPartAllowed,
            isLocalPartRequiringQuotedStringAllowed,
            isEmptyLocalPartAllowed,
        )
        return EmailAddressParser(address, config).parse()
    }
}
