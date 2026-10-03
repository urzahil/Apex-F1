package com.example.data.cache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [HistoryClassificationEntity::class], version = 1, exportSchema = false)
abstract class ApexDatabase : RoomDatabase() {
    abstract fun historyClassificationDao(): HistoryClassificationDao

    companion object {
        @Volatile private var instance: ApexDatabase? = null
        fun get(context: Context): ApexDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, ApexDatabase::class.java, "apex-f1.db").build().also { instance = it }
        }
    }
}