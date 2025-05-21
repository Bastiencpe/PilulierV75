package com.example.pilulier

import android.content.Context
import android.opengl.GLSurfaceView
import android.util.Log
import android.view.SurfaceView
import com.google.android.filament.Engine
import com.google.android.filament.IndirectLight
import com.google.android.filament.Skybox
import com.google.android.filament.Texture
import com.google.android.filament.Viewport
import com.google.android.filament.utils.ModelViewer
import java.nio.ByteBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class FilamentRenderer(private val context: Context, private val glbFileName: String) : GLSurfaceView.Renderer {

    private lateinit var modelViewer: ModelViewer
    private lateinit var surfaceView: SurfaceView

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        Log.d("FilamentRenderer", "onSurfaceCreated")

        modelViewer = ModelViewer(surfaceView)
        val engine = modelViewer.engine

        try {
            val input = context.assets.open("models/$glbFileName").use { it.readBytes() }
            val buffer = ByteBuffer.allocateDirect(input.size).put(input)
            buffer.flip()

            modelViewer.loadModelGlb(buffer)
            modelViewer.transformToUnitCube()

            modelViewer.scene.skybox = Skybox.Builder().build(engine)
            modelViewer.scene.indirectLight = IndirectLight.Builder()
                .reflections(Texture.Builder().build(engine))
                .intensity(30_000.0f)
                .build(engine)

            Log.d("FilamentRenderer", "Modèle chargé avec succès : $glbFileName")

        } catch (e: Exception) {
            Log.e("FilamentRenderer", "Erreur lors du chargement du modèle : $glbFileName", e)
        }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        Log.d("FilamentRenderer", "onSurfaceChanged: $width x $height")
        modelViewer.view.setViewport(Viewport(0, 0, width, height))
    }

    override fun onDrawFrame(gl: GL10?) {
        modelViewer.render(System.nanoTime())
    }

    fun setSurfaceView(surfaceView: SurfaceView) {
        this.surfaceView = surfaceView
    }
}
