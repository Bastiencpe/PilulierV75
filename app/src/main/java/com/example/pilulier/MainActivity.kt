package com.example.pilulier

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import android.widget.TextView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Appliquer les marges pour les barres système
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        // === Ajout de l'affichage de la date ===
        val dateTextView: TextView = findViewById(R.id.date_text)
        val dateFormat = SimpleDateFormat("EEEE d MMMM", Locale.FRANCE)
        val currentDate: String = dateFormat.format(Date())
        dateTextView.text = currentDate

        // Navigation via le menu du bas
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {

                // Ouvre la page Infos
                R.id.nav_info -> {
                    startActivity(Intent(this, InfoActivity::class.java))
                    true
                }

                // Ouvre le calendrier
                R.id.nav_calendar -> {
                    startActivity(Intent(this, CalendrierActivity::class.java))
                    true
                }

                // Ouvre la page Pilules (à créer plus tard si besoin)
                R.id.nav_pill -> {
                    // startActivity(Intent(this, PilulesActivity::class.java))
                    true
                }

                // Ouvre la page Flamme (à créer plus tard aussi)
                R.id.nav_fire -> {
                    // startActivity(Intent(this, FlammeActivity::class.java))
                    true
                }

                // Ouvre le profil utilisateur
                R.id.nav_profile -> {
                    startActivity(Intent(this, ProfilActivity::class.java))
                    true
                }

                else -> false
            }
        }
    }
}
