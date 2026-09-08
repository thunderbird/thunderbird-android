package net.thunderbird.core.database

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactly
import kotlin.test.Test

class DefaultDatabaseContributionRegistryTest {
    @Test
    fun `orders contributions deterministically`() {
        val testSubject = DefaultDatabaseContributionRegistry(
            listOf(
                contribution(id = "message", tableName = DatabaseTableName("messages")),
                contribution(id = "account", tableName = DatabaseTableName("accounts")),
                contribution(id = "folder", tableName = DatabaseTableName("folders")),
            ),
        )

        val result = testSubject.orderedContributions()

        assertThat(result.map { it.id.value }).containsExactly("account", "folder", "message")
    }

    @Test
    fun `rejects duplicate contribution IDs with actionable message`() {
        assertFailure {
            DefaultDatabaseContributionRegistry(
                listOf(contribution(id = "message"), contribution(id = "message")),
            )
        }.transform { it.message.orEmpty() }.contains("Duplicate database contribution IDs: message")
    }

    @Test
    fun `rejects a table declared by multiple contributions`() {
        val tableName = DatabaseTableName("messages")

        assertFailure {
            DefaultDatabaseContributionRegistry(
                listOf(
                    contribution(id = "message", tableName = tableName),
                    contribution(id = "search", tableName = tableName),
                ),
            )
        }.transform { it.message.orEmpty() }.contains("messages (message, search)")
    }

    @Test
    fun `rejects a table whose owner differs from its contribution`() {
        assertFailure {
            DefaultDatabaseContributionRegistry(
                listOf(
                    contribution(
                        id = "message",
                        tableOwnerId = DatabaseContributionId("folder"),
                    ),
                ),
            )
        }.transform { it.message.orEmpty() }.contains("messages (folder)")
    }

    @Test
    fun `rejects duplicate migration transitions from one contribution`() {
        val versionOne = DatabaseVersion(1)
        val versionTwo = DatabaseVersion(2)

        assertFailure {
            DefaultDatabaseContributionRegistry(
                listOf(
                    contribution(
                        id = "message",
                        migrations = setOf(
                            DatabaseMigration(versionOne, versionTwo, "Add messages"),
                            DatabaseMigration(versionOne, versionTwo, "Add message index"),
                        ),
                    ),
                ),
            )
        }.transform { it.message.orEmpty() }.contains("message")
    }
}

private fun contribution(
    id: String,
    tableName: DatabaseTableName = DatabaseTableName("messages"),
    tableOwnerId: DatabaseContributionId = DatabaseContributionId(id),
    migrations: Set<DatabaseMigration> = emptySet(),
): DatabaseContribution = object : DatabaseContribution {
    override val id = DatabaseContributionId(id)
    override val tables = setOf(DatabaseTable(tableOwnerId, tableName))
    override val migrations = migrations
}
