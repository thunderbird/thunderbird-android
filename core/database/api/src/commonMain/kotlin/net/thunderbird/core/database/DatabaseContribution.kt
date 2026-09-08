package net.thunderbird.core.database

/**
 * Domain-neutral metadata for a schema contribution enabled by application composition.
 *
 * Contributions are statically wired. This contract does not provide runtime plugin loading.
 */
interface DatabaseContribution {
    val id: DatabaseContributionId
    val tables: Set<DatabaseTable>
    val migrations: Set<DatabaseMigration>
}
