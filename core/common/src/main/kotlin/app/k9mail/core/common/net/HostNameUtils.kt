package app.k9mail.core.common.net

/**
 * Code to check the validity of host names and IP addresses.
 *
 * Based on
 * [mailnews/base/src/hostnameUtils.jsm](https://searchfox.org/comm-central/source/mailnews/base/src/hostnameUtils.jsm)
 *
 * Note: The naming of these functions is inconsistent with the rest of the Android project to match the original
 * source. Please use more appropriate names when refactoring this code.
 */
@Suppress("MagicNumber", "ReturnCount")
object HostNameUtils {
    /**
     * Check if `hostName` is an IP address or a valid hostname.
     *
     * @return Unobscured host name if `hostName` is valid.
     */
    fun isLegalHostNameOrIP(hostName: String): String? {
        /*
         RFC 1123:
         Whenever a user inputs the identity of an Internet host, it SHOULD
         be possible to enter either (1) a host domain name or (2) an IP
         address in dotted-decimal ("#.#.#.#") form.  The host SHOULD check
         the string syntactically for a dotted-decimal number before
         looking it up in the Domain Name System.
         */

        return isLegalIPAddress(hostName) ?: isLegalHostName(hostName)
    }

    /**
     * Check if `hostName` is a valid IP address (IPv4 or IPv6).
     *
     * @return Unobscured canonicalized IPv4 or IPv6 address if it is valid, otherwise `null`.
     */
    fun isLegalIPAddress(hostName: String): String? {
        return isLegalIPv4Address(hostName) ?: isLegalIPv6Address(hostName)
    }

    /**
     * Check if `hostName` is a valid IPv4 address.
     *
     * @return Unobscured canonicalized address if `hostName` is an IPv4 address. Returns `null` if it's not.
     */
    fun isLegalIPv4Address(hostName: String): String? {
        // Break the IP address down into individual components.
        val ipComponentStrings = hostName.split(".")
        if (ipComponentStrings.size != 4) {
            return null
        }

        val ipComponents = ipComponentStrings.map { toIPv4NumericComponent(it) }

        if (ipComponents.any { it == null }) {
            return null
        }

        // First component of zero is not valid.
        if (ipComponents.first() == 0) {
            return null
        }

        return hostName
    }

    /**
     * Converts an IPv4 address component to a number if it is valid. Returns `null` otherwise.
     */
    private fun toIPv4NumericComponent(value: String): Int? {
        return if (IPV4_COMPONENT_PATTERN.matches(value)) {
            value.toInt(radix = 10).takeIf { it in 0..255 }
        } else {
            null
        }
    }

