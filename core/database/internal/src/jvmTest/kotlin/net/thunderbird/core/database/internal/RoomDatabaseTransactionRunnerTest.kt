package net.thunderbird.core.database.internal

import androidx.room3.Room
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class RoomDatabaseTransactionRunnerTest {
    @Test
    fun `commits successful transaction`() = runTest {
        val database = RoomDatabaseBootstrap(
            builder = Room.inMemoryDatabaseBuilder<TestRoomDatabase>(),
            migrations = emptyList(),
        ).open()
        val testSubject = createRoomDatabaseTransactionRunner(database)

        try {
            testSubject.transaction {
                database.recordDao().insert(TestRecord(1, "value", "note"))
            }

            assertThat(database.recordDao().count()).isEqualTo(1)
        } finally {
            database.close()
        }
    }

    @Test
    fun `rolls back failed transaction`() = runTest {
        val database = RoomDatabaseBootstrap(
            builder = Room.inMemoryDatabaseBuilder<TestRoomDatabase>(),
            migrations = emptyList(),
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
