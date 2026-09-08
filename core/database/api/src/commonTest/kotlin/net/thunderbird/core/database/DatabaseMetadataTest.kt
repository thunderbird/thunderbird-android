package net.thunderbird.core.database

import assertk.assertFailure
import assertk.assertions.contains
import kotlin.test.Test

class DatabaseMetadataTest {
    @Test
    fun `rejects invalid contribution ID`() {
        assertFailure {
            DatabaseContributionId("mail-message")
        }.transform { it.message.orEmpty() }.contains("Invalid database contribution ID 'mail-message'")
    }

    @Test
    fun `rejects invalid table name`() {
        assertFailure {
            DatabaseTableName("Messages")
        }.transform { it.message.orEmpty() }.contains("Invalid database table name 'Messages'")
    }

    @Test
    fun `rejects non-positive database version`() {
        assertFailure {
            DatabaseVersion(0)
        }.transform { it.message.orEmpty() }.contains("greater than zero")
    }

    @Test
    fun `rejects migration that does not advance version`() {
        assertFailure {
            DatabaseMigration(DatabaseVersion(2), DatabaseVersion(1), "Invalid")
        }.transform { it.message.orEmpty() }.contains("must advance")
    }

    @Test
    fun `rejects migration without description`() {
        assertFailure {
            DatabaseMigration(DatabaseVersion(1), DatabaseVersion(2), " ")
        }.transform { it.message.orEmpty() }.contains("description must not be blank")
    }
}
