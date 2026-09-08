package net.thunderbird.core.database

/** A positive version in the coordinated database schema history. */
@JvmInline
value class DatabaseVersion(
    val value: Int,
) : Comparable<DatabaseVersion> {
    init {
        require(value > 0) { "Database version must be greater than zero, but was $value." }
    }

    override fun compareTo(other: DatabaseVersion): Int = value.compareTo(other.value)
}
