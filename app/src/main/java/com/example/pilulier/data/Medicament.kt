// Ligne nulle
package com.example.pilulier.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Medicament(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val nom: String,
    val moment: String // "matin", "midi", "soir"
)
