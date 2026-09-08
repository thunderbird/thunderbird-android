package net.thunderbird.core.database

/** Runs work atomically in the application's coordinated database. */
interface DatabaseTransactionRunner {
    suspend fun <T> transaction(block: suspend () -> T): T
}
