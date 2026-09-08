package net.thunderbird.core.database.internal

import androidx.room3.RoomDatabase
import net.thunderbird.core.database.DatabaseTransactionRunner

fun createRoomDatabaseTransactionRunner(database: RoomDatabase): DatabaseTransactionRunner =
    RoomDatabaseTransactionRunner(database)
