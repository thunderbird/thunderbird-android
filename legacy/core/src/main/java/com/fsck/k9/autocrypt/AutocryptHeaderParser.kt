package com.fsck.k9.autocrypt

import androidx.annotation.VisibleForTesting
import com.fsck.k9.mail.Message
import com.fsck.k9.mail.internet.MimeUtility
import net.thunderbird.core.logging.legacy.Log
import okio.ByteString.Companion.decodeBase64

internal object AutocryptHeaderParser {

    private const val TAG = "AutocryptHeaderParser"
    fun getValidAutocryptHeader(currentMessage: Message): AutocryptHeader? {
        val headers = currentMessage.getHeader(AutocryptHeader.AUTOCRYPT_HEADER).orEmpty().asList()
        return parseAllAutocryptHeaders(headers).singleOrNull()
    }

    @VisibleForTesting
    fun parseAutocryptHeader(headerValue: String): AutocryptHeader? {
        val parameters = MimeUtility.getAllHeaderParameters(headerValue).toMutableMap()

        val type = parameters.remove(AutocryptHeader.AUTOCRYPT_PARAM_TYPE)
        val base64KeyData = parameters.remove(AutocryptHeader.AUTOCRYPT_PARAM_KEY_DATA)
        val to = parameters.remove(AutocryptHeader.AUTOCRYPT_PARAM_ADDR)
        val preferEncrypt = parameters.remove(AutocryptHeader.AUTOCRYPT_PARAM_PREFER_ENCRYPT)

        val decodedKeyData = base64KeyData?.decodeBase64()

        return when {
            type != null && type != AutocryptHeader.AUTOCRYPT_TYPE_1 -> {
                Log.e(TAG, "Unsupported type parameter: $type")
                null
            }

            base64KeyData == null -> {
                Log.e(TAG, "Missing key parameter")
                null
            }

            decodedKeyData == null -> {
                Log.e(TAG, "Error parsing base64 data")
                null
            }

            to == null -> {
                Log.e(TAG, "No 'to' header found")
                null
            }

            hasCriticalParameters(parameters) -> null

            else -> AutocryptHeader(
                parameters = parameters,
                addr = to,
                keyData = decodedKeyData.toByteArray(),
                isPreferEncryptMutual = AutocryptHeader.AUTOCRYPT_PREFER_ENCRYPT_MUTUAL.equals(
                    preferEncrypt,
                    ignoreCase = true,
                ),
            )
        }
    }

    private fun hasCriticalParameters(parameters: Map<String, String>): Boolean {
        return parameters.keys.any { !it.startsWith("_") }
    }

    private fun parseAllAutocryptHeaders(headers: Collection<String>): List<AutocryptHeader> {
        return headers.mapNotNull(::parseAutocryptHeader)
    }
}
