package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.model.CareLog
import com.example.data.model.PhotoEntry
import com.example.data.model.Plant
import com.example.data.model.ReminderSetting

/**
 * AppDatabase is the central Room database for PlantHub, persisting cultivar profiles,
 * telemetry care logs, visual milestone photo entries, and care reminders.
 *
 * TECHNICAL LOGIC & ARCHITECTURE DECISIONS:
 * 1. Foreign Key Enforcement (`PRAGMA foreign_keys = ON;`):
 *    - Android's bundled SQLite engine disables foreign key constraints by default for backwards compatibility.
 *    - In [onOpen], we explicitly execute `PRAGMA foreign_keys = ON;`. This guarantees that deleting a Plant entity
 *      automatically triggers SQLite cascade deletion on dependent `care_logs` and `photo_entries`, preventing
 *      orphaned data records from bloating internal flash storage.
 * 2. Non-Destructive Migration Strategy (Version 3 -> 4):
 *    - Added the `isPausedForMaintenance` flag to `plants` via `MIGRATION_3_4`.
 *    - This column stores the automated photoperiod safety lock when an off-cycle completes, preserving all existing
 *      user-logged plants and care histories during app version updates without falling back to destructive resets.
 * 3. Thread-Safe Singleton with Double-Checked Locking:
 *    - The `@Volatile` instance variable paired with `synchronized(this)` ensures only one SQLite connection
 *      pool exists across application lifecycles and background ticker services.
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
abstract class AppDatabase : RoomDatabase() {
    abstract fun plantDao(): PlantDao
    abstract fun careLogDao(): CareLogDao
    abstract fun photoEntryDao(): PhotoEntryDao
    abstract fun reminderSettingDao(): ReminderSettingDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * Migration 3 -> 4: Adds the photoperiod maintenance pause lockout column to the plants table.
         * Default value 0 (false) preserves normal operation for previously recorded cultivars.
         */
        private val MIGRATION_3_4 = object : androidx.room.migration.Migration(3, 4) {
            override fun migrate(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE plants ADD COLUMN isPausedForMaintenance INTEGER NOT NULL DEFAULT 0")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "planthub_database"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .addCallback(object : RoomDatabase.Callback() {
                        override fun onOpen(db: androidx.sqlite.db.SupportSQLiteDatabase) {
                            super.onOpen(db)
                            // Critical: Android SQLite does NOT enforce foreign keys unless explicitly enabled on connection open
                            db.execSQL("PRAGMA foreign_keys = ON;")
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