    /**
     * Check if `hostName` is a valid IPv6 address.
     *
     * @returns Unobscured canonicalized address if `hostName` is an IPv6 address. Returns `null` if it's not.
     */
    fun isLegalIPv6Address(hostName: String): String? {
        // Break the IP address down into individual components.
        val ipComponentStrings = hostName.lowercase().split(":")

        // Make sure there are at least 3 components.
        if (ipComponentStrings.size < 3) {
            return null
        }

        // Take care if the last part is written in decimal using dots as separators.
        val lastPart = isLegalIPv4Address(ipComponentStrings.last())
        val ipComponentHexStrings = if (lastPart != null) {
            val lastPartComponents = lastPart.split(".").map { it.toInt(radix = 10) }
            // Convert it into standard IPv6 components.
            val part1 = ((lastPartComponents[0] shl 8) or lastPartComponents[1]).toString(radix = 16)
            val part2 = ((lastPartComponents[2] shl 8) or lastPartComponents[3]).toString(radix = 16)

            ipComponentStrings.subList(0, ipComponentStrings.lastIndex) + part1 + part2
        } else {
            ipComponentStrings
        }

        // Make sure that there is only one empty component.
        var emptyIndex = -1
        for (index in 1 until ipComponentHexStrings.lastIndex) {
            if (ipComponentHexStrings[index] == "") {
                // If we already found an empty component return null.
                if (emptyIndex != -1) {
                    return null
                }

                emptyIndex = index
            }
        }

        // If we found an empty component, extend it.
        val fullIpComponentStrings = if (emptyIndex != -1) {
            buildList(capacity = 8) {
                for (i in 0 until emptyIndex) {
                    add(ipComponentHexStrings[i])
                }

                repeat(8 - ipComponentHexStrings.size + 1) {
                    add("0")
                }

                for (i in (emptyIndex + 1)..ipComponentHexStrings.lastIndex) {
                    add(ipComponentHexStrings[i])
                }
            }
        } else {
            ipComponentHexStrings
        }

        // Make sure there are 8 components.
        if (fullIpComponentStrings.size != 8) {
            return null
        }

        // Format all components to 4 character hex value.
        val ipComponents = fullIpComponentStrings.map { ipComponentString ->
            if (ipComponentString == "") {
                0
            } else if (IPV6_COMPONENT_PATTERN.matches(ipComponentString)) {
                ipComponentString.toInt(radix = 16)
            } else {
                return null
            }
        }

        // Treat 0000:0000:0000:0000:0000:0000:0000:0000 as an invalid IPv6 address.
        if (ipComponents.all { it == 0 }) {
            return null
        }

        // Pad the component with 0:s.
        val canonicalIpComponents = ipComponents.map { it.toString(radix = 16).padStart(4, '0') }

        // TODO: support Zone indices in Link-local addresses? Currently they are rejected.
        // http://en.wikipedia.org/wiki/IPv6_address#Link-local_addresses_and_zone_indices

        return canonicalIpComponents.joinToString(":")
    }

    /**
     * Check if `hostName` is a valid hostname.
     *
     * @returns The host name if it is valid. Returns `null` if it's not.
     */
    fun isLegalHostName(hostName: String): String? {
        /*
         RFC 952:
         A "name" (Net, Host, Gateway, or Domain name) is a text string up
         to 24 characters drawn from the alphabet (A-Z), digits (0-9), minus
         sign (-), and period (.).  Note that periods are only allowed when
         they serve to delimit components of "domain style names". (See
         RFC-921, "Domain Name System Implementation Schedule", for
         background).  No blank or space characters are permitted as part of a
         name. No distinction is made between upper and lower case.  The first
         character must be an alpha character.  The last character must not be
         a minus sign or period.

         RFC 1123:
         The syntax of a legal Internet host name was specified in RFC-952
         [DNS:4].  One aspect of host name syntax is hereby changed: the
         restriction on the first character is relaxed to allow either a
         letter or a digit.  Host software MUST support this more liberal
         syntax.

         Host software MUST handle host names of up to 63 characters and
         SHOULD handle host names of up to 255 characters.

         RFC 1034:
         Relative names are either taken relative to a well known origin, or to a
         list of domains used as a search list.  Relative names appear mostly at
         the user interface, where their interpretation varies from
         implementation to implementation, and in master files, where they are
         relative to a single origin domain name.  The most common interpretation
         uses the root "." as either the single origin or as one of the members
         of the search list, so a multi-label relative name is often one where
         the trailing dot has been omitted to save typing.

         Since a complete domain name ends with the root label, this leads to
         a printed form which ends in a dot.
         */

        return hostName.takeIf { hostName.length <= 255 && HOST_PATTERN.matches(hostName) }
    }

    /**
     * Clean up the hostname or IP. Usually used to sanitize a value input by the user.
     * It is usually applied before we know if the hostname is even valid.
     */
    fun cleanUpHostName(hostName: String): String {
        return hostName.trim()
    }

    private const val LDH_LABEL = "([a-z0-9]|[a-z0-9][a-z0-9\\-]{0,61}[a-z0-9])"
    private val HOST_PATTERN = """($LDH_LABEL\.)*$LDH_LABEL\.?""".toRegex(RegexOption.IGNORE_CASE)

    private val IPV4_COMPONENT_PATTERN = "(0|([1-9][0-9]{0,2}))".toRegex()
    private val IPV6_COMPONENT_PATTERN = "[0-9a-f]{1,4}".toRegex()
}
