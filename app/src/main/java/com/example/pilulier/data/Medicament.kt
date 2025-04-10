package com.example.pilulier.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicaments")
data class Medicament(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val nom: String,
    val moment: String,
    val dateDebut: String?,
    val dateFin: String?,
    val frequence: String?,
    val pris: Boolean = false // État par défaut (non pris)
)


