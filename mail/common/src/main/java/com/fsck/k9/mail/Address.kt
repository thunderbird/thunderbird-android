package com.fsck.k9.mail

import com.fsck.k9.mail.helper.Rfc822Tokenizer
import java.io.Serializable
import java.util.regex.Pattern
import net.thunderbird.core.logging.legacy.Log
import org.apache.james.mime4j.MimeException
import org.apache.james.mime4j.codec.DecodeMonitor
import org.apache.james.mime4j.codec.EncoderUtil
import org.apache.james.mime4j.field.address.DefaultAddressParser
import org.jetbrains.annotations.VisibleForTesting

@Suppress("MemberNameEqualsClassName")
class Address : Serializable {
    val address: String
    val personal: String?

    constructor(address: Address) {
        this.address = address.address
        this.personal = address.personal
    }

    @JvmOverloads
    constructor(address: String, personal: String? = null) : this(address, personal, true)

    private constructor(address: String, personal: String?, parse: Boolean) {
        if (parse) {
            val tokens = Rfc822Tokenizer.tokenize(address)
            if (tokens.isNotEmpty()) {
                val token = tokens[0]
                this.address = requireNotNull(token.address) { "token.getAddress()" }
                val name = token.name
                this.personal = if (!name.isNullOrEmpty()) {
                    name
                } else {
                    personal?.trim()
                }
            } else {
                Log.e("Invalid address: %s", address)
                this.address = address
                this.personal = personal
            }
        } else {
            this.address = address
            this.personal = personal
        }
    }

