package app.k9mail.core.common.mail

import app.k9mail.core.common.net.Domain

/**
 * The domain part of an email address.
 *
 * @param value String representation of the email domain with the original capitalization.
 */
class EmailDomain internal constructor(val value: String) {
    /**
     * The normalized (converted to lower case) string representation of this email domain.
     */
    val normalized: String = value.lowercase()

    /**
     * Returns this email domain with the original capitalization.
     *
     * @see value
     */
    override fun toString(): String = value

    /**
     * Compares the normalized string representations of two [EmailDomain] instances.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as EmailDomain

        return normalized == other.normalized
    }

    override fun hashCode(): Int {
        return normalized.hashCode()
    }

    companion object {
        /**
         * Parses the string representation of an email domain.
         *
         * @throws EmailAddressParserException in case of an error.
         */
        fun parse(domain: String): EmailDomain {
            return EmailDomainParser(domain).parseDomain()
        }
    }
}

fun EmailDomain.toDomain() = Domain(value)
