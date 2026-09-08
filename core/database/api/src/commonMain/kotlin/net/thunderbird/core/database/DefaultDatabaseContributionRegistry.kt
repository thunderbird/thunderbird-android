package net.thunderbird.core.database

/**
 * Validates statically configured schema contributions and exposes them in deterministic order.
 */
class DefaultDatabaseContributionRegistry(
    contributions: Collection<DatabaseContribution>,
) : DatabaseContributionRegistry {
    override val contributions: Set<DatabaseContribution> = contributions.validate().toSet()
}

/** Returns contributions in the deterministic order used by database assembly. */
fun DatabaseContributionRegistry.orderedContributions(): List<DatabaseContribution> =
    contributions.sortedBy { it.id.value }

private fun Collection<DatabaseContribution>.validate(): Collection<DatabaseContribution> {
    val duplicateIds = groupBy { it.id }.filterValues { it.size > 1 }.keys
    require(duplicateIds.isEmpty()) {
        "Duplicate database contribution IDs: ${duplicateIds.sortedBy { it.value }.joinToString { it.value }}."
    }

    forEach { contribution ->
        val foreignOwners = contribution.tables.filter { it.ownerId != contribution.id }
        require(foreignOwners.isEmpty()) {
            "Database contribution '${contribution.id.value}' declares tables owned by another contribution: " +
                foreignOwners.joinToString { "${it.name.value} (${it.ownerId.value})" } + "."
        }

        val duplicateTransitions = contribution.migrations
            .groupBy { it.from to it.to }
            .filterValues { it.size > 1 }
            .keys
        require(duplicateTransitions.isEmpty()) {
            "Database contribution '${contribution.id.value}' declares duplicate migration transitions: " +
                duplicateTransitions.joinToString { (from, to) -> "${from.value}->${to.value}" } + "."
        }
    }

    val duplicateTables = flatMap { it.tables }
        .groupBy { it.name }
        .filterValues { it.size > 1 }
    require(duplicateTables.isEmpty()) {
        "Database tables must have exactly one owner: " + duplicateTables.entries.joinToString { (name, tables) ->
            "${name.value} (${tables.map { it.ownerId.value }.sorted().joinToString()})"
        } + "."
    }

    return this
}