    val hostname: String?
        get() {
            val hostIdx = address.lastIndexOf("@")
            return if (hostIdx == -1) null else address.substring(hostIdx + 1)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Address) return false
        return address == other.address && personal == other.personal
    }

    override fun hashCode(): Int {
        var hash = address.hashCode()
        if (personal != null) {
            hash += 3 * personal.hashCode()
        }
        return hash
    }

    override fun toString(): String {
        return if (!personal.isNullOrEmpty()) {
            quoteAtoms(personal) + " <" + address + ">"
        } else {
            address
        }
    }

    fun toEncodedString(): String {
        return if (!personal.isNullOrEmpty()) {
            EncoderUtil.encodeAddressDisplayName(personal) + " <" + address + ">"
        } else {
            address
        }
    }

    /**
     *  Returns true if either the localpart or the domain of this
     *  address contains any non-ASCII characters, and false if all
     *  characters used are within ASCII.
     *
     *  Note that this returns false for an address such as "Naïve
     *  Assumption <naive.assumption@example.com>", because both
     *  localpart and domain are all-ASCII. There's an ï there, but
     *  it's not in either localpart or domain.
     */
    fun needsUnicode(): Boolean {
        var i = address.length - 1
        while (i >= 0 && address[i].code < ASCII_MAX) {
            i--
        }
        return i >= 0
    }

    companion object {
        private const val serialVersionUID = 1L
        private const val ASCII_MAX = 128
        private val ATOM = Pattern.compile("^(?:[a-zA-Z0-9!#$%&'*+\\-/=?^_`{|}~]|\\s)+$")

        /**
         * Parse a comma separated list of email addresses in human readable format and return an
         * array of Address objects, RFC-822 encoded.
         *
         * @param addressList
         * @return An array of 0 or more Addresses.
         */
        @JvmStatic
        fun parseUnencoded(addressList: String?): Array<Address> {
            if (addressList.isNullOrEmpty()) return emptyArray()

            val tokens = Rfc822Tokenizer.tokenize(addressList)
            val addresses = tokens.mapNotNull { token ->
                val address = token.address
                if (!address.isNullOrEmpty()) {
                    val name = if (token.name.isNullOrEmpty()) null else token.name
                    Address(address, name, false)
                } else {
                    null
                }
            }
            return addresses.toTypedArray()
        }

        /**
         * Parse a comma separated list of addresses in RFC-822 format and return an
         * array of Address objects.
         *
         * @param addressList
         * @return An array of 0 or more Addresses.
         */
        @JvmStatic
        fun parse(addressList: String?): Array<Address> {
            if (addressList.isNullOrEmpty()) {
                return emptyArray()
            }
            val addresses = mutableListOf<Address>()
            try {
                val parsedList = DefaultAddressParser.DEFAULT
                    .parseAddressList(addressList, DecodeMonitor.SILENT).flatten()
                for (i in 0 until parsedList.size) {
                    val mailbox = parsedList[i]
                    addresses.add(Address(mailbox.localPart + "@" + mailbox.domain, mailbox.name, false))
                }
            } catch (pe: MimeException) {
                Log.e(pe, "MimeException in Address.parse()")
                // broken addresses are never added to the resulting array
            }
            return addresses.toTypedArray()
        }

        /**
         * Unpacks an address list previously packed with packAddressList()
         * @param addressList Packed address list.
         * @return Unpacked list.
         */
        @JvmStatic
        fun unpack(addressList: String?): Array<Address> {
            addressList ?: return emptyArray()
            val addresses = mutableListOf<Address>()
            val length = addressList.length
            var pairStartIndex = 0
            var pairEndIndex: Int
            var addressEndIndex: Int
            while (pairStartIndex < length) {
                pairEndIndex = addressList.indexOf(",\u0001", pairStartIndex)
                if (pairEndIndex == -1) {
                    pairEndIndex = length
                }
                addressEndIndex = addressList.indexOf(";\u0001", pairStartIndex)
                val addr: String
                val personal: String?
                if (addressEndIndex == -1 || addressEndIndex > pairEndIndex) {
                    addr = addressList.substring(pairStartIndex, pairEndIndex)
                    personal = null
                } else {
                    addr = addressList.substring(pairStartIndex, addressEndIndex)
                    personal = addressList.substring(addressEndIndex + 2, pairEndIndex)
                }
                addresses.add(Address(addr, personal, false))
                pairStartIndex = pairEndIndex + 2
            }
            return addresses.toTypedArray()
        }

        /**
         * Packs an address list into a String that is very quick to read
         * and parse. Packed lists can be unpacked with unpackAddressList()
         * The packed list is a ",\u0001" separated list of:
         * address;\u0001personal
         * @param addresses Array of addresses to pack.
         * @return Packed addresses.
         */
        @JvmStatic
        fun pack(addresses: Array<Address>): String {
            val sb = StringBuilder()
            for (i in addresses.indices) {
                val address = addresses[i]
                sb.append(address.address)
                val personal = address.personal
                if (personal != null) {
                    sb.append(";\u0001")
                    // Escape quotes in the address part on the way in
                    sb.append(personal.replace("\"", "\\\""))
                }
                if (i < addresses.size - 1) {
                    sb.append(",\u0001")
                }
            }
            return sb.toString()
        }

        @JvmStatic
        fun toString(addresses: Array<Address>): String {
            return addresses.joinToString(", ")
        }

        /**
         * Quote a string, if necessary, based upon the definition of an "atom," as defined by RFC2822
         * (http://tools.ietf.org/html/rfc2822#section-3.2.4). Strings that consist purely of atoms are
         * left unquoted; anything else is returned as a quoted string.
         * @param text String to quote.
         * @return Possibly quoted string.
         */
        @JvmStatic
        fun quoteAtoms(text: String): String {
            return if (ATOM.matcher(text).matches()) {
                text
            } else {
                quoteString(text)
            }
        }

        /**
         * Ensures that the given string starts and ends with the double quote character.
         * The string is not modified in any way except to add the double quote character to start
         * and end if it's not already there.
         * sample -> "sample"
         * "sample" -> "sample"
         * ""sample"" -> ""sample""
         * "sample"" -> "sample"
         * sa"mp"le -> "sa"mp"le"
         * "sa"mp"le" -> "sa"mp"le"
         * (empty string) -> ""
         * " -> """
         * @param s
         * @return
         */
        @JvmStatic
        @VisibleForTesting
        fun quoteString(s: String): String {
            return if (!s.matches(Regex("^\".*\"$"))) {
                "\"" + s + "\""
            } else {
                s
            }
        }
    }
}
