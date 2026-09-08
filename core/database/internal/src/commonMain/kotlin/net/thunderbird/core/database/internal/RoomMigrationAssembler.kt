package net.thunderbird.core.database.internal

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import net.thunderbird.core.database.DatabaseContributionRegistry
import net.thunderbird.core.database.orderedContributions

/** Validates and coordinates contribution-owned Room migrations. */
class RoomMigrationAssembler(
    private val contributionRegistry: DatabaseContributionRegistry,
) {
    fun assemble(migrations: Collection<RoomDatabaseMigration>): List<Migration> {
        val contributions = contributionRegistry.orderedContributions()
        val contributionsById = contributions.associateBy { it.id }

        migrations.forEach { registeredMigration ->
            require(registeredMigration.contributionId in contributionsById) {
                "Room migration ${registeredMigration.migration.startVersion}->" +
                    "${registeredMigration.migration.endVersion} has unknown contribution owner " +
                    "'${registeredMigration.contributionId.value}'."
            }
        }

        val registeredByOwnerAndTransition = migrations.groupBy {
            Triple(it.contributionId, it.migration.startVersion, it.migration.endVersion)
        }
        val duplicates = registeredByOwnerAndTransition.filterValues { it.size > 1 }.keys
        require(duplicates.isEmpty()) {
            "Duplicate Room migrations: " + duplicates.joinToString { (owner, from, to) ->
                "${owner.value}:$from->$to"
            } + "."
        }

        val expected = contributions.flatMap { contribution ->
            contribution.migrations.map { migration ->
                Triple(contribution.id, migration.from.value, migration.to.value)
            }
        }.toSet()
        val actual = registeredByOwnerAndTransition.keys
        val missing = expected - actual
        require(missing.isEmpty()) {
            "Missing Room migrations: " + missing.sortedWith(transitionComparator()).joinToString { (owner, from, to) ->
                "${owner.value}:$from->$to"
            } + "."
        }
        val undeclared = actual - expected
        require(undeclared.isEmpty()) {
            "Room migrations without matching contribution metadata: " +
                undeclared.sortedWith(transitionComparator()).joinToString { (owner, from, to) ->
                    "${owner.value}:$from->$to"
                } + "."
        }

        return migrations
            .groupBy { it.migration.startVersion to it.migration.endVersion }
            .entries
            .sortedWith(compareBy({ it.key.first }, { it.key.second }))
            .map { (transition, steps) ->
                CoordinatedRoomMigration(
                    startVersion = transition.first,
                    endVersion = transition.second,
                    steps = steps.sortedBy { it.contributionId.value },
                )
            }
    }
}

private class CoordinatedRoomMigration(
    startVersion: Int,
    endVersion: Int,
    private val steps: List<RoomDatabaseMigration>,
) : Migration(startVersion, endVersion) {
    override suspend fun migrate(connection: SQLiteConnection) {
        steps.forEach { it.migration.migrate(connection) }
    }
}

private fun transitionComparator() = compareBy<Triple<net.thunderbird.core.database.DatabaseContributionId, Int, Int>>(
    { it.second },
    { it.third },
    { it.first.value },
)
