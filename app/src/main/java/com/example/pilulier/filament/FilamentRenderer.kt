package com.example.pilulier.filament

import android.content.Context
import android.opengl.GLSurfaceView
import android.view.SurfaceView
import com.google.android.filament.utils.ModelViewer
import java.nio.ByteBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import com.google.android.filament.Viewport


class FilamentRenderer(
    private val context: Context,
    private val glbFileName: String
) : GLSurfaceView.Renderer {

    private lateinit var modelViewer: ModelViewer
    private lateinit var surfaceView: SurfaceView

    fun setSurfaceView(surfaceView: SurfaceView) {
        this.surfaceView = surfaceView
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        modelViewer = ModelViewer(surfaceView)

        val inputStream = context.assets.open("models/$glbFileName")
        val buffer: ByteBuffer = ByteBuffer.wrap(inputStream.readBytes())

        modelViewer.loadModelGlb(buffer)
        modelViewer.transformToUnitCube()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        modelViewer.view.viewport = Viewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        modelViewer.render(System.nanoTime())
    }
}
