package com.example.pilulier

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import java.text.SimpleDateFormat
import java.util.*

class HistoriqueActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var prefs: SharedPreferences
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.FRANCE)

    override fun onCreate(savedInstanceState: Bundle?) {
        prefs = getSharedPreferences("settings", MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_mode", false)
        setTheme(if (isDark) R.style.Theme_Pilulier else R.style.Theme_Pilulier)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_historique)

        findViewById<ImageButton>(R.id.btnBackHistorique).setOnClickListener {
            finish()
        }

        recyclerView = findViewById(R.id.recyclerHistorique)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val items = getDerniersJoursAvecProgression()
        recyclerView.adapter = HistoriqueAdapter(items)

        setupGraphique(items)
    }

    private fun getDerniersJoursAvecProgression(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        val calendar = Calendar.getInstance()

        repeat(7) {
            val date = dateFormat.format(calendar.time)
            val progression = prefs.getString("historique_$date", "0/0") ?: "0/0"
            list.add(Pair(date, progression))
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        return list.reversed() // du plus ancien au plus récent
    }

    private fun setupGraphique(data: List<Pair<String, String>>) {
        val barChart = findViewById<BarChart>(R.id.barChart)
        val entries = ArrayList<BarEntry>()
        val labels = ArrayList<String>()

        for ((index, pair) in data.withIndex()) {
            val (date, status) = pair
            labels.add(date.substring(5)) // MM-dd
            val parts = status.split("/")
            val total = parts.getOrNull(1)?.toFloatOrNull() ?: 0f
            val pris = parts.getOrNull(0)?.toFloatOrNull() ?: 0f
            val ratio = if (total > 0) pris / total * 100 else 0f
            entries.add(BarEntry(index.toFloat(), ratio))
        }

        val dataSet = BarDataSet(entries, "Progression %")
        dataSet.color = getColor(android.R.color.holo_blue_light)
        val barData = BarData(dataSet)
        barChart.data = barData

        barChart.xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        barChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        barChart.xAxis.setDrawGridLines(false)
        barChart.axisLeft.axisMinimum = 0f
        barChart.axisLeft.axisMaximum = 100f
        barChart.axisRight.isEnabled = false
        barChart.description.isEnabled = false
        barChart.legend.isEnabled = false
        barChart.invalidate()
    }

    class HistoriqueAdapter(private val data: List<Pair<String, String>>) :
        RecyclerView.Adapter<HistoriqueAdapter.HistoriqueViewHolder>() {

        class HistoriqueViewHolder(val view: ViewGroup) : RecyclerView.ViewHolder(view) {
            val dateText: TextView = view.findViewById(R.id.dateText)
            val statusText: TextView = view.findViewById(R.id.statusText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoriqueViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_historique, parent, false) as ViewGroup
            return HistoriqueViewHolder(view)
        }

        override fun onBindViewHolder(holder: HistoriqueViewHolder, position: Int) {
            val (date, status) = data[position]
            holder.dateText.text = date
            holder.statusText.text = if (status.startsWith("0/0")) "–" else status
        }

        override fun getItemCount() = data.size
    }
}