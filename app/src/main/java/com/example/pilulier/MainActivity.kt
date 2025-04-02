package com.example.pilulier

import android.content.Intent
import android.content.SharedPreferences
import android.media.MediaPlayer
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
    private lateinit var flameAnim: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        // Appliquer thème
        prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES
            else AppCompatDelegate.MODE_NIGHT_NO
        )

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val dateTextView: TextView = findViewById(R.id.date_text)
        val currentDate = SimpleDateFormat("EEEE d MMMM", Locale.FRANCE).format(Date())
        dateTextView.text = currentDate

        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)
        flameAnim = findViewById(R.id.flame_animation)

        val matinContainer = findViewById<LinearLayout>(R.id.matin_container)
        val midiContainer = findViewById<LinearLayout>(R.id.midi_container)
        val soirContainer = findViewById<LinearLayout>(R.id.soir_container)

        val todayKey = dateFormat.format(Date())
        val lastDate = prefs.getString("last_open_date", "")
        val isNewDay = todayKey != lastDate

        val traitements = mapOf(
            "matin" to listOf("Anti-inflammatoire", "Fer"),
            "midi" to listOf("Doliprane"),
            "soir" to listOf("Advil")
        )

        ajouterMedsDynamique(matinContainer, traitements["matin"] ?: emptyList())
        ajouterMedsDynamique(midiContainer, traitements["midi"] ?: emptyList())
        ajouterMedsDynamique(soirContainer, traitements["soir"] ?: emptyList())

        if (isNewDay) {
            flammeDéjàValidée = false
            prefs.edit().putString("last_open_date", todayKey).apply()
        }

        restaurerEtatCheckboxes()

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

        mettreAJourProgression()
    }

    override fun onPause() {
        super.onPause()
        sauvegarderEtatCheckboxes()
    }

    private fun ajouterMedsDynamique(container: LinearLayout, medicaments: List<String>) {
        container.removeAllViews()
        for (med in medicaments) {
            val checkBox = CheckBox(this)
            checkBox.text = med
            checkBox.textSize = 16f
            checkBox.isChecked = false

            checkBox.setOnCheckedChangeListener { _, _ ->
                verifierToutesLesCasesCochees()
                mettreAJourProgression()
            }

            container.addView(checkBox)
        }
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

    private fun sauvegarderEtatCheckboxes() {
        val allCheckboxes = getToutesLesCheckboxes()
        val editor = prefs.edit()
        allCheckboxes.forEachIndexed { index, checkBox ->
            editor.putBoolean("checkbox_$index", checkBox.isChecked)
        }
        editor.apply()
    }

    private fun restaurerEtatCheckboxes() {
        val allCheckboxes = getToutesLesCheckboxes()
        allCheckboxes.forEachIndexed { index, checkBox ->
            val etat = prefs.getBoolean("checkbox_$index", false)
            checkBox.isChecked = etat
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
            jouerSonReussite() // 🔊 Ajout ici
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

        val dateKey = dateFormat.format(Date())
        prefs.edit().putString("historique_$dateKey", "$cochees/$total").apply()
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

    private fun jouerSonReussite() {
        val mediaPlayer = MediaPlayer.create(this, R.raw.success)
        mediaPlayer.start()
        mediaPlayer.setOnCompletionListener {
            it.release()
        }
    }
}
