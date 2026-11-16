package com.example.data.local

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File
import kotlin.concurrent.Volatile

@Volatile
private var DESKTOP_DB_INSTANCE: AppDatabase? = null

/** Creates a Room KMP database builder backed by the user's local Desktop home directory. */
fun getDesktopDatabaseBuilder(): RoomDatabase.Builder<AppDatabase> {
    val dbDir = File(System.getProperty("user.home"), ".planthub").apply { mkdirs() }
    val dbFile = File(dbDir, "${AppDatabase.DATABASE_NAME}.db")
    return Room.databaseBuilder<AppDatabase>(
        name = dbFile.absolutePath
    )
}

/** Thread-safe singleton accessor for Desktop JVM targets. */
fun AppDatabase.Companion.getDesktopDatabase(): AppDatabase {
    return DESKTOP_DB_INSTANCE ?: synchronized(this) {
        DESKTOP_DB_INSTANCE ?: buildFromBuilder(getDesktopDatabaseBuilder()).also {
            DESKTOP_DB_INSTANCE = it
        }
    }
}
