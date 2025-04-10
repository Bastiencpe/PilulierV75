package com.example.pilulier.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [Medicament::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicamentDao(): MedicamentDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "pilulier-db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
