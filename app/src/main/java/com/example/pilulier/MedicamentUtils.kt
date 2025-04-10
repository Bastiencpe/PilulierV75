package com.example.pilulier

import java.text.SimpleDateFormat
import java.util.*
import com.example.pilulier.data.AppDatabase
import com.example.pilulier.data.Medicament

fun ajouterMedsInitiales(db: AppDatabase) {
    db.medicamentDao().supprimerTous()
    // Vérifier si les médicaments existent déjà dans la base de données avant de les ajouter
    if (db.medicamentDao().getMedicamentParNom("Doliprane").isEmpty()) {
        db.medicamentDao().ajouterMedicament(
            Medicament(
                nom = "Doliprane",
                moment = "matin",
                dateDebut = "2025-04-10",
                dateFin = "2025-04-17",
                frequence = "quotidien"
            )
        )
    }

    if (db.medicamentDao().getMedicamentParNom("Fer").isEmpty()) {
        db.medicamentDao().ajouterMedicament(
            Medicament(
                nom = "Fer",
                moment = "soir",
                dateDebut = "2025-04-10",
                dateFin = "2025-04-17",
                frequence = "1j sur 2"
            )
        )
    }

    if (db.medicamentDao().getMedicamentParNom("Advil").isEmpty()) {
        db.medicamentDao().ajouterMedicament(
            Medicament(
                nom = "Advil",
                moment = "midi",
                dateDebut = "2025-04-10",
                dateFin = "2025-04-17",
                frequence = "quotidien"
            )
        )
    }
}

fun getMedsPourDate(db: AppDatabase, dateStr: String): List<String> {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
    val date = format.parse(dateStr) ?: return emptyList()

    return db.medicamentDao().getTousLesMedicaments().filter { med ->
        val debut = med.dateDebut?.let { format.parse(it) }
        val fin = med.dateFin?.let { format.parse(it) }
        if (debut == null || fin == null || date.before(debut) || date.after(fin)) return@filter false

        when (med.frequence?.lowercase(Locale.FRANCE)) {
            "quotidien" -> true
            "1j sur 2" -> ((date.time - debut.time) / (1000 * 60 * 60 * 24)) % 2 == 0L
            "hebdomadaire" -> {
                val cal = Calendar.getInstance().apply { time = date }
                cal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY
            }
            else -> false
        }
    }.map { it.nom }
}
