package com.example.pilulier

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.example.pilulier.data.AppDatabase
import com.example.pilulier.data.Medicament
import java.text.SimpleDateFormat
import java.util.*

class AjoutActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ajout)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "pilulier-db"
        ).fallbackToDestructiveMigration().allowMainThreadQueries().build()

        val etNom = findViewById<EditText>(R.id.etNomMedicament)
        val spinnerMoment = findViewById<Spinner>(R.id.spinnerMoment)
        val spinnerFrequence = findViewById<Spinner>(R.id.spinnerFrequence)
        val etDateDebut = findViewById<EditText>(R.id.etDateDebut)
        val etDateFin = findViewById<EditText>(R.id.etDateFin)
        val btnEnregistrer = findViewById<Button>(R.id.btnEnregistrer)
        val btnBack = findViewById<ImageButton>(R.id.btnBackAjout)
        val btnCamera = findViewById<Button>(R.id.btnCamera)

        btnCamera.setOnClickListener {
            startActivity(Intent(this, PhotoActivity::class.java))
        }




        // Remplissage des Spinners
        val moments = listOf("matin", "midi", "soir")
        val frequences = listOf("quotidien", "1j sur 2", "hebdomadaire")

        val momentAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, moments)
        momentAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerMoment.adapter = momentAdapter

        val frequenceAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, frequences)
        frequenceAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerFrequence.adapter = frequenceAdapter

        // Date pickers
        val calendar = Calendar.getInstance()

        val dateListenerDebut = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(year, month, day)
            etDateDebut.setText(dateFormat.format(selectedDate.time))
        }

        val dateListenerFin = DatePickerDialog.OnDateSetListener { _, year, month, day ->
            val selectedDate = Calendar.getInstance()
            selectedDate.set(year, month, day)
            etDateFin.setText(dateFormat.format(selectedDate.time))
        }

        etDateDebut.setOnClickListener {
            DatePickerDialog(
                this, dateListenerDebut,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        etDateFin.setOnClickListener {
            DatePickerDialog(
                this, dateListenerFin,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // Enregistrement du médicament
        btnEnregistrer.setOnClickListener {
            val nom = etNom.text.toString()
            val moment = spinnerMoment.selectedItem.toString()
            val dateDebut = etDateDebut.text.toString()
            val dateFin = etDateFin.text.toString()
            val frequence = spinnerFrequence.selectedItem.toString()

            if (nom.isNotBlank() && dateDebut.isNotBlank() && dateFin.isNotBlank()) {
                val medicament = Medicament(
                    nom = nom,
                    moment = moment,
                    dateDebut = dateDebut,
                    dateFin = dateFin,
                    frequence = frequence
                )
                db.medicamentDao().ajouterMedicament(medicament)

                Toast.makeText(this, "Médicament ajouté !", Toast.LENGTH_SHORT).show()
                finish() // Retour à MainActivity
            } else {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            }
        }

        // Bouton retour
        btnBack.setOnClickListener {
            finish()
        }


    }
}
