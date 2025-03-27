package com.example.pilulier

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var compteurFlamme = 1
    private var flammeDéjàValidée = false
    private lateinit var bottomNav: BottomNavigationView
    private lateinit var prefs: SharedPreferences
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)

    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        // Thème clair/sombre
        prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Date affichée
        val dateTextView: TextView = findViewById(R.id.date_text)
        val currentDate: String = SimpleDateFormat("EEEE d MMMM", Locale.FRANCE).format(Date())
        dateTextView.text = currentDate

        // Initialiser la barre de progression
        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)

        // Containers médicaments
        val matinContainer = findViewById<LinearLayout>(R.id.matin_container)
        val midiContainer = findViewById<LinearLayout>(R.id.midi_container)
        val soirContainer = findViewById<LinearLayout>(R.id.soir_container)

        // Vérifier date
        val todayKey = dateFormat.format(Date())
        val lastDate = prefs.getString("last_open_date", "")
        val isNewDay = todayKey != lastDate

        // Exemple de données
        val traitements = mapOf(
            "matin" to listOf("Anti-inflammatoire", "Fer"),
            "midi" to listOf("Doliprane"),
            "soir" to listOf("Advil")
        )

        ajouterMedsDynamique(matinContainer, traitements["matin"] ?: emptyList(), isNewDay)
        ajouterMedsDynamique(midiContainer, traitements["midi"] ?: emptyList(), isNewDay)
        ajouterMedsDynamique(soirContainer, traitements["soir"] ?: emptyList(), isNewDay)

        prefs.edit().putString("last_open_date", todayKey).apply()

        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.menu.findItem(R.id.nav_fire).title = "🔥 $compteurFlamme"

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_info -> {
                    startActivity(Intent(this, InfoActivity::class.java))
                    true
                }
                R.id.nav_calendar -> {
                    startActivity(Intent(this, CalendrierActivity::class.java))
                    true
                }
                R.id.nav_pill -> true
                R.id.nav_fire -> true
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfilActivity::class.java))
                    true
                }
                else -> false
            }
        }
    }

    private fun ajouterMedsDynamique(container: LinearLayout, medicaments: List<String>, decocher: Boolean) {
        container.removeAllViews()
        for (med in medicaments) {
            val checkBox = CheckBox(this)
            checkBox.text = med
            checkBox.textSize = 16f
            checkBox.isChecked = false // !decocher

            checkBox.setOnCheckedChangeListener { _, _ ->
                verifierToutesLesCasesCochees()
                mettreAJourProgression()
            }

            container.addView(checkBox)
        }
    }

    private fun verifierToutesLesCasesCochees() {
        val matinContainer = findViewById<LinearLayout>(R.id.matin_container)
        val midiContainer = findViewById<LinearLayout>(R.id.midi_container)
        val soirContainer = findViewById<LinearLayout>(R.id.soir_container)

        val toutesLesCases = mutableListOf<CheckBox>()
        for (container in listOf(matinContainer, midiContainer, soirContainer)) {
            for (i in 0 until container.childCount) {
                val view = container.getChildAt(i)
                if (view is CheckBox) {
                    toutesLesCases.add(view)
                }
            }
        }

        val toutesCochees = toutesLesCases.all { it.isChecked }

        if (toutesCochees && !flammeDéjàValidée) {
            compteurFlamme++
            bottomNav.menu.findItem(R.id.nav_fire).title = "🔥 $compteurFlamme"
            flammeDéjàValidée = true
        }

        if (!toutesCochees) {
            flammeDéjàValidée = false
        }
    }

    private fun mettreAJourProgression() {
        val matinContainer = findViewById<LinearLayout>(R.id.matin_container)
        val midiContainer = findViewById<LinearLayout>(R.id.midi_container)
        val soirContainer = findViewById<LinearLayout>(R.id.soir_container)

        var total = 0
        var cochées = 0

        for (container in listOf(matinContainer, midiContainer, soirContainer)) {
            for (i in 0 until container.childCount) {
                val view = container.getChildAt(i)
                if (view is CheckBox) {
                    total++
                    if (view.isChecked) cochées++
                }
            }
        }

        val pourcentage = if (total > 0) (cochées * 100 / total) else 0
        progressBar.progress = pourcentage

        progressText.text = if (pourcentage == 100)
            "🎉 $cochées/$total médicaments pris !"
        else
            "$cochées/$total médicaments pris"
    }
}
