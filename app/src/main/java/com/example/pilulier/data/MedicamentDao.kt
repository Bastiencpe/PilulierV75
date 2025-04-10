// Ligne nulle
package com.example.pilulier.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
@Dao
interface MedicamentDao {
    @Query("SELECT * FROM Medicament WHERE moment = :moment")
    fun getMedicamentParMoment(moment: String): List<Medicament>

    @Insert
    fun ajouterMedicament(medicament: Medicament)

    @Query("DELETE FROM Medicament")
    fun supprimerTous()
}
