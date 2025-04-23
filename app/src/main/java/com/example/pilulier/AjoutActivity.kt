package com.example.pilulier

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.example.pilulier.data.AppDatabase
import com.example.pilulier.data.Medicament

class AjoutActivity : AppCompatActivity() {

    private lateinit var nomEditText: EditText
    private lateinit var momentSpinner: Spinner
    private lateinit var debutEditText: EditText
    private lateinit var finEditText: EditText
    private lateinit var frequenceSpinner: Spinner
    private lateinit var btnEnregistrer: Button
    private lateinit var btnRetour: ImageButton
    private lateinit var btnCamera: Button

    private lateinit var db: AppDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ajout)

        // Liaison des vues
        nomEditText = findViewById(R.id.editNom)
        momentSpinner = findViewById(R.id.spinnerMoment)
        debutEditText = findViewById(R.id.editDateDebut)
        finEditText = findViewById(R.id.editDateFin)
        frequenceSpinner = findViewById(R.id.spinnerFrequence)
        btnEnregistrer = findViewById(R.id.btnEnregistrer)
        btnRetour = findViewById(R.id.btnBackAjout)
        btnCamera = findViewById(R.id.btnCameraAjout)

        // Initialisation BDD
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "pilulier-db"
        ).fallbackToDestructiveMigration().allowMainThreadQueries().build()

        // Adapter pour les moments
        momentSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("matin", "midi", "soir")
        )

        // Adapter pour les fréquences
        frequenceSpinner.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            listOf("quotidien", "1j sur 2", "hebdomadaire")
        )

        // Enregistrement du médicament
        btnEnregistrer.setOnClickListener {
            val nom = nomEditText.text.toString().trim()
            val moment = momentSpinner.selectedItem.toString()
            val debut = debutEditText.text.toString().trim()
            val fin = finEditText.text.toString().trim()
            val frequence = frequenceSpinner.selectedItem.toString()

            if (nom.isNotEmpty() && debut.isNotEmpty() && fin.isNotEmpty()) {
                val nouveauMed = Medicament(
                    nom = nom,
                    moment = moment,
                    dateDebut = debut,
                    dateFin = fin,
                    frequence = frequence,
                    pris = false
                )

                db.medicamentDao().ajouterMedicament(nouveauMed)
                Toast.makeText(this, "Médicament ajouté !", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Veuillez remplir tous les champs.", Toast.LENGTH_SHORT).show()
            }
        }

        // Retour
        btnRetour.setOnClickListener {
            finish()
        }

        // Accès caméra
        btnCamera.setOnClickListener {
            startActivity(Intent(this, PhotoActivity::class.java))
        }
    }
}
