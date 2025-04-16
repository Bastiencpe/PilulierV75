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
    private var flammeDejaValidee = false

    private lateinit var bottomNav: BottomNavigationView
    private lateinit var prefs: SharedPreferences
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)

    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var flameAnim: TextView

    private lateinit var today: String

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = Room.databaseBuilder(applicationContext, AppDatabase::class.java, "pilulier-db")
            .fallbackToDestructiveMigration()
            .allowMainThreadQueries()
            .build()

        today = dateFormat.format(Date())

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val dateTextView: TextView = findViewById(R.id.date_text)
        val currentDateStr = SimpleDateFormat("EEEE d MMMM", Locale.FRANCE).format(Date())
        dateTextView.text = currentDateStr

        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)
        flameAnim = findViewById(R.id.flame_animation)

        afficherMedicamentParMoment()
        mettreAJourProgression()

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
                R.id.nav_pill -> {
                    startActivity(Intent(this, PhotoActivity::class.java)); true
                }
                R.id.nav_fire -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfilActivity::class.java)); true
                }
                else -> false
            }
        }
    }

    private fun afficherMedicamentParMoment() {
        val matinContainer = findViewById<LinearLayout>(R.id.matin_container)
        val midiContainer = findViewById<LinearLayout>(R.id.midi_container)
        val soirContainer = findViewById<LinearLayout>(R.id.soir_container)

        ajouterMeds(matinContainer, db.medicamentDao().getMedicamentParMoment("matin"))
        ajouterMeds(midiContainer, db.medicamentDao().getMedicamentParMoment("midi"))
        ajouterMeds(soirContainer, db.medicamentDao().getMedicamentParMoment("soir"))
    }

    private fun ajouterMeds(container: LinearLayout, meds: List<Medicament>) {
        container.removeAllViews()
        for (med in meds) {
            val checkBox = CheckBox(this)
            checkBox.text = med.nom
            checkBox.textSize = 16f
            checkBox.isChecked = med.pris

            checkBox.setOnCheckedChangeListener { _, isChecked ->
                db.medicamentDao().majEtatPrise(med.copy(pris = isChecked))
                verifierToutesLesCasesCochees()
                mettreAJourProgression()
            }

            container.addView(checkBox)
        }
    }

    private fun verifierToutesLesCasesCochees() {
        val toutesLesCases = getToutesLesCheckboxes()
        val toutesCochees = toutesLesCases.all { it.isChecked }

        if (toutesCochees && !flammeDejaValidee) {
            compteurFlamme++
            bottomNav.menu.findItem(R.id.nav_fire).title = "🔥 $compteurFlamme"
            flammeDejaValidee = true
            lancerAnimationFlamme()
        }

        if (!toutesCochees) {
            flammeDejaValidee = false
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

        prefs.edit().putString("historique_$today", "$cochees/$total").apply()
    }

    private fun getToutesLesCheckboxes(): List<CheckBox> {
        val containers = listOf(
            findViewById(R.id.matin_container),
            findViewById(R.id.midi_container),
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
