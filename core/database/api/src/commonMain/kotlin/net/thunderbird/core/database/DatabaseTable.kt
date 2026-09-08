package net.thunderbird.core.database

/** Declares which contribution owns a physical database table. */
data class DatabaseTable(
    val ownerId: DatabaseContributionId,
    val name: DatabaseTableName,
)
