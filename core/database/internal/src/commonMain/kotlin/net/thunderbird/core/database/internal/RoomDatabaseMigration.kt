package net.thunderbird.core.database.internal

import androidx.room3.migration.Migration
import net.thunderbird.core.database.DatabaseContributionId

/** Associates a Room migration implementation with its schema contribution. */
data class RoomDatabaseMigration(
    val contributionId: DatabaseContributionId,
    val migration: Migration,
)
