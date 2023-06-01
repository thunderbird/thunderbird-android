package app.k9mail.core.common.mail

import app.k9mail.core.common.mail.EmailAddressParserError.DnsLabelLengthExceeded
import app.k9mail.core.common.mail.EmailAddressParserError.DomainLengthExceeded
import app.k9mail.core.common.mail.EmailAddressParserError.ExpectedEndOfInput

// See RFC 1035, 2.3.4.
// For the string representation used in emails (labels separated by dots, no final dot allowed), we end up with a
// maximum of 253 characters.
internal const val MAXIMUM_DOMAIN_LENGTH = 253

// See RFC 1035, 2.3.4.
internal const val MAXIMUM_DNS_LABEL_LENGTH = 63

/**
 * Parser for domain names in email addresses.
 *
 * From RFC 5321:
 * ```
 * Domain                  = sub-domain *("." sub-domain)
 * sub-domain              = Let-dig [Ldh-str]
 * Let-dig                 = ALPHA / DIGIT
 * Ldh-str                 = *( ALPHA / DIGIT / "-" ) Let-dig
 * ```
 */
internal class EmailDomainParser(
    input: String,
    startIndex: Int = 0,
    endIndex: Int = input.length,
) : AbstractParser(input, startIndex, endIndex) {

    fun parseDomain(): EmailDomain {
        val domain = readDomain()

        if (!endReached()) {
            parserError(ExpectedEndOfInput)
        }

        return domain
    }

    fun readDomain(): EmailDomain {
        val domain = readString {
            expectSubDomain()

            while (!endReached() && peek() == DOT) {
                expect(DOT)
                expectSubDomain()
            }
        }

        if (domain.length > MAXIMUM_DOMAIN_LENGTH) {
            parserError(DomainLengthExceeded)
        }

        return EmailDomain(domain)
    }

    private fun expectSubDomain() {
        val startIndex = currentIndex

        expectLetDig()

        var requireLetDig = false
        while (!endReached()) {
            val character = peek()
            when {
                character == HYPHEN -> {
                    requireLetDig = true
                    expect(HYPHEN)
                }
                character.isLetDig -> {
                    requireLetDig = false
                    expectLetDig()
                }
                else -> break
            }
        }

        if (requireLetDig) {
            expectLetDig()
        }

        if (currentIndex - startIndex > MAXIMUM_DNS_LABEL_LENGTH) {
            parserError(DnsLabelLengthExceeded)
        }
    }

    private fun expectLetDig() {
        expect("'Let-dig'") { it.isLetDig }
    }
}
