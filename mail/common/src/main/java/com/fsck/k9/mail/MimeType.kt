package com.fsck.k9.mail

class MimeType private constructor(
    val type: String,
    val subtype: String,
) {
    override fun toString(): String {
        return "$type/$subtype"
    }

    override fun equals(other: Any?): Boolean {
        return other is MimeType && type == other.type && subtype == other.subtype
    }

    override fun hashCode(): Int {
        return toString().hashCode()
    }

    companion object {
        private const val TOKEN = "([a-zA-Z0-9-!#$%&'*+.^_`{|}~]+)"
        private val MIME_TYPE = Regex("$TOKEN/$TOKEN")

        @JvmStatic
        @JvmName("parse")
        fun String.toMimeType(): MimeType {
            val matchResult = requireNotNull(MIME_TYPE.matchEntire(this)) { "Invalid MIME type: $this" }

            val type = matchResult.groupValues[1].lowercase()
            val subtype = matchResult.groupValues[2].lowercase()

            return MimeType(type, subtype)
        }

        @JvmStatic
        @JvmName("parseOrNull")
        fun String?.toMimeTypeOrNull(): MimeType? {
            return try {
                this?.toMimeType()
            } catch (e: IllegalArgumentException) {
                null
            }
        }
    }
}
