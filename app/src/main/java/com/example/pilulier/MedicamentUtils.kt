package com.example.pilulier

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
