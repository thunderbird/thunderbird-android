package net.thunderbird.core.database.internal

import androidx.room3.ConstructedBy
import androidx.room3.Dao
import androidx.room3.Database
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.RoomDatabase
import androidx.room3.RoomDatabaseConstructor
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteConnection
import net.thunderbird.core.database.DatabaseContribution
import net.thunderbird.core.database.DatabaseContributionId
import net.thunderbird.core.database.DatabaseMigration
import net.thunderbird.core.database.DatabaseTable
import net.thunderbird.core.database.DatabaseTableName
import net.thunderbird.core.database.DatabaseVersion

@Entity(tableName = "test_records")
data class TestRecord(
    @PrimaryKey val id: Long,
    val value: String,
    val note: String,
)

@Dao
interface TestRecordDao {
    @Insert
    suspend fun insert(record: TestRecord)

    @Query("SELECT COUNT(*) FROM test_records")
    suspend fun count(): Int

    @Query("SELECT note FROM test_records WHERE id = :id")
    suspend fun getNote(id: Long): String
}

@Database(
    entities = [TestRecord::class],
    version = 2,
    exportSchema = true,
)
@ConstructedBy(TestRoomDatabaseConstructor::class)
abstract class TestRoomDatabase : RoomDatabase() {
    abstract fun recordDao(): TestRecordDao
}

@Suppress("KotlinNoActualForExpect")
expect object TestRoomDatabaseConstructor : RoomDatabaseConstructor<TestRoomDatabase> {
    override fun initialize(): TestRoomDatabase
}

val TestDatabaseContributionId = DatabaseContributionId("test")

val TestDatabaseContribution = object : DatabaseContribution {
    override val id = TestDatabaseContributionId
    override val tables = setOf(DatabaseTable(id, DatabaseTableName("test_records")))
    override val migrations = setOf(
        DatabaseMigration(DatabaseVersion(1), DatabaseVersion(2), "Add test record note"),
    )
}

val TestRoomMigration1To2 = RoomDatabaseMigration(
    contributionId = TestDatabaseContributionId,
    migration = object : Migration(1, 2) {
        override suspend fun migrate(connection: SQLiteConnection) {
            connection.prepare(
                "ALTER TABLE test_records ADD COLUMN note TEXT NOT NULL DEFAULT ''",
            ).use { it.step() }
        }
    },
)

fun SQLiteConnection.createVersionOneTestDatabase() {
    prepare(
        "CREATE TABLE test_records (id INTEGER NOT NULL, value TEXT NOT NULL, PRIMARY KEY(id))",
    ).use { it.step() }
    prepare("INSERT INTO test_records(id, value) VALUES (1, 'before restart')").use { it.step() }
    prepare("PRAGMA user_version = 1").use { it.step() }
}
