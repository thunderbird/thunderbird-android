package net.thunderbird.core.database.internal

import androidx.room3.RoomDatabase
import androidx.room3.migration.Migration
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.Dispatchers

/** Applies the production driver, coroutine context, and coordinated migrations before opening Room. */
class RoomDatabaseBootstrap<T : RoomDatabase> private constructor(
    private val builder: RoomDatabase.Builder<T>,
    private val migrations: List<Migration>,
    private val configuration: Configuration,
) {
    constructor(
        builder: RoomDatabase.Builder<T>,
        migrations: List<Migration>,
        queryCoroutineContext: CoroutineContext = Dispatchers.IO,
    ) : this(
        builder = builder,
        migrations = migrations,
        configuration = Configuration(queryCoroutineContext, BundledSQLiteDriver()),
    )

    internal constructor(
        builder: RoomDatabase.Builder<T>,
        migrations: List<Migration>,
        driver: SQLiteDriver,
        queryCoroutineContext: CoroutineContext = Dispatchers.IO,
    ) : this(
        builder = builder,
        migrations = migrations,
        configuration = Configuration(queryCoroutineContext, driver),
    )

    fun open(): T = builder
        .setDriver(configuration.driver)
        .setQueryCoroutineContext(configuration.queryCoroutineContext)
        .addMigrations(*migrations.toTypedArray())
        .build()

    private data class Configuration(
        val queryCoroutineContext: CoroutineContext,
        val driver: SQLiteDriver,
    )
}
