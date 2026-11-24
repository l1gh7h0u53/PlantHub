package com.example.data.local

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.RoomDatabaseConstructor
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.example.data.model.CareLog
import com.example.data.model.PhotoEntry
import com.example.data.model.Plant
import com.example.data.model.ReminderSetting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO

/**
 * Room KMP compiler-generated constructor bridge across Android, Desktop JVM, and iOS.
 */
@Suppress("KotlinNoActualForExpect")
expect object AppDatabaseConstructor : RoomDatabaseConstructor<AppDatabase> {
    override fun initialize(): AppDatabase
}

/**
 * AppDatabase is the central Room KMP database for PlantHub, persisting cultivar profiles,
 * telemetry care logs, visual milestone photo entries, and care reminders across Android, Desktop, and iOS.
 *
 * TECHNICAL LOGIC & ARCHITECTURE DECISIONS:
 * 1. Foreign Key Enforcement (`PRAGMA foreign_keys = ON;`):
 *    - Executed on every database connection open via [SQLiteConnection.execSQL], ensuring cascade
 *      deletion on `care_logs` and `photo_entries` whenever a parent Plant is deleted.
 * 2. Non-Destructive Migration Strategy (Version 3 -> 4):
 *    - Adds `isPausedForMaintenance` cleanly via multiplatform [Migration] using [SQLiteConnection].
 * 3. Bundled SQLite Driver:
 *    - [BundledSQLiteDriver] guarantees identical SQLite engine behavior across Android, Desktop JVM, and iOS.
 */
@Database(
    entities = [
        Plant::class,
        CareLog::class,
        PhotoEntry::class,
        ReminderSetting::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
@ConstructedBy(AppDatabaseConstructor::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun careLogDao(): CareLogDao
    abstract fun photoEntryDao(): PhotoEntryDao
    abstract fun reminderSettingDao(): ReminderSettingDao

    companion object {
        const val DATABASE_NAME = "planthub_database"

        /**
         * Migration 3 -> 4: Adds the photoperiod maintenance pause lockout column to the plants table.
         */
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("ALTER TABLE plants ADD COLUMN isPausedForMaintenance INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Configures a platform-supplied [RoomDatabase.Builder] with migrations, foreign-key pragma,
         * coroutine dispatcher, and the cross-platform [BundledSQLiteDriver].
         */
        fun buildFromBuilder(builder: Builder<AppDatabase>): AppDatabase {
            return builder
                .addMigrations(MIGRATION_3_4)
                .fallbackToDestructiveMigration(dropAllTables = true)
                .addCallback(object : Callback() {
                    override fun onOpen(connection: SQLiteConnection) {
                        super.onOpen(connection)
                        connection.execSQL("PRAGMA foreign_keys = ON;")
                        // Performance: WAL allows concurrent UI reads during background writes;
                        // synchronous=NORMAL and MEMORY temp_store eliminate unnecessary fsync stalls.
                        connection.execSQL("PRAGMA journal_mode = WAL;")
                        connection.execSQL("PRAGMA synchronous = NORMAL;")
                        connection.execSQL("PRAGMA temp_store = MEMORY;")
                    }
                })
                .setDriver(BundledSQLiteDriver())
                .setQueryCoroutineContext(Dispatchers.IO)
                .build()
        }
    }
}
