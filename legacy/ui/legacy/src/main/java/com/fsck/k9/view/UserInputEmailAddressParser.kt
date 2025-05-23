package com.fsck.k9.view

import com.fsck.k9.mail.Address
import net.thunderbird.core.common.net.HostNameUtils
import org.apache.james.mime4j.util.CharsetUtil

/**
 * Used to parse name & email address pairs entered by the user.
 *
 * TODO: Build a custom implementation that can deal with typical inputs from users who are not familiar with the
 *  RFC 5322 address-list syntax. See (ignored) tests in `UserInputEmailAddressParserTest`.
 */
internal class UserInputEmailAddressParser {

    @Throws(NonAsciiEmailAddressException::class)
    fun parse(input: String): List<Address> {
        return Address.parseUnencoded(input)
            .mapNotNull { address ->
                when {
                    address.isIncomplete() -> null
                    address.isNonAsciiAddress() -> throw NonAsciiEmailAddressException(address.address)
                    address.isInvalidDomainPart() -> null
                    else -> Address.parse(address.toEncodedString()).firstOrNull()
                }
            }
    }

    private fun Address.isIncomplete() = hostname.isNullOrBlank()

    private fun Address.isNonAsciiAddress() = !CharsetUtil.isASCII(address)

    private fun Address.isInvalidDomainPart() = HostNameUtils.isLegalHostNameOrIP(hostname) == null
}

internal class NonAsciiEmailAddressException(message: String) : Exception(message)
