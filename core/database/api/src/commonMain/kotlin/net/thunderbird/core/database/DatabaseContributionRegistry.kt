package net.thunderbird.core.database

/** Provides the schema contributions selected by application composition. */
interface DatabaseContributionRegistry {
    val contributions: Set<DatabaseContribution>
}
