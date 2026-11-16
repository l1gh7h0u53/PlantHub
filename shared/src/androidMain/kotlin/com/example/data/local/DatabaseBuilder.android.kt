package com.example.data.local

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlin.concurrent.Volatile

@Volatile
private var ANDROID_DB_INSTANCE: AppDatabase? = null

/** Creates a Room KMP database builder backed by Android internal app storage. */
fun getAndroidDatabaseBuilder(context: Context): RoomDatabase.Builder<AppDatabase> {
    val appContext = context.applicationContext
    val dbFile = appContext.getDatabasePath(AppDatabase.DATABASE_NAME)
    return Room.databaseBuilder<AppDatabase>(
        context = appContext,
        name = dbFile.absolutePath
    )
}

/**
 * Thread-safe singleton accessor preserving backward compatibility for Android callers
 * and existing Robolectric database setups.
 */
fun AppDatabase.Companion.getDatabase(context: Context): AppDatabase {
    return ANDROID_DB_INSTANCE ?: synchronized(this) {
        ANDROID_DB_INSTANCE ?: buildFromBuilder(getAndroidDatabaseBuilder(context)).also {
            ANDROID_DB_INSTANCE = it
        }
    }
}
