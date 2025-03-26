package com.example.pilulier

import android.widget.TextView
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Définir le TextView pour afficher la date
        val dateTextView: TextView = findViewById(R.id.date_text)

        // Obtenir la date actuelle
        val currentDate = Calendar.getInstance()

        // Définir le format de la date : Jour de la semaine et jour du mois
        val dateFormat = SimpleDateFormat("EEEE d MMMM", Locale.FRENCH)

        // Formater la date actuelle
        val formattedDate = dateFormat.format(currentDate.time)

        // Afficher la date formatée dans le TextView
        dateTextView.text = formattedDate

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_calendar -> {
                    val intent = Intent(this, CalendrierActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }
}
