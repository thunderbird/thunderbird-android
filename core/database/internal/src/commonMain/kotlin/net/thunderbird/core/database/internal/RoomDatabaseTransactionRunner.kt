package net.thunderbird.core.database.internal

import androidx.room3.RoomDatabase
import androidx.room3.withWriteTransaction
import net.thunderbird.core.database.DatabaseTransactionRunner

internal class RoomDatabaseTransactionRunner(
    private val database: RoomDatabase,
) : DatabaseTransactionRunner {
    override suspend fun <T> transaction(block: suspend () -> T): T = database.withWriteTransaction {
        block()
    }
}
