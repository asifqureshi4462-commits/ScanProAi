package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * NOTE on migrations: schema export is now ON (exportSchema = true, see
 * app/schemas/ after building), which is required to write real Migration
 * objects. We don't have the historical v1 schema for this project (it
 * wasn't exported before), so we can't safely hand-write a v1->v2
 * migration without guessing at columns that may not match production
 * data. fallbackToDestructiveMigration() remains as a safety net for that
 * one gap only.
 *
 * From this version forward: any future version bump MUST ship a real
 * Migration(oldVersion, newVersion) added to the .addMigrations(...) call
 * below, using the exported schema JSON as the source of truth — do not
 * rely on destructive fallback going forward, since that silently deletes
 * every user's saved documents and chat history on upgrade.
 */
@Database(entities = [ScannedDocument::class, ChatMessageEntity::class], version = 2, exportSchema = true)
abstract class ScanProDatabase : RoomDatabase() {
    abstract fun documentDao(): DocumentDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: ScanProDatabase? = null

        fun getDatabase(context: Context): ScanProDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ScanProDatabase::class.java,
                    "scanpro_database"
                )
                    // Add real Migration objects here as the schema evolves,
                    // e.g.: .addMigrations(MIGRATION_2_3)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
