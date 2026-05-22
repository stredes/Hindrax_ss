package com.hindrax.ss.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.hindrax.ss.data.entity.*

@Database(
    entities = [
        AuditSessionEntity::class,
        AuditResultEntity::class,
        AllowedTargetEntity::class,
        ToolTaskEntity::class,
        TaskEntity::class,
        TaskHistoryEntity::class,
        InventoryEntity::class,
        PeerEntity::class,
        ChatMessageEntity::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class HindraxDatabase : RoomDatabase() {
    abstract fun auditSessionDao(): AuditSessionDao
    abstract fun auditResultDao(): AuditResultDao
    abstract fun allowedTargetDao(): AllowedTargetDao
    abstract fun toolTaskDao(): ToolTaskDao
    abstract fun taskDao(): TaskDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: HindraxDatabase? = null

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE peers ADD COLUMN lastKnownIp TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE tasks ADD COLUMN assignedPeerId TEXT")
                database.execSQL("ALTER TABLE peers ADD COLUMN nickname TEXT")
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE peers ADD COLUMN latitude REAL")
                database.execSQL("ALTER TABLE peers ADD COLUMN longitude REAL")
                database.execSQL("ALTER TABLE peers ADD COLUMN locationAccuracy REAL")
                database.execSQL("ALTER TABLE peers ADD COLUMN locationUpdatedAt INTEGER")
            }
        }

        fun getDatabase(context: Context): HindraxDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    HindraxDatabase::class.java,
                    "hindrax_database"
                )
                .addMigrations(MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8)
                // If there are missing migrations during development, allow destructive migration
                // to avoid crashes. In production prefer providing proper Migration objects.
                .fallbackToDestructiveMigration()
                .fallbackToDestructiveMigrationOnDowngrade()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
