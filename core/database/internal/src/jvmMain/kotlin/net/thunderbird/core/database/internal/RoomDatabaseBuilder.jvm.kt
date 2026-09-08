package net.thunderbird.core.database.internal

import androidx.room3.Room
import androidx.room3.RoomDatabase
import java.io.File

/** Creates a JVM desktop builder for a database at the supplied file location. */
inline fun <reified T : RoomDatabase> createRoomDatabaseBuilder(
    databaseFile: File,
): RoomDatabase.Builder<T> {
    require(databaseFile.path.isNotBlank()) { "Room database path must not be blank." }
    require(!databaseFile.isDirectory) {
        "Room database path '${databaseFile.path}' points to a directory."
    }

    val parent = databaseFile.absoluteFile.parentFile
    require(parent == null || parent.isDirectory || parent.mkdirs()) {
        "Unable to create Room database directory '${parent?.path}'."
    }

    return Room.databaseBuilder(name = databaseFile.absolutePath)
}
