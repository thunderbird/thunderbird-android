package net.thunderbird.core.database.internal

import android.content.Context
import androidx.room3.Room
import androidx.room3.RoomDatabase

/** Creates an Android builder for a database in the application's private database directory. */
inline fun <reified T : RoomDatabase> createRoomDatabaseBuilder(
    context: Context,
    name: String,
): RoomDatabase.Builder<T> {
    require(name.isNotBlank()) { "Room database name must not be blank." }
    require('/' !in name && '\\' !in name) {
        "Room database name '$name' must be a file name, not a path."
    }

    return Room.databaseBuilder(
        context = context.applicationContext,
        name = name,
    )
}
