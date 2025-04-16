package com.example.pilulier

import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class ProfilActivity : AppCompatActivity() {

    private lateinit var prefs: SharedPreferences

    private lateinit var nom: EditText
    private lateinit var prenom: EditText
    private lateinit var urgence: EditText
    private lateinit var donnees: EditText
    // private lateinit var ordonnances: EditText
    // private lateinit var preferences: EditText
    private lateinit var themeSwitch: Switch
    private lateinit var btnHistorique: Button
    private lateinit var imageProfil: ImageView

    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        // Appliquer le thème dès le démarrage (sans recréer plus tard)
        val settingsPrefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isDarkMode = settingsPrefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profil)

        prefs = getSharedPreferences("profil_user", MODE_PRIVATE)

        // Liaison des vues
        nom = findViewById(R.id.etNom)
        prenom = findViewById(R.id.etPrenom)
        urgence = findViewById(R.id.etUrgence)
        donnees = findViewById(R.id.etDonnees)
        // ordonnances = findViewById(R.id.etOrdonnances)
        // preferences = findViewById(R.id.etPreferences)
        themeSwitch = findViewById(R.id.themeSwitch)
        btnHistorique = findViewById(R.id.btnHistorique)
        imageProfil = findViewById(R.id.imageProfil)

        // Charger les données utilisateur
        nom.setText(prefs.getString("nom", ""))
        prenom.setText(prefs.getString("prenom", ""))
        urgence.setText(prefs.getString("urgence", ""))
        donnees.setText(prefs.getString("donnees", ""))
        // ordonnances.setText(prefs.getString("ordonnances", ""))
        // preferences.setText(prefs.getString("preferences", ""))

        // Charger la photo de profil
        prefs.getString("photo_uri", null)?.let { uriString ->
            imageProfil.setImageURI(Uri.parse(uriString))
        }

        // Appliquer l'état du switch du thème
        themeSwitch.isChecked = isDarkMode

        // Changer de thème sans recréer l'activité
        themeSwitch.setOnCheckedChangeListener { _, isChecked ->
            settingsPrefs.edit().putBoolean("dark_mode", isChecked).apply()
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else AppCompatDelegate.MODE_NIGHT_NO
            )
            // Pas de recreate(), les cases de MainActivity ne sont pas perdues !
        }

        // Choix de la photo
        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val uri = result.data?.data
                if (uri != null) {
                    imageProfil.setImageURI(uri)
                    prefs.edit().putString("photo_uri", uri.toString()).apply()
                }
            }
        }

        imageProfil.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
            }
            imagePickerLauncher.launch(intent)
        }

        // Accès à l'historique
        btnHistorique.setOnClickListener {
            startActivity(Intent(this, HistoriqueActivity::class.java))
        }

        // Bouton retour
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        prefs.edit().apply {
            putString("nom", nom.text.toString())
            putString("prenom", prenom.text.toString())
            putString("urgence", urgence.text.toString())
            putString("donnees", donnees.text.toString())
            // putString("ordonnances", ordonnances.text.toString())
            // putString("preferences", preferences.text.toString())
            apply()
        }
    }
}
