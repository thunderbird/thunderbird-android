package net.thunderbird.core.database

/** The physical name of a table owned by a database contribution. */
@JvmInline
value class DatabaseTableName(
    val value: String,
) {
    init {
        require(value.matches(VALID_VALUE)) {
            "Invalid database table name '$value'. Expected a lower-case identifier beginning with a letter " +
                "and containing only letters, digits, or underscores."
        }
    }

    private companion object {
        val VALID_VALUE = Regex("^[a-z][a-z0-9_]*$")
    }
}
