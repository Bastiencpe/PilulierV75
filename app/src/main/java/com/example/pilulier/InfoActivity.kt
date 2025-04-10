package com.example.pilulier

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class InfoActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)

        val trollSwitch = findViewById<SwitchCompat>(R.id.switchNePasToucher)

        trollSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                // Message drôle
                Toast.makeText(this, "Je t'avais dit de ne pas toucher ! 😜", Toast.LENGTH_SHORT).show()

                // Revenir à OFF après 500 ms
                Handler(Looper.getMainLooper()).postDelayed({
                    trollSwitch.isChecked = false
                }, 500)
            }
        }

        // Bouton retour
        findViewById<ImageButton>(R.id.btnBackInfo).setOnClickListener {
            finish()
        }
    }
}
