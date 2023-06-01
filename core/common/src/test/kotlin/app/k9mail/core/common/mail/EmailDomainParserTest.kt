package app.k9mail.core.common.mail

import app.k9mail.core.common.mail.EmailAddressParserError.DnsLabelLengthExceeded
import app.k9mail.core.common.mail.EmailAddressParserError.DomainLengthExceeded
import app.k9mail.core.common.mail.EmailAddressParserError.ExpectedEndOfInput
import app.k9mail.core.common.mail.EmailAddressParserError.UnexpectedCharacter
import assertk.all
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.prop
import kotlin.test.Test

class EmailDomainParserTest {
    @Test
    fun `simple domain`() {
        val emailDomain = parseEmailDomain("DOMAIN.example")

        assertThat(emailDomain.value).isEqualTo("DOMAIN.example")
        assertThat(emailDomain.normalized).isEqualTo("domain.example")
    }

    @Test
    fun `label starting with hyphen`() {
        assertFailure {
            parseEmailDomain("-domain.example")
        }.isInstanceOf<EmailAddressParserException>().all {
            prop(EmailAddressParserException::error).isEqualTo(UnexpectedCharacter)
            prop(EmailAddressParserException::position).isEqualTo(0)
            hasMessage("Expected 'Let-dig'")
        }
    }

    @Test
    fun `label ending with hyphen`() {
        assertFailure {
            parseEmailDomain("domain-.example")
        }.isInstanceOf<EmailAddressParserException>().all {
            prop(EmailAddressParserException::error).isEqualTo(UnexpectedCharacter)
            prop(EmailAddressParserException::position).isEqualTo(7)
            hasMessage("Expected 'Let-dig'")
        }
    }

    @Test
    fun `label exceeds maximum size`() {
        assertFailure {
            parseEmailDomain("1xxxxxxxxx2xxxxxxxxx3xxxxxxxxx4xxxxxxxxx5xxxxxxxxx6xxxxxxxxx1234.example")
        }.isInstanceOf<EmailAddressParserException>().all {
            prop(EmailAddressParserException::error).isEqualTo(DnsLabelLengthExceeded)
            prop(EmailAddressParserException::position).isEqualTo(64)
            hasMessage("DNS labels exceeds maximum length of 63 characters")
        }
    }

    @Test
    fun `domain exceeds maximum size`() {
        assertFailure {
            parseEmailDomain(
                "1xxxxxxxxx2xxxxxxxxx3xxxxxxxxx4xxxxxxxxx5xxxxxxxxx6xxxxxxxxx123." +
                    "1xxxxxxxxx2xxxxxxxxx3xxxxxxxxx4xxxxxxxxx5xxxxxxxxx6xxxxxxxxx123." +
                    "1xxxxxxxxx2xxxxxxxxx3xxxxxxxxx4xxxxxxxxx5xxxxxxxxx6xxxxxxxxx123." +
                    "1xxxxxxxxx2xxxxxxxxx3xxxxxxxxx4xxxxxxxxx5xxxxxxxxx6xxxxxxxxx12",
            )
        }.isInstanceOf<EmailAddressParserException>().all {
            prop(EmailAddressParserException::error).isEqualTo(DomainLengthExceeded)
            prop(EmailAddressParserException::position).isEqualTo(254)
            hasMessage("Domain exceeds maximum length of 253 characters")
        }
    }

    @Test
    fun `input contains additional character`() {
        assertFailure {
            parseEmailDomain("domain.example#")
        }.isInstanceOf<EmailAddressParserException>().all {
            prop(EmailAddressParserException::error).isEqualTo(ExpectedEndOfInput)
            prop(EmailAddressParserException::position).isEqualTo(14)
            hasMessage("Expected end of input")
        }
    }

    private fun parseEmailDomain(domain: String): EmailDomain {
        return EmailDomainParser(domain).parseDomain()
    }
}
