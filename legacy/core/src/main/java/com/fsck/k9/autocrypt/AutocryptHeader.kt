package com.fsck.k9.autocrypt

import okio.ByteString

data class AutocryptHeader(
    val parameters: Map<String, String>,
    val addr: String,
    val keyData: ByteArray,
    val isPreferEncryptMutual: Boolean,
) {

    fun toRawHeaderString(): String {
        // TODO we don't properly fold lines here. if we want to support parameters, we need to do that somehow
        if (parameters.isNotEmpty()) {
            error("arbitrary parameters not supported")
        }

        return buildString {
            append(AUTOCRYPT_HEADER).append(": ")
            append(AUTOCRYPT_PARAM_ADDR).append('=').append(addr).append("; ")
            if (isPreferEncryptMutual) {
                append(AUTOCRYPT_PARAM_PREFER_ENCRYPT)
                    .append('=').append(AUTOCRYPT_PREFER_ENCRYPT_MUTUAL).append("; ")
            }
            append(AUTOCRYPT_PARAM_KEY_DATA).append("=")
            append(createFoldedBase64KeyData(keyData))
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AutocryptHeader) return false

        return isPreferEncryptMutual == other.isPreferEncryptMutual &&
            keyData.contentEquals(other.keyData) &&
            addr == other.addr &&
            parameters == other.parameters
    }

    override fun hashCode(): Int {
        var result = keyData.contentHashCode()
        result = 31 * result + addr.hashCode()
        result = 31 * result + parameters.hashCode()
        result = 31 * result + if (isPreferEncryptMutual) 1 else 0
        return result
    }

    companion object {
        const val AUTOCRYPT_HEADER = "Autocrypt"

        const val AUTOCRYPT_PARAM_ADDR = "addr"
        const val AUTOCRYPT_PARAM_KEY_DATA = "keydata"

        const val AUTOCRYPT_PARAM_TYPE = "type"
        const val AUTOCRYPT_TYPE_1 = "1"

        const val AUTOCRYPT_PARAM_PREFER_ENCRYPT = "prefer-encrypt"
        const val AUTOCRYPT_PREFER_ENCRYPT_MUTUAL = "mutual"

        private const val HEADER_LINE_LENGTH = 76

        fun createFoldedBase64KeyData(keyData: ByteArray): String {
            val base64KeyData = ByteString.of(data = keyData).base64()
            val result = StringBuilder()

            for (i in base64KeyData.indices step HEADER_LINE_LENGTH) {
                result.append("\r\n ")
                if (i + HEADER_LINE_LENGTH <= base64KeyData.length) {
                    result.append(base64KeyData, i, i + HEADER_LINE_LENGTH)
                } else {
                    result.append(base64KeyData, i, base64KeyData.length)
                }
            }

            return result.toString()
        }
    }
}
