package com.example.nallanudi.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.nallanudi.database.dao.TechnicalTermDao
import com.example.nallanudi.database.entity.TechnicalTerm

@Database(
    entities = [TechnicalTerm::class],
    version = 1,
    exportSchema = false
)
abstract class NallaNudiDatabase : RoomDatabase() {
    
    abstract fun technicalTermDao(): TechnicalTermDao
    
    companion object {
        @Volatile
        private var INSTANCE: NallaNudiDatabase? = null
        
        fun getDatabase(context: Context): NallaNudiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NallaNudiDatabase::class.java,
                    "nalla_nudi_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// Author: E Thrinadh Chowdary
