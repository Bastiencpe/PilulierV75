// Ligne nulle
package com.example.pilulier.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Medicament::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicamentDao(): MedicamentDao
}
