package net.thunderbird.core.database.internal

import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.database.DatabaseContribution
import net.thunderbird.core.database.DatabaseContributionId
import net.thunderbird.core.database.DatabaseMigration
import net.thunderbird.core.database.DatabaseTable
import net.thunderbird.core.database.DatabaseTableName
import net.thunderbird.core.database.DatabaseVersion
import net.thunderbird.core.database.DefaultDatabaseContributionRegistry

class RoomMigrationAssemblerTest {
    @Test
    fun `combines migration steps in deterministic contribution order`() = runTest {
        val executionOrder = mutableListOf<String>()
        val account = contribution("account", "accounts")
        val message = contribution("message", "messages")
        val testSubject = RoomMigrationAssembler(DefaultDatabaseContributionRegistry(listOf(message, account)))
        val migrations = listOf(
            roomMigration(message.id, executionOrder),
            roomMigration(account.id, executionOrder),
        )

        val result = testSubject.assemble(migrations)
        BundledSQLiteDriver().open(":memory:").use { connection ->
            result.single().migrate(connection)
        }

        assertThat(result.single().startVersion).isEqualTo(1)
        assertThat(result.single().endVersion).isEqualTo(2)
        assertThat(executionOrder).containsExactly("account", "message")
    }

    @Test
    fun `rejects a missing migration implementation`() {
        val contribution = contribution("message", "messages")
        val testSubject = RoomMigrationAssembler(DefaultDatabaseContributionRegistry(listOf(contribution)))

        assertFailure {
            testSubject.assemble(emptyList())
        }.transform { it.message.orEmpty() }.contains("Missing Room migrations: message:1->2")
    }

    @Test
    fun `rejects migration owned by an unknown contribution`() {
        val contribution = contribution("message", "messages")
        val testSubject = RoomMigrationAssembler(DefaultDatabaseContributionRegistry(listOf(contribution)))

        assertFailure {
            testSubject.assemble(listOf(roomMigration(DatabaseContributionId("unknown"), mutableListOf())))
        }.transform { it.message.orEmpty() }.contains("unknown contribution owner 'unknown'")
    }
}

private fun contribution(id: String, table: String): DatabaseContribution = object : DatabaseContribution {
    override val id = DatabaseContributionId(id)
    override val tables = setOf(DatabaseTable(this.id, DatabaseTableName(table)))
    override val migrations = setOf(
        DatabaseMigration(
            from = DatabaseVersion(1),
            to = DatabaseVersion(2),
            description = "Migrate $table",
        ),
    )
}

private fun roomMigration(
    contributionId: DatabaseContributionId,
    executionOrder: MutableList<String>,
): RoomDatabaseMigration = RoomDatabaseMigration(
    contributionId = contributionId,
    migration = object : Migration(1, 2) {
        override suspend fun migrate(connection: SQLiteConnection) {
            executionOrder += contributionId.value
        }
    },
)
