package com.fsck.k9.autocrypt

import androidx.annotation.VisibleForTesting
import com.fsck.k9.mail.Part
import com.fsck.k9.mail.internet.MimeUtility
import java.util.Collections
import net.thunderbird.core.logging.legacy.Log
import okio.ByteString.Companion.decodeBase64

internal object AutocryptGossipHeaderParser {

    fun getAllAutocryptGossipHeaders(part: Part): List<AutocryptGossipHeader> {
        val headers = part.getHeader(AutocryptGossipHeader.AUTOCRYPT_GOSSIP_HEADER)
        val autocryptHeaders = parseAllAutocryptGossipHeaders(headers)
        return Collections.unmodifiableList(autocryptHeaders)
    }

    @VisibleForTesting
    fun parseAutocryptGossipHeader(headerValue: String): AutocryptGossipHeader? {
        val parameters = MimeUtility.getAllHeaderParameters(headerValue).toMutableMap()

        val type = parameters.remove(AutocryptHeader.AUTOCRYPT_PARAM_TYPE)
        val base64KeyData = parameters.remove(AutocryptHeader.AUTOCRYPT_PARAM_KEY_DATA)
        val addr = parameters.remove(AutocryptHeader.AUTOCRYPT_PARAM_ADDR)
        val decodedKey = base64KeyData?.decodeBase64()

        return when {
            type != null && type != AutocryptHeader.AUTOCRYPT_TYPE_1 -> {
                Log.e("autocrypt: unsupported type parameter %s", type)
                null
            }

            base64KeyData == null -> {
                Log.e("autocrypt: missing key parameter")
                null
            }

            decodedKey == null -> {
                Log.e("autocrypt: error parsing base64 data")
                null
            }

            addr == null -> {
                Log.e("autocrypt: no to header!")
                null
            }

            hasCriticalParameters(parameters) -> {
                null
            }

            else -> {
                AutocryptGossipHeader(addr, decodedKey.toByteArray())
            }
        }
    }

    private fun hasCriticalParameters(parameters: Map<String, String>): Boolean {
        return parameters.keys.any { !it.startsWith("_") }
    }

    private fun parseAllAutocryptGossipHeaders(headers: Array<String>): List<AutocryptGossipHeader> {
        return headers.mapNotNull { header ->
            parseAutocryptGossipHeader(header) ?: run {
                Log.e("Encountered malformed autocrypt-gossip header - skipping!")
                null
            }
        }
    }
}
