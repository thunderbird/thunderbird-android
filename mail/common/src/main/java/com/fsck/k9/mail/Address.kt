package com.fsck.k9.mail

import com.fsck.k9.mail.helper.Rfc822Tokenizer
import com.fsck.k9.mail.helper.TextUtils.isEmpty
import java.io.Serializable
import java.util.regex.Pattern
import net.thunderbird.core.logging.legacy.Log
import org.apache.james.mime4j.MimeException
import org.apache.james.mime4j.codec.DecodeMonitor
import org.apache.james.mime4j.codec.EncoderUtil
import org.apache.james.mime4j.field.address.DefaultAddressParser
import org.jetbrains.annotations.VisibleForTesting

class Address @JvmOverloads constructor(
    address: String,
    personal: String? = null,
    parse: Boolean = true,
) : Serializable {

    @Suppress("MemberNameEqualsClassName")
    var address: String = address
        private set

    var personal: String? = personal
        private set

    constructor(address: Address) : this(address.address, address.personal)

    init {
        if (parse) {
            val tokens = Rfc822Tokenizer.tokenize(address)
            if (tokens.isNotEmpty()) {
                val token = tokens[0]
                this.address = requireNotNull(token.address) {
                    "token.getAddress()"
                }
                val name = token.name
                this.personal = if (name.isNullOrEmpty()) {
                    /*
                     * Don't use the "personal" argument if "address" is of the form:
                     * James Bond <james.bond@mi6.uk>
                     *
                     * See issue 2920
                     */
                    personal?.trim()
                } else {
                    name
                }
            } else {
                Log.e("Invalid address: %s", address)
            }
        }
    }

    val hostname: String?
        get() {
            val hostIdx = address.lastIndexOf("@")
            return if (hostIdx == -1) null else address.substring(hostIdx + 1)
        }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as Address
        if (address != other.address) return false
        if (personal != other.personal) return false
        return true
    }

    override fun hashCode(): Int {
        var result = address.hashCode()
        result = 31 * result + (personal?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String =
        if (personal.isNullOrBlank()) {
            address
        } else {
            quoteAtoms(personal) + " <$address>"
        }

    fun toEncodedString(): String =
        if (personal.isNullOrBlank()) {
            address
        } else {
            EncoderUtil.encodeAddressDisplayName(personal) + " <$address>"
        }

    /**
     * Returns true if either the localpart or the domain of this
     * address contains any non-ASCII characters, and false if all
     * characters used are within ASCII.
     *
     * Note that this returns false for an address such as "Naïve
     * Assumption &lt;naive.assumption@example.com&gt;", because both
     * localpart and domain are all-ASCII. There's an ï there, but
     * it's not in either localpart or domain.
     */
    fun needsUnicode(): Boolean {
        var i = address.length - 1
        while (i >= 0 && address[i].code < ASCII_LIMIT) i--
        return i >= 0
    }

    companion object {
        private const val serialVersionUID = 1L

        /**
         * Any character with code ≥ 128 is non-ASCII
         */
        private const val ASCII_LIMIT = 128

        private val ATOM: Pattern = Pattern.compile("^(?:[a-zA-Z0-9!#$%&'*+\\-/=?^_`{|}~]|\\s)+$")

        /**
         * Immutable empty [Address] array
         */
        private val EMPTY_ADDRESS_ARRAY = emptyArray<Address>()

        /**
         * Parse a comma separated list of email addresses in human readable format and return an
         * array of Address objects, RFC-822 encoded.
         *
         * @param addressList
         * @return An array of 0 or more Addresses.
         */
        @JvmStatic
        fun parseUnencoded(addressList: String?): Array<Address> {
            if (isEmpty(addressList)) return EMPTY_ADDRESS_ARRAY

            val tokens = Rfc822Tokenizer.tokenize(addressList)
            return tokens
                .filterNot { it.address.isNullOrBlank() }
                .map { Address(requireNotNull(it.address), it.name, false) }
                .toTypedArray()
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
            if (isEmpty(addressList)) return EMPTY_ADDRESS_ARRAY

            return try {
                DefaultAddressParser.DEFAULT
                    .parseAddressList(addressList, DecodeMonitor.SILENT)
                    .flatten()
                    .map { mailbox ->
                        Address(
                            address = "${mailbox.localPart}@${mailbox.domain}",
                            personal = mailbox.name,
                            parse = false,
                        )
                    }
                    .toTypedArray()
            } catch (pe: MimeException) {
                Log.e(pe, "MimeException in Address.parse()")
                // broken addresses are never added to the resulting array
                EMPTY_ADDRESS_ARRAY
            }
        }

        @JvmStatic
        fun toString(addresses: Array<Address>): String =
            if (addresses.isEmpty()) {
                ""
            } else {
                addresses.joinToString(", ")
            }

        /**
         * Unpacks an address list previously packed with packAddressList()
         * @param addressList Packed address list.
         * @return Unpacked list.
         */
        @JvmStatic
        fun unpack(addressList: String?): Array<Address> {
            if (addressList == null) return EMPTY_ADDRESS_ARRAY

            val addresses = mutableListOf<Address>()

            val length = addressList.length
            var pairStartIndex = 0

            while (pairStartIndex < length) {
                var pairEndIndex = addressList.indexOf(",\u0001", pairStartIndex)
                if (pairEndIndex == -1) {
                    pairEndIndex = length
                }
                val addressEndIndex = addressList.indexOf(";\u0001", pairStartIndex)

                var address: String?
                var personal: String? = null

                if (addressEndIndex == -1 || addressEndIndex > pairEndIndex) {
                    address = addressList.substring(pairStartIndex, pairEndIndex)
                } else {
                    address = addressList.substring(pairStartIndex, addressEndIndex)
                    personal = addressList.substring(addressEndIndex + 2, pairEndIndex)
                }
                addresses.add(Address(address, personal, false))
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
        fun pack(addresses: Array<Address>?): String {
            if (addresses == null) return ""

            val sb = StringBuilder()
            var i = 0
            val count = addresses.size
            while (i < count) {
                val address = addresses[i]
                sb.append(address.address)
                var personal = address.personal
                if (personal != null) {
                    sb.append(";\u0001")
                    // Escape quotes in the address part on the way in
                    personal = personal.replace("\"".toRegex(), "\\\"")
                    sb.append(personal)
                }
                if (i < count - 1) {
                    sb.append(",\u0001")
                }
                i++
            }
            return sb.toString()
        }

        /**
         * Quote a string, if necessary, based upon the definition of an "atom," as defined by RFC2822
         * (http://tools.ietf.org/html/rfc2822#section-3.2.4). Strings that consist purely of atoms are
         * left unquoted; anything else is returned as a quoted string.
         * @param text String to quote.
         * @return Possibly quoted string.
         */
        @JvmStatic
        fun quoteAtoms(text: String?): String? =
            text?.let {
                if (ATOM.matcher(it).matches()) {
                    text
                } else {
                    quoteString(it)
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
        fun quoteString(s: String?): String? =
            s?.let {
                if (s.matches("^\".*\"$".toRegex())) {
                    s
                } else {
                    "\"" + s + "\""
                }
            }
    }
}
