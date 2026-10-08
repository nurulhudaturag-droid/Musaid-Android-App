package qiubzen.musaid.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [RoutineItem::class, ChecklistItem::class, CompletionRecord::class, Goal::class, GoalStep::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun appDao(): AppDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * v2 → v3: unique (dateEpochDay, itemType, itemId) on completion_records.
         * Existing duplicate rows (double-tap race) are deduplicated first — keep the
         * newest row (MAX(id)) — otherwise the unique index creation would fail.
         */
        private val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "DELETE FROM completion_records WHERE id NOT IN " +
                        "(SELECT MAX(id) FROM completion_records GROUP BY dateEpochDay, itemType, itemId)"
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_completion_records_date_type_item` " +
                        "ON `completion_records` (`dateEpochDay`, `itemType`, `itemId`)"
                )
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                // Re-check inside the lock: callers on Dispatchers.IO (receivers) and the
                // main thread can both observe a null INSTANCE concurrently, and a second
                // build would create a competing Room instance with its own InvalidationTracker.
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "daily_routine_db"
                )
                .addMigrations(MIGRATION_2_3)
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
