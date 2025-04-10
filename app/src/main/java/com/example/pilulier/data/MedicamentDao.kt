package com.example.pilulier.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface MedicamentDao {

    @Query("SELECT * FROM Medicament WHERE moment = :moment AND date = :date")
    fun getMedicamentParMomentEtDate(moment: String, date: String): List<Medicament>

    @Insert
    fun ajouterMedicament(medicament: Medicament)

    @Query("UPDATE Medicament SET pris = :etat WHERE id = :id")
    fun majEtatPrise(id: Int, etat: Boolean)

    @Query("DELETE FROM Medicament")
    fun supprimerTous()
}
