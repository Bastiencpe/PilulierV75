package com.example.pilulier

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import kotlin.random.Random

class InfoActivity : AppCompatActivity() {

    private lateinit var btnFuyant: Button
    private lateinit var rootLayout: ConstraintLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_info)


        val btnBack = findViewById<ImageButton>(R.id.btnBackInfo)
        btnFuyant = findViewById(R.id.btnFuyant)
        rootLayout = findViewById(R.id.info_main)

        btnBack.setOnClickListener { finish() }

        btnFuyant.setOnClickListener {
            btnFuyant.visibility = View.INVISIBLE

            rootLayout.post {
                val maxX = rootLayout.width - btnFuyant.width
                val maxY = rootLayout.height - btnFuyant.height

                val randomX = Random.nextInt(0, maxX)
                val randomY = Random.nextInt(0, maxY)

                val constraintSet = ConstraintSet()
                constraintSet.clone(rootLayout)

                constraintSet.connect(
                    btnFuyant.id,
                    ConstraintSet.START,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.START,
                    randomX
                )
                constraintSet.connect(
                    btnFuyant.id,
                    ConstraintSet.TOP,
                    ConstraintSet.PARENT_ID,
                    ConstraintSet.TOP,
                    randomY
                )

                constraintSet.applyTo(rootLayout)
                btnFuyant.visibility = View.VISIBLE
            }
        }
    }
}
