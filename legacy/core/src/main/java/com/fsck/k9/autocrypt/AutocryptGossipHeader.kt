package com.fsck.k9.autocrypt

internal data class AutocryptGossipHeader(@JvmField val addr: String, @JvmField val keyData: ByteArray) {
    fun toRawHeaderString() = buildString {
        append(AUTOCRYPT_GOSSIP_HEADER).append(": ")
        append(AUTOCRYPT_PARAM_ADDR).append('=').append(addr).append("; ")
        append(AUTOCRYPT_PARAM_KEY_DATA).append('=')
        append(AutocryptHeader.createFoldedBase64KeyData(keyData))
    }

    override fun equals(other: Any?): Boolean =
        other is AutocryptGossipHeader &&
            addr == other.addr &&
            keyData.contentEquals(other.keyData)

    override fun hashCode(): Int =
        31 * keyData.contentHashCode() + addr.hashCode()

    companion object {
        const val AUTOCRYPT_GOSSIP_HEADER: String = "Autocrypt-Gossip"

        private const val AUTOCRYPT_PARAM_ADDR = "addr"
        private const val AUTOCRYPT_PARAM_KEY_DATA = "keydata"
    }
}
