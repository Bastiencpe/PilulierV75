package com.example.pilulier

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.ImageButton
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class ProfilActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profil)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val themeSwitch = findViewById<Switch>(R.id.themeSwitch)

        prefs = getSharedPreferences("settings", MODE_PRIVATE)

        // Initialiser le switch selon le thème actuel
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        themeSwitch.isChecked = isDarkMode

        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                prefs.edit().putBoolean("dark_mode", true).apply()
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                prefs.edit().putBoolean("dark_mode", false).apply()
            }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }
}
