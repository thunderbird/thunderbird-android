package com.fsck.k9.mail.internet

import com.fsck.k9.mail.Address

/**
 * Encode and fold email addresses for use in header values.
 */
object AddressHeaderBuilder {
    @JvmStatic
    fun createHeaderValue(addresses: Array<Address>): String {
        require(addresses.isNotEmpty()) { "addresses must not be empty" }

        return buildString {
            var lineLength = 0
            for ((index, address) in addresses.withIndex()) {
                val encodedAddress = address.toEncodedString()
                val encodedAddressLength = encodedAddress.length

                if (index > 0 && lineLength + 2 + encodedAddressLength + 1 > RECOMMENDED_MAX_LINE_LENGTH) {
                    append(",$CRLF ")
                    append(encodedAddress)
                    lineLength = encodedAddressLength + 1
                } else {
                    if (index > 0) {
                        append(", ")
                        lineLength += 2
                    }

                    append(encodedAddress)
                    lineLength += encodedAddressLength
                }
            }
        }
    }
}
