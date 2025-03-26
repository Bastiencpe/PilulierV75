package com.example.pilulier

import android.os.Bundle
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class InfoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        val btnBack = findViewById<ImageButton>(R.id.btnBackInfo)
        btnBack.setOnClickListener {
            finish()
        }
    }
}
