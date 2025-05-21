package com.example.pilulier

import android.opengl.GLSurfaceView
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class ModelViewerActivity : AppCompatActivity() {

    private lateinit var glSurfaceView: GLSurfaceView
    private var renderer: FilamentRenderer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val glbFile = intent.getStringExtra("OBJ_FILE")

        if (glbFile.isNullOrBlank()) {
            Toast.makeText(this, "Fichier .glb manquant", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        try {
            glSurfaceView = GLSurfaceView(this).apply {
                setEGLContextClientVersion(3)
                setBackgroundColor(0xFF1E1E1E.toInt()) // fond sombre pour éviter le noir
            }

            renderer = FilamentRenderer(this, glbFile)
            renderer?.setSurfaceView(glSurfaceView)

            glSurfaceView.setRenderer(renderer)
            glSurfaceView.renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY

            setContentView(glSurfaceView)

            Toast.makeText(this, "Chargement de $glbFile", Toast.LENGTH_SHORT).show()
            Log.d("ModelViewerActivity", "Affichage de : $glbFile")

        } catch (e: Exception) {
            Log.e("ModelViewerActivity", "Erreur lors de l'initialisation", e)
            Toast.makeText(this, "Erreur d'affichage du modèle", Toast.LENGTH_LONG).show()
            finish()
        }
    }

    override fun onPause() {
        super.onPause()
        glSurfaceView.onPause()
    }

    override fun onResume() {
        super.onResume()
        glSurfaceView.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        renderer = null
    }
}
