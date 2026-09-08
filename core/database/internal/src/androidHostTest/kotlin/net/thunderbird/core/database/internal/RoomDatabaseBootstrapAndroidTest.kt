package net.thunderbird.core.database.internal

import android.content.Context
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import net.thunderbird.core.database.DefaultDatabaseContributionRegistry
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomDatabaseBootstrapAndroidTest {
    @Test
    fun `rejects database name containing a path`() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        assertFailure {
            createRoomDatabaseBuilder<TestRoomDatabase>(context, "directory/test.db")
        }.transform { it.message.orEmpty() }.contains("must be a file name")
    }

    @Test
    fun `opens migrates and reopens file-backed database`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val databaseName = "core-database-android-test.db"
        val databaseFile = context.getDatabasePath(databaseName)
        val driver = AndroidSQLiteDriver()
        val migrations = RoomMigrationAssembler(
            DefaultDatabaseContributionRegistry(listOf(TestDatabaseContribution)),
        ).assemble(listOf(TestRoomMigration1To2))
        context.deleteDatabase(databaseName)

        try {
            databaseFile.parentFile?.mkdirs()
            driver.open(databaseFile.absolutePath).use { connection ->
                connection.createVersionOneTestDatabase()
            }

            val migratedDatabase = openDatabase(context, databaseName, migrations, driver)
            try {
                assertThat(migratedDatabase.recordDao().getNote(1)).isEqualTo("")
                migratedDatabase.recordDao().insert(TestRecord(2, "after migration", "persisted"))
            } finally {
                migratedDatabase.close()
            }

            val reopenedDatabase = openDatabase(context, databaseName, migrations, driver)
            try {
                assertThat(reopenedDatabase.recordDao().count()).isEqualTo(2)
                assertThat(reopenedDatabase.recordDao().getNote(2)).isEqualTo("persisted")
            } finally {
                reopenedDatabase.close()
            }
        } finally {
            context.deleteDatabase(databaseName)
        }
    }
}

private fun openDatabase(
    context: Context,
    databaseName: String,
    migrations: List<androidx.room3.migration.Migration>,
    driver: AndroidSQLiteDriver,
): TestRoomDatabase = RoomDatabaseBootstrap(
    builder = createRoomDatabaseBuilder<TestRoomDatabase>(context, databaseName),
    migrations = migrations,
    driver = driver,
).open()
