package net.thunderbird.core.database.internal

import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.database.DefaultDatabaseContributionRegistry

class RoomDatabaseBootstrapJvmTest {
    @Test
    fun `rejects database path that points to a directory`() {
        val directory = Files.createTempDirectory("core-database-invalid-").toFile()

        try {
            assertFailure {
                createRoomDatabaseBuilder<TestRoomDatabase>(directory)
            }.transform { it.message.orEmpty() }.contains("points to a directory")
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `opens migrates and reopens file-backed database`() = runTest {
        val directory = Files.createTempDirectory("core-database-jvm-").toFile()
        val databaseFile = File(directory, "nested/test.db")
        val migrations = RoomMigrationAssembler(
            DefaultDatabaseContributionRegistry(listOf(TestDatabaseContribution)),
        ).assemble(listOf(TestRoomMigration1To2))

        try {
            databaseFile.parentFile.mkdirs()
            BundledSQLiteDriver().open(databaseFile.absolutePath).use { connection ->
                connection.createVersionOneTestDatabase()
            }

            val migratedDatabase = openDatabase(databaseFile, migrations)
            try {
                assertThat(migratedDatabase.recordDao().getNote(1)).isEqualTo("")
                migratedDatabase.recordDao().insert(TestRecord(2, "after migration", "persisted"))
            } finally {
                migratedDatabase.close()
            }

            val reopenedDatabase = openDatabase(databaseFile, migrations)
            try {
                assertThat(reopenedDatabase.recordDao().count()).isEqualTo(2)
                assertThat(reopenedDatabase.recordDao().getNote(2)).isEqualTo("persisted")
            } finally {
                reopenedDatabase.close()
            }
        } finally {
            directory.deleteRecursively()
        }
    }
}

private fun openDatabase(
    databaseFile: File,
    migrations: List<androidx.room3.migration.Migration>,
): TestRoomDatabase = RoomDatabaseBootstrap(
    builder = createRoomDatabaseBuilder<TestRoomDatabase>(databaseFile),
    migrations = migrations,
).open()
