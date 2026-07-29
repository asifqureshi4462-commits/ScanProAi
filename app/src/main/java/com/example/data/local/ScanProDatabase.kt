package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ScannedDocument::class, ChatMessageEntity::class], version = 2, exportSchema = false)
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
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
