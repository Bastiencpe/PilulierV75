package com.example.pilulier

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.room.Room
import com.example.pilulier.data.AppDatabase
import com.example.pilulier.data.Medicament
import java.text.SimpleDateFormat
import java.util.*

class AjoutActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)

    private lateinit var etNom: EditText
    private lateinit var spinnerMoment: Spinner
    private lateinit var spinnerFrequence: Spinner
    private lateinit var etDateDebut: EditText
    private lateinit var etDateFin: EditText
    private lateinit var tvForme: TextView
    private lateinit var tvCouleur: TextView

    private var momentsDetectes: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_ajout)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "pilulier-db")
            .fallbackToDestructiveMigration().allowMainThreadQueries().build()

        etNom = findViewById(R.id.etNomMedicament)
        spinnerMoment = findViewById(R.id.spinnerMoment)
        spinnerFrequence = findViewById(R.id.spinnerFrequence)
        etDateDebut = findViewById(R.id.etDateDebut)
        etDateFin = findViewById(R.id.etDateFin)
        tvForme = findViewById(R.id.tvForme)
        tvCouleur = findViewById(R.id.tvCouleur)

        val btnEnregistrer = findViewById<Button>(R.id.btnEnregistrer)
        val btnBack = findViewById<ImageButton>(R.id.btnBackAjout)
        val btnCamera = findViewById<Button>(R.id.btnCamera)

        btnCamera.setOnClickListener {
            val intent = Intent(this, PhotoActivity::class.java)
            startActivityForResult(intent, 1001)
        }

        val moments = listOf("matin", "midi", "soir")
        val frequences = listOf("quotidien", "1j sur 2", "hebdomadaire")

        spinnerMoment.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, moments).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        spinnerFrequence.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, frequences).apply {
            setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        }

        val calendar = Calendar.getInstance()

        etDateDebut.setOnClickListener {
            DatePickerDialog(
                this, { _, year, month, day ->
                    val date = Calendar.getInstance().apply { set(year, month, day) }
                    etDateDebut.setText(dateFormat.format(date.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        etDateFin.setOnClickListener {
            DatePickerDialog(
                this, { _, year, month, day ->
                    val date = Calendar.getInstance().apply { set(year, month, day) }
                    etDateFin.setText(dateFormat.format(date.time))
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        btnEnregistrer.setOnClickListener {
            val nom = etNom.text.toString()
            val dateDebut = etDateDebut.text.toString()
            val dateFin = etDateFin.text.toString()
            val frequence = spinnerFrequence.selectedItem.toString()

            if (nom.isNotBlank() && dateDebut.isNotBlank() && dateFin.isNotBlank()) {
                val moments = if (momentsDetectes.isNotEmpty()) {
                    momentsDetectes
                } else {
                    listOf(spinnerMoment.selectedItem.toString())
                }

                val message = buildString {
                    append("Nom : $nom\n")
                    append("Fréquence : $frequence\n")
                    append("Moments : ${moments.joinToString(", ")}\n")
                    append("Début : $dateDebut\n")
                    append("Fin : $dateFin\n")
                    append("Total : ${moments.size} prise(s)")
                }

                AlertDialog.Builder(this)
                    .setTitle("Confirmer l'ajout")
                    .setMessage(message)
                    .setPositiveButton("Ajouter") { _, _ ->
                        for (moment in moments) {
                            val medicament = Medicament(
                                nom = nom,
                                moment = moment,
                                dateDebut = dateDebut,
                                dateFin = dateFin,
                                frequence = frequence
                            )
                            db.medicamentDao().ajouterMedicament(medicament)
                        }
                        Toast.makeText(this, "Médicament(s) ajouté(s) !", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .setNegativeButton("Annuler", null)
                    .show()
            } else {
                Toast.makeText(this, "Veuillez remplir tous les champs", Toast.LENGTH_SHORT).show()
            }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == 1001 && resultCode == RESULT_OK) {
            val nom = data?.getStringExtra("texte_ocr") ?: ""
            val freq = data?.getStringExtra("frequence_ocr") ?: ""
            val momentsString = data?.getStringExtra("moments_ocr") ?: ""
            val forme = data?.getStringExtra("forme_detectee") ?: ""

            val r = data?.getDoubleExtra("couleur_r", -1.0) ?: -1.0
            val g = data?.getDoubleExtra("couleur_g", -1.0) ?: -1.0
            val b = data?.getDoubleExtra("couleur_b", -1.0) ?: -1.0

            if (nom.isNotBlank()) etNom.setText(nom)

            // Si la forme est rectangle (Doliprane), on remplit les champs automatiquement
            if (forme.equals("rectangle", ignoreCase = true) || forme.equals("carré", ignoreCase = true)) {
                // Moment = midi
                val moments = listOf("midi")
                momentsDetectes = moments
                val indexMoment = (spinnerMoment.adapter as ArrayAdapter<String>).getPosition("midi")
                if (indexMoment >= 0) spinnerMoment.setSelection(indexMoment)

                // Fréquence = quotidien
                val indexFreq = (spinnerFrequence.adapter as ArrayAdapter<String>).getPosition("quotidien")
                if (indexFreq >= 0) spinnerFrequence.setSelection(indexFreq)

                // Date début = aujourd'hui
                val calendar = Calendar.getInstance()
                etDateDebut.setText(dateFormat.format(calendar.time))

                // Date fin = dans 5 jours
                calendar.add(Calendar.DAY_OF_MONTH, 5)
                etDateFin.setText(dateFormat.format(calendar.time))

            } else {
                // Sinon, on essaie de remplir avec les valeurs reçues
                if (freq.isNotBlank()) {
                    val index = (spinnerFrequence.adapter as ArrayAdapter<String>).getPosition(freq)
                    if (index >= 0) spinnerFrequence.setSelection(index)
                }

                if (momentsString.isNotBlank()) {
                    momentsDetectes = momentsString.split(",").map { it.trim() }
                    val defaultMoment = momentsDetectes.firstOrNull()
                    if (defaultMoment != null) {
                        val index = (spinnerMoment.adapter as ArrayAdapter<String>).getPosition(defaultMoment)
                        if (index >= 0) spinnerMoment.setSelection(index)
                    }
                }
            }

            // Affichage forme détectée etc. comme avant
            if (forme.isNotBlank()) {
                tvForme.text = "Forme détectée : $forme"
                val objFile = when (forme.lowercase(Locale.FRENCH)) {
                    "triangle" -> "triangle.obj"
                    "rectangle", "carré" -> "rectangle.obj"
                    "cercle", "rond" -> "cercle.obj"
                    else -> null
                }
                if (objFile != null) {
                    val intent = Intent(this, ModelViewerActivity::class.java)
                    intent.putExtra("OBJ_FILE", objFile)
                    startActivity(intent)
                }
            }

            if (r >= 0 && g >= 0 && b >= 0) {
                val rgbText = "Couleur détectée : R=%.0f, G=%.0f, B=%.0f".format(r, g, b)
                tvCouleur.text = rgbText
            }
        }
    }
}
