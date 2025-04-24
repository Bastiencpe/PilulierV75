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
    private lateinit var prefs: SharedPreferences
    private lateinit var progressBar: ProgressBar
    private lateinit var progressText: TextView
    private lateinit var flameAnim: TextView
    private lateinit var bottomNav: BottomNavigationView

    private var compteurFlamme = 1
    private var flammeDejaValidee = false
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)
    private lateinit var today: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "pilulier-db"
        ).fallbackToDestructiveMigration().allowMainThreadQueries().build()

        prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isDarkMode = prefs.getBoolean("dark_mode", false)
        AppCompatDelegate.setDefaultNightMode(
            if (isDarkMode) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
        )

        today = dateFormat.format(Date())
        resetPrisesSiNouveauJour()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val dateTextView: TextView = findViewById(R.id.date_text)
        dateTextView.text = SimpleDateFormat("EEEE d MMMM", Locale.FRANCE).format(Date())

        progressBar = findViewById(R.id.progressBar)
        progressText = findViewById(R.id.progressText)
        flameAnim = findViewById(R.id.flame_animation)

        afficherMedicaments()

        bottomNav = findViewById(R.id.bottom_navigation)
        bottomNav.menu.findItem(R.id.nav_fire).title = "🔥 $compteurFlamme"
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_info -> startActivity(Intent(this, InfoActivity::class.java))
                R.id.nav_calendar -> startActivity(Intent(this, CalendrierActivity::class.java))
                R.id.nav_pill -> startActivity(Intent(this, AjoutActivity::class.java))
                R.id.nav_profile -> startActivity(Intent(this, ProfilActivity::class.java))
            }
            true
        }
    }

    private fun resetPrisesSiNouveauJour() {
        val dernierJour = prefs.getString("dernier_jour", null)
        if (dernierJour != today) {
            db.medicamentDao().resetToutesPrises()
            prefs.edit().putString("dernier_jour", today).apply()
        }
    }

    private fun afficherMedicaments() {
        val matin = findViewById<LinearLayout>(R.id.matin_container)
        val midi = findViewById<LinearLayout>(R.id.midi_container)
        val soir = findViewById<LinearLayout>(R.id.soir_container)

        ajouterMeds(matin, getMeds("matin"))
        ajouterMeds(midi, getMeds("midi"))
        ajouterMeds(soir, getMeds("soir"))

        mettreAJourProgression()
    }

    private fun getMeds(moment: String): List<Medicament> {
        val todayDate = dateFormat.parse(today) ?: return emptyList()
        return db.medicamentDao().getMedicamentParMoment(moment).filter { med ->
            val debut = med.dateDebut?.let { dateFormat.parse(it) }
            val fin = med.dateFin?.let { dateFormat.parse(it) }
            if (debut == null || fin == null) return@filter false
            if (todayDate.before(debut) || todayDate.after(fin)) return@filter false
            when (med.frequence?.lowercase(Locale.FRENCH)) {
                "quotidien" -> true
                "1j sur 2" -> ((todayDate.time - debut.time) / (1000 * 60 * 60 * 24)) % 2 == 0L
                "hebdomadaire" -> {
                    val cal = Calendar.getInstance().apply { time = todayDate }
                    cal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY
                }
                else -> false
            }
        }
    }

    private fun ajouterMeds(container: LinearLayout, meds: List<Medicament>) {
        container.removeAllViews()
        for (med in meds) {
            val checkBox = CheckBox(this)
            checkBox.text = med.nom
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
        val toutes = getToutesLesCheckboxes()
        val toutesCochees = toutes.all { it.isChecked }
        if (toutesCochees && !flammeDejaValidee) {
            compteurFlamme++
            bottomNav.menu.findItem(R.id.nav_fire).title = "🔥 $compteurFlamme"
            flammeDejaValidee = true
            lancerAnimationFlamme()
        }
        if (!toutesCochees) flammeDejaValidee = false
    }

    private fun mettreAJourProgression() {
        val toutes = getToutesLesCheckboxes()
        val total = toutes.size
        val cochees = toutes.count { it.isChecked }
        progressBar.progress = if (total > 0) cochees * 100 / total else 0
        progressText.text = if (cochees == total && total > 0)
            "🎉 $cochees/$total médicaments pris !" else "$cochees/$total médicaments pris"
    }

    private fun getToutesLesCheckboxes(): List<CheckBox> {
        val containers = listOf(
            findViewById<LinearLayout>(R.id.matin_container),
            findViewById(R.id.midi_container),
            findViewById(R.id.soir_container)
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
            .alpha(1f).scaleX(1.5f).scaleY(1.5f).setDuration(400)
            .withEndAction {
                flameAnim.animate()
                    .alpha(0f).scaleX(1f).scaleY(1f).setDuration(400)
                    .withEndAction { flameAnim.visibility = View.INVISIBLE }
                    .start()
            }.start()
    }

    override fun onResume() {
        super.onResume()
        afficherMedicaments()
    }

}
