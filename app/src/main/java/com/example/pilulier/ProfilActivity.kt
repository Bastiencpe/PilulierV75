package com.example.pilulier

import android.content.SharedPreferences
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Switch
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class ProfilActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    private lateinit var nom: EditText
    private lateinit var prenom: EditText
    private lateinit var urgence: EditText
    private lateinit var ordonnances: EditText
    private lateinit var donnees: EditText
    private lateinit var preferences: EditText
    private lateinit var themeSwitch: Switch

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profil)

        prefs = getSharedPreferences("profil_user", MODE_PRIVATE)

        // Lier les vues
        nom = findViewById(R.id.etNom)
        prenom = findViewById(R.id.etPrenom)
        urgence = findViewById(R.id.etUrgence)
        ordonnances = findViewById(R.id.etOrdonnances)
        donnees = findViewById(R.id.etDonnees)
        preferences = findViewById(R.id.etPreferences)
        themeSwitch = findViewById(R.id.themeSwitch)

        // Remplir les champs avec les données enregistrées
        nom.setText(prefs.getString("nom", ""))
        prenom.setText(prefs.getString("prenom", ""))
        urgence.setText(prefs.getString("urgence", ""))
        ordonnances.setText(prefs.getString("ordonnances", ""))
        donnees.setText(prefs.getString("donnees", ""))
        preferences.setText(prefs.getString("preferences", ""))

        // Thème sombre
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        themeSwitch.isChecked = isDarkMode

        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.edit().putBoolean("dark_mode", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        // Bouton retour
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    override fun onPause() {
        super.onPause()

        // Sauvegarde automatique des champs
        prefs.edit().apply {
            putString("nom", nom.text.toString())
            putString("prenom", prenom.text.toString())
            putString("urgence", urgence.text.toString())
            putString("ordonnances", ordonnances.text.toString())
            putString("donnees", donnees.text.toString())
            putString("preferences", preferences.text.toString())
            apply()
        }
    }
}
