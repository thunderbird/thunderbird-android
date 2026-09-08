package net.thunderbird.core.database

/** Describes a contribution-owned step in the coordinated schema migration history. */
data class DatabaseMigration(
    val from: DatabaseVersion,
    val to: DatabaseVersion,
    val description: String,
) {
    init {
        require(to > from) {
            "Database migration must advance the schema version, but was ${from.value} to ${to.value}."
        }
        require(description.isNotBlank()) { "Database migration description must not be blank." }
    }
}
