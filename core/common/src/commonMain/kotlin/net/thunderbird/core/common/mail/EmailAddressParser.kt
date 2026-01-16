package net.thunderbird.core.common.mail

import net.thunderbird.core.common.mail.EmailAddress.Warning
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

/**
 * Parse an email address.
 *
 * This class currently doesn't support internationalized domain names (RFC 5891) or non-ASCII local parts (RFC 6532).
 *
 * From RFC 5321:
 * ```
 * Mailbox                 = Local-part "@" ( Domain / address-literal )
 *
 * Local-part              = Dot-string / Quoted-string
 * Dot-string              = Atom *("."  Atom)
 * Quoted-string           = DQUOTE *QcontentSMTP DQUOTE
 * QcontentSMTP            = qtextSMTP / quoted-pairSMTP
 * qtextSMTP               = %d32-33 / %d35-91 / %d93-126
 * quoted-pairSMTP         = %d92 %d32-126
 *
 * Domain                  - see DomainParser
 * address-literal         - We intentionally don't support address literals
 * ```
 */
internal class EmailAddressParser(
    input: String,
    private val config: EmailAddressParserConfig,
) : BaseParser(input) {

    fun parse(): EmailAddress {
        val emailAddress = readEmailAddress()

        if (!endReached()) {
            parserError(ExpectedEndOfInput)
        }

        if (
            config.isEmailAddressLengthCheckEnabled && Warning.EmailAddressExceedsLengthLimit in emailAddress.warnings
        ) {
            parserError(TotalLengthExceeded)
        }

        if (config.isLocalPartLengthCheckEnabled && Warning.LocalPartExceedsLengthLimit in emailAddress.warnings) {
            parserError(LocalPartLengthExceeded, position = input.lastIndexOf('@'))
        }

        if (
            !config.isLocalPartRequiringQuotedStringAllowed && Warning.QuotedStringInLocalPart in emailAddress.warnings
        ) {
            parserError(LocalPartRequiresQuotedString, position = 0)
        }

        if (!config.isEmptyLocalPartAllowed && Warning.EmptyLocalPart in emailAddress.warnings) {
            parserError(EmptyLocalPart, position = 1)
        }

        return emailAddress
    }

    private fun readEmailAddress(): EmailAddress {
        val localPart = readLocalPart()

        expect(AT)
        val domain = readDomainPart()

        return EmailAddress(localPart, domain)
    }

    private fun readLocalPart(): String {
        val character = peek()
        val localPart = when {
            character.isAtext -> {
                readDotString()
            }
            character == DQUOTE -> {
                if (config.isQuotedLocalPartAllowed) {
                    readQuotedString()
                } else {
                    parserError(QuotedStringInLocalPart)
                }
            }
            else -> {
                parserError(InvalidLocalPart)
            }
        }

        return localPart
    }

    private fun readDotString(): String {
        return buildString {
            appendAtom()

            while (!endReached() && peek() == DOT) {
                expect(DOT)
                append(DOT)
                appendAtom()
            }
        }
    }

    private fun StringBuilder.appendAtom() {
        val startIndex = currentIndex
        skipWhile { it.isAtext }

        if (startIndex == currentIndex) {
            parserError(InvalidDotString)
        }

        append(input, startIndex, currentIndex)
    }

    private fun readQuotedString(): String {
        return buildString {
            expect(DQUOTE)

            while (!endReached()) {
                val character = peek()
                when {
                    character.isQtext -> append(read())
                    character == BACKSLASH -> {
                        expect(BACKSLASH)
                        val escapedCharacter = read()
                        if (!escapedCharacter.isQuotedChar) {
                            parserError(InvalidQuotedString)
                        }
                        append(escapedCharacter)
                    }

                    character == DQUOTE -> break
                    else -> parserError(InvalidQuotedString)
                }
            }

            expect(DQUOTE)
        }
    }

    private fun readDomainPart(): EmailDomain {
        val character = peek()
        return when {
            character.isLetDig -> readDomain()
            character == '[' -> parserError(AddressLiteralsNotSupported)
            else -> parserError(InvalidDomainPart)
        }
    }

    private fun readDomain(): EmailDomain {
        return withParser(EmailDomainParser(input, currentIndex)) {
            readDomain()
        }
    }
}
