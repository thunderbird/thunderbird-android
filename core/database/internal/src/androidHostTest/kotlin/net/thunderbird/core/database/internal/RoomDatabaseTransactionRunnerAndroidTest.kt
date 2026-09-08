package net.thunderbird.core.database.internal

import androidx.room3.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class RoomDatabaseTransactionRunnerAndroidTest {
    @Test
    fun `rolls back failed transaction`() = runTest {
        val database = RoomDatabaseBootstrap(
            builder = Room.inMemoryDatabaseBuilder<TestRoomDatabase>(),
            migrations = emptyList(),
            driver = AndroidSQLiteDriver(),
        ).open()
        val testSubject = createRoomDatabaseTransactionRunner(database)

        try {
            assertFailure {
                testSubject.transaction {
                    database.recordDao().insert(TestRecord(1, "value", "note"))
                    error("Stop transaction")
                }
            }

            assertThat(database.recordDao().count()).isEqualTo(0)
        } finally {
            database.close()
        }
    }
}
