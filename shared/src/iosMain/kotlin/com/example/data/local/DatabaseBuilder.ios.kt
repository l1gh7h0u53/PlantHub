package com.example.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/** Creates a Room KMP database builder stored inside iOS NSDocumentDirectory. */
@OptIn(ExperimentalForeignApi::class)
fun getIosDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null
    )
    val dbFilePath = requireNotNull(documentDirectory?.path) + "/${AppDatabase.DATABASE_NAME}.db"
    return Room.databaseBuilder<AppDatabase>(
        name = dbFilePath
    )
}

private var IOS_DB_INSTANCE: AppDatabase? = null

/** Builds and caches the iOS [AppDatabase] instance backed by [BundledSQLiteDriver]. */
fun AppDatabase.Companion.getIosDatabase(): AppDatabase {
    return IOS_DB_INSTANCE ?: buildFromBuilder(getIosDatabaseBuilder()).also {
        IOS_DB_INSTANCE = it
    }
}
