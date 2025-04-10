package com.example.pilulier
import androidx.room.Room
import com.example.pilulier.data.AppDatabase

import android.os.Bundle
import android.widget.CalendarView
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.*

class CalendrierActivity : AppCompatActivity() {

    private lateinit var calendarView: CalendarView
    private lateinit var medsText: TextView

    // Médicaments fictifs par date (format yyyy-MM-dd)
    private val medsParJour = mapOf(
        "2025-04-04" to listOf("Doliprane", "Vitamine D"),
        "2025-04-05" to listOf("Ibuprofène"),
        "2025-04-06" to emptyList()
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_calendrier)
        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "pilulier-db"
        ).fallbackToDestructiveMigration().allowMainThreadQueries().build()

        calendarView = findViewById(R.id.calendarView)
        medsText = findViewById(R.id.tvMedsOfDay)

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)

        calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            val dateStr = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            val liste = getMedsPourDate(db, dateStr)

            medsText.text = when {
                liste.isEmpty() -> "Aucun médicament prévu ce jour-là."
                else -> "Médicaments du $dateStr :\n• " + liste.joinToString("\n• ")
            }

        }

        // Bouton retour
        findViewById<ImageButton>(R.id.btnBack).setOnClickListener {
            finish()
        }
    }
}
