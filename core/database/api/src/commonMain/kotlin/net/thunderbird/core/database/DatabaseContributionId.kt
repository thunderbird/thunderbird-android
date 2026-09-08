package net.thunderbird.core.database

/**
 * Identifies a statically configured database schema contribution.
 *
 * This is an infrastructure identifier. It must not contain domain record identity.
 */
@JvmInline
value class DatabaseContributionId(
    val value: String,
) {
    init {
        require(value.matches(VALID_VALUE)) {
            "Invalid database contribution ID '$value'. Expected a lower-case identifier beginning with a letter " +
                "and containing only letters, digits, or underscores."
        }
    }

    private companion object {
        val VALID_VALUE = Regex("^[a-z][a-z0-9_]*$")
    }
}
