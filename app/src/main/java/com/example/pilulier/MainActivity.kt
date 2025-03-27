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

    private var compteurFlamme = 1
    private var flammeDéjàValidée = false
    private lateinit var bottomNav: BottomNavigationView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Gérer les marges système
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

        // Références aux conteneurs
        val matinContainer = findViewById<LinearLayout>(R.id.matin_container)
        val midiContainer = findViewById<LinearLayout>(R.id.midi_container)
        val soirContainer = findViewById<LinearLayout>(R.id.soir_container)

        // Exemple de médicaments
        val traitements = mapOf(
            "matin" to listOf("Anti-inflammatoire", "Fer"),
            "midi" to listOf("Doliprane"),
            "soir" to listOf("Advil")
        )

        // Ajout dynamique
        ajouterMedsDynamique(matinContainer, traitements["matin"] ?: emptyList())
        ajouterMedsDynamique(midiContainer, traitements["midi"] ?: emptyList())
        ajouterMedsDynamique(soirContainer, traitements["soir"] ?: emptyList())

        // Initialisation de la barre de navigation
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
                R.id.nav_pill -> {
                    // Placeholder
                    true
                }
                R.id.nav_fire -> {
                    // Aucun effet, affichage uniquement
                    true
                }
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

            // Vérification à chaque changement
            checkBox.setOnCheckedChangeListener { _, _ ->
                verifierToutesLesCasesCochees()
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
}
