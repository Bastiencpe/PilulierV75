package com.example.pilulier.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface MedicamentDao {

    @Insert
    fun ajouterMedicament(medicament: Medicament)

    @Query("SELECT * FROM medicaments WHERE nom = :nom")
    fun getMedicamentParNom(nom: String): List<Medicament>

    @Query("SELECT * FROM medicaments WHERE moment = :moment")
    fun getMedicamentParMoment(moment: String): List<Medicament>

    @Query("DELETE FROM medicaments")
    fun supprimerTous()

    @Query("SELECT * FROM medicaments")
    fun getTousLesMedicaments(): List<Medicament>

    @Update
    fun majEtatPrise(medicament: Medicament) // Mise à jour avec un objet Medicament
}
