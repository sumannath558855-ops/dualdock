package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [ClonedSpace::class],
    version = 1,
    exportSchema = false
)
abstract class DualDockDatabase : RoomDatabase() {
    abstract fun clonedSpaceDao(): ClonedSpaceDao

    companion object {
        @Volatile
        private var INSTANCE: DualDockDatabase? = null

        fun getInstance(context: Context): DualDockDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    DualDockDatabase::class.java,
                    "dualdock_vault.db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
