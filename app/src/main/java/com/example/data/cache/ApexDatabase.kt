package com.example.data.cache

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ApiCacheEntity::class],
    version = 1,
    exportSchema = false
)
abstract class ApexDatabase : RoomDatabase() {
    abstract fun apiCacheDao(): ApiCacheDao

    companion object {
        @Volatile private var INSTANCE: ApexDatabase? = null

        fun getInstance(context: Context): ApexDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    ApexDatabase::class.java,
                    "apex_f1.db"
                ).fallbackToDestructiveMigration().build().also { INSTANCE = it }
            }
    }
}
