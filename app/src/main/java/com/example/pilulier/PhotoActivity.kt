package com.example.pilulier

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Bundle
import android.os.Environment
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.File
import java.io.FileOutputStream
import java.util.*

class PhotoActivity : AppCompatActivity() {

    private lateinit var textureView: AutoFitTextureView
    private lateinit var photoPreview: ImageView
    private lateinit var btnBack: Button
    private lateinit var btnCapture: Button

    private lateinit var cameraManager: CameraManager
    private lateinit var cameraId: String
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private lateinit var captureRequestBuilder: CaptureRequest.Builder
    private var previewSize: Size? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_photo)

        textureView = findViewById(R.id.textureView)
        photoPreview = findViewById(R.id.photoPreview)
        btnBack = findViewById(R.id.btnBack)
        btnCapture = findViewById(R.id.btnCapture)

        btnBack.setOnClickListener { finish() }
        btnCapture.setOnClickListener { prendrePhoto() }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), 1)
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        cameraManager = getSystemService(CAMERA_SERVICE) as CameraManager
        cameraId = cameraManager.cameraIdList[0]

        val characteristics = cameraManager.getCameraCharacteristics(cameraId)
        val map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        previewSize = map?.getOutputSizes(SurfaceTexture::class.java)
            ?.maxByOrNull { it.width * it.height }

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                // Définir le bon ratio ici
                previewSize?.let {
                    textureView.setAspectRatio(it.width, it.height)
                }
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    private fun openCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
            return

        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                createPreviewSession()
            }

            override fun onDisconnected(camera: CameraDevice) {
                cameraDevice?.close()
                cameraDevice = null
            }

            override fun onError(camera: CameraDevice, error: Int) {
                cameraDevice?.close()
                cameraDevice = null
            }
        }, null)
    }

    private fun createPreviewSession() {
        val surfaceTexture = textureView.surfaceTexture ?: return
        previewSize?.let {
            surfaceTexture.setDefaultBufferSize(it.width, it.height)
        }

        val surface = Surface(surfaceTexture)

        captureRequestBuilder = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW).apply {
            addTarget(surface)
        }

        cameraDevice?.createCaptureSession(listOf(surface), object : CameraCaptureSession.StateCallback() {
            override fun onConfigured(session: CameraCaptureSession) {
                captureSession = session
                captureSession?.setRepeatingRequest(captureRequestBuilder.build(), null, null)
            }

            override fun onConfigureFailed(session: CameraCaptureSession) {
                Toast.makeText(this@PhotoActivity, "Erreur d'affichage caméra", Toast.LENGTH_SHORT).show()
            }
        }, null)
    }

    private fun prendrePhoto() {
        val bitmap = textureView.bitmap ?: return
        photoPreview.setImageBitmap(bitmap)
        photoPreview.visibility = ImageView.VISIBLE

        val filename = "photo_${System.currentTimeMillis()}.jpg"
        val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), filename)
        FileOutputStream(file).use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it)
        }

        Toast.makeText(this, "Photo enregistrée avec succès", Toast.LENGTH_SHORT).show()
    }

    private fun stopCamera() {
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
    }

    override fun onPause() {
        super.onPause()
        stopCamera()
    }

    override fun onResume() {
        super.onResume()
        if (textureView.isAvailable) {
            openCamera()
        } else {
            startCamera()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        }
    }
}
