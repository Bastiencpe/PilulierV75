package com.example.pilulier

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.view.View
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.room.Room
import com.example.pilulier.data.AppDatabase
import com.example.pilulier.data.Medicament
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var db: AppDatabase
    private var compteurFlamme = 1
    private var flammeDéjàValidée = false

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var prefs: SharedPreferences
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)

    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var flameAnim: TextView

    private lateinit var today: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialisation de la base de données
        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "pilulier-db"
        ).fallbackToDestructiveMigration().allowMainThreadQueries().build()

        // Thème
        prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        // Marges (gestion des fenêtres système)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Date actuelle
        val dateTextView: TextView = findViewById(R.id.date_text)
        val currentDateStr = SimpleDateFormat("EEEE d MMMM", Locale.FRANCE).format(Date())
        today = dateFormat.format(Date())
        dateTextView.text = currentDateStr

        // UI
        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)
        flameAnim = findViewById(R.id.flame_animation)

        // Exemple de données initiales (si nécessaire)
        resetDatabase()

        // Filtrer les médicaments du jour et les afficher
        afficherMedicamentParMoment()

        mettreAJourProgression()

        // Navigation
        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.menu.findItem(R.id.nav_fire).title = "🔥 $compteurFlamme"

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_info -> {
                    startActivity(Intent(this, InfoActivity::class.java)); true
                }
                R.id.nav_calendar -> {
                    startActivity(Intent(this, CalendrierActivity::class.java)); true
                }
                R.id.nav_pill -> true
                R.id.nav_fire -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfilActivity::class.java)); true
                }
                else -> false
            }
        }
    }

    private fun resetDatabase() {
        db.medicamentDao().supprimerTous()
        db.medicamentDao().ajouterMedicament(
            Medicament(
                nom = "Doliprane",
                moment = "matin",
                dateDebut = "2025-04-10",
                dateFin = "2025-04-17",
                frequence = "quotidien"
            )
        )
        db.medicamentDao().ajouterMedicament(
            Medicament(
                nom = "Fer",
                moment = "soir",
                dateDebut = "2025-04-10",
                dateFin = "2025-04-17",
                frequence = "1j sur 2"
            )
        )
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

    private fun afficherMedicamentParMoment() {
        val matinContainer = findViewById<LinearLayout>(R.id.matin_container)
        val midiContainer = findViewById<LinearLayout>(R.id.midi_container)
        val soirContainer = findViewById<LinearLayout>(R.id.soir_container)

        ajouterMeds(matinContainer, filtrerMedicamentDuJour(db.medicamentDao().getMedicamentParMoment("matin")))
        ajouterMeds(midiContainer, filtrerMedicamentDuJour(db.medicamentDao().getMedicamentParMoment("midi")))
        ajouterMeds(soirContainer, filtrerMedicamentDuJour(db.medicamentDao().getMedicamentParMoment("soir")))
    }

    private fun filtrerMedicamentDuJour(medicaments: List<Medicament>): List<Medicament> {
        val todayDate = dateFormat.parse(today) ?: return emptyList()
        return medicaments.filter { med ->
            val dateDebut = med.dateDebut?.let { dateFormat.parse(it) } ?: return@filter false
            val dateFin = med.dateFin?.let { dateFormat.parse(it) } ?: return@filter false

            if (todayDate.before(dateDebut) || todayDate.after(dateFin)) return@filter false

            when (med.frequence?.lowercase(Locale.FRANCE)) {
                "quotidien" -> true
                "1j sur 2" -> {
                    val diff = (todayDate.time - dateDebut.time) / (1000 * 60 * 60 * 24)
                    diff % 2 == 0L
                }
                "hebdomadaire" -> {
                    val calendar = Calendar.getInstance().apply { time = todayDate }
                    calendar.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY // ou autre jour ?
                }
                else -> false
            }
        }
    }

    private fun ajouterMeds(container: LinearLayout, meds: List<Medicament>) {
        container.removeAllViews()
        for (med in meds) {
            val checkBox = CheckBox(this)
            checkBox.text = med.nom
            checkBox.textSize = 16f
            checkBox.isChecked = med.pris

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                // Récupère le médicament de la base de données
                val updatedMedicament = med.copy(pris = isChecked) // Met à jour l'état "pris" avec la valeur de la case à cocher
                db.medicamentDao().majEtatPrise(updatedMedicament) // Passe l'objet Medicament mis à jour
                verifierToutesLesCasesCochees()
                mettreAJourProgression()
            }


            container.addView(checkBox)
        }
    }

    private fun verifierToutesLesCasesCochees() {
        val toutesLesCases = getToutesLesCheckboxes()
        val toutesCochees = toutesLesCases.all { it.isChecked }

        if (toutesCochees && !flammeDéjàValidée) {
            compteurFlamme++
            bottomNav.menu.findItem(R.id.nav_fire).title = "🔥 $compteurFlamme"
            flammeDéjàValidée = true
            lancerAnimationFlamme()
        }

        if (!toutesCochees) {
            flammeDéjàValidée = false
        }
    }

    private fun mettreAJourProgression() {
        val toutesLesCases = getToutesLesCheckboxes()
        val total = toutesLesCases.size
        val cochees = toutesLesCases.count { it.isChecked }
        val pourcentage = if (total > 0) cochees * 100 / total else 0

        progressBar.progress = pourcentage
        progressText.text = if (pourcentage == 100)
            "🎉 $cochees/$total médicaments pris !"
        else
            "$cochees/$total médicaments pris"
    }

    private fun getToutesLesCheckboxes(): List<CheckBox> {
        val containers = listOf(
            findViewById<LinearLayout>(R.id.matin_container),
            findViewById<LinearLayout>(R.id.midi_container),
            findViewById<LinearLayout>(R.id.soir_container)
        )
        return containers.flatMap { container ->
            (0 until container.childCount).mapNotNull {
                container.getChildAt(it) as? CheckBox
            }
        }
    }

    private fun lancerAnimationFlamme() {
        flameAnim.alpha = 0f
        flameAnim.scaleX = 0.5f
        flameAnim.scaleY = 0.5f
        flameAnim.visibility = View.VISIBLE

        flameAnim.animate()
            .alpha(1f)
            .scaleX(1.5f)
            .scaleY(1.5f)
            .setDuration(400)
            .withEndAction {
                flameAnim.animate()
                    .alpha(0f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(400)
                    .withEndAction {
                        flameAnim.visibility = View.INVISIBLE
                    }
                    .start()
            }
            .start()
    }
}
