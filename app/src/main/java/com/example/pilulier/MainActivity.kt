package com.example.pilulier

import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    private var compteurFlamme = 1  // Compteur de clics sur le bouton flamme

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Appliquer les marges pour les barres système
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Affichage de la date
        val dateTextView: TextView = findViewById(R.id.date_text)
        val dateFormat = SimpleDateFormat("EEEE d MMMM", Locale.FRANCE)
        val currentDate: String = dateFormat.format(Date())
        dateTextView.text = currentDate

        // Initialisation de la barre de navigation en bas
        // Mise à jour dynamique des médicaments
        val matinContainer = findViewById<LinearLayout>(R.id.matin_container)
        val midiContainer = findViewById<LinearLayout>(R.id.midi_container)
        val soirContainer = findViewById<LinearLayout>(R.id.soir_container)

        // Exemple : Liste des médicaments (à remplacer par les données réelles)
        val traitements = mapOf(
            "matin" to listOf("Anti-inflammatoire", "Fer"),
            "midi" to listOf("Doliprane"),
            "soir" to listOf("Advil")
        )

        ajouterMedsDynamique(matinContainer, traitements["matin"] ?: emptyList())
        ajouterMedsDynamique(midiContainer, traitements["midi"] ?: emptyList())
        ajouterMedsDynamique(soirContainer, traitements["soir"] ?: emptyList())

        // Navigation via le menu du bas
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // Gère les clics sur les icônes du menu du bas
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {

                // Ouvre la page Info
                R.id.nav_info -> {
                    startActivity(Intent(this, InfoActivity::class.java))
                    true
                }

                // Ouvre la page Calendrier
                R.id.nav_calendar -> {
                    startActivity(Intent(this, CalendrierActivity::class.java))
                    true
                }

                // Ouvre la page Pilules (à créer si besoin)
                R.id.nav_pill -> {
                    // Pour l'instant, l'activité Pilules n'est pas encore implémentée.
                    // startActivity(Intent(this, PilulesActivity::class.java))
                    // Placeholder pour la navigation Pilules
                    true
                }

                // Incrémente le compteur flamme
                R.id.nav_fire -> {
                    // Pour l'instant, l'activité Flamme n'est pas encore implémentée.
                    // startActivity(Intent(this, FlammeActivity::class.java))
                    compteurFlamme++
                    bottomNav.menu.findItem(R.id.nav_fire).title = compteurFlamme.toString()
                    true
                }

                // Ouvre la page Profil utilisateur
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfilActivity::class.java))
                    true
                }

                else -> false
            }
        }
    }

    private fun ajouterMedsDynamique(container: LinearLayout, medicaments: List<String>) {
        container.removeAllViews()
        for (med in medicaments) {
            val checkBox = CheckBox(this)
            checkBox.text = med
            checkBox.textSize = 16f
            container.addView(checkBox)
        }
    }
}
