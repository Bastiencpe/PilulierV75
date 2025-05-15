package com.example.pilulier

import android.Manifest
import android.content.Intent
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
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import java.io.FileOutputStream
import java.util.*

class PhotoActivity : AppCompatActivity() {

    private lateinit var textureView: AutoFitTextureView
    private lateinit var photoPreview: ImageView
    private lateinit var btnBack: Button
    private lateinit var btnCapture: Button
    private lateinit var resultTextView: TextView

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
        resultTextView = findViewById(R.id.resultTextView)

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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) return

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

        detecterTexteEtForme(bitmap)
        Toast.makeText(this, "Photo enregistrée avec succès", Toast.LENGTH_SHORT).show()
    }

    private fun detecterTexteEtForme(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val texte = visionText.text.ifBlank { "Aucun texte détecté." }

                val nomLigne = visionText.textBlocks
                    .flatMap { it.lines }
                    .map { it.text.trim() }
                    .firstOrNull { it.matches(Regex("^[A-ZÉÈÀÂÇ]{3,}(\\s[A-ZÉÈÀÂÇ]{2,})?\$")) }
                    ?: texte.lines().firstOrNull()?.take(30) ?: "Nom inconnu"

                val texteNettoye = texte.lowercase(Locale.FRANCE).replace("\\s+".toRegex(), " ")

                val frequence = when {
                    Regex("1 ?j(our)? ?sur ?2").containsMatchIn(texteNettoye) -> "1j sur 2"
                    Regex("hebdomadaire|1 fois par semaine").containsMatchIn(texteNettoye) -> "hebdomadaire"
                    Regex("tous les jours|quotidien|chaque jour").containsMatchIn(texteNettoye) -> "quotidien"
                    else -> ""
                }

                val momentsDetectes = mutableListOf<String>()
                if (texteNettoye.contains("matin")) momentsDetectes.add("matin")
                if (texteNettoye.contains("midi")) momentsDetectes.add("midi")
                if (texteNettoye.contains("soir")) momentsDetectes.add("soir")
                val momentsString = momentsDetectes.joinToString(",")

                // 🔍 Appelle ta fonction OpenCV ici
                val forme = detecterForme(bitmap)

                resultTextView.text = "$nomLigne\n$frequence\n$momentsString\nForme : $forme"

                val resultIntent = Intent()
                resultIntent.putExtra("texte_ocr", nomLigne)
                resultIntent.putExtra("frequence_ocr", frequence)
                resultIntent.putExtra("moments_ocr", momentsString)
                resultIntent.putExtra("forme_detectee", forme)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
            .addOnFailureListener { e ->
                resultTextView.text = "Erreur OCR : ${e.message}"
            }
    }

    private fun detecterForme(bitmap: Bitmap): String {
        if (!org.opencv.android.OpenCVLoader.initDebug()) {
            Toast.makeText(this, "OpenCV non initialisé", Toast.LENGTH_SHORT).show()
            return "inconnue"
        }

        val mat = org.opencv.core.Mat()
        org.opencv.android.Utils.bitmapToMat(bitmap, mat)

        val gray = org.opencv.core.Mat()
        org.opencv.imgproc.Imgproc.cvtColor(mat, gray, org.opencv.imgproc.Imgproc.COLOR_BGR2GRAY)
        org.opencv.imgproc.Imgproc.GaussianBlur(gray, gray, org.opencv.core.Size(5.0, 5.0), 0.0)

        val edges = org.opencv.core.Mat()
        org.opencv.imgproc.Imgproc.Canny(gray, edges, 75.0, 200.0)

        val contours = mutableListOf<org.opencv.core.MatOfPoint>()
        val hierarchy = org.opencv.core.Mat()
        org.opencv.imgproc.Imgproc.findContours(
            edges, contours, hierarchy,
            org.opencv.imgproc.Imgproc.RETR_TREE,
            org.opencv.imgproc.Imgproc.CHAIN_APPROX_SIMPLE
        )

        for (contour in contours) {
            val approx = org.opencv.core.MatOfPoint2f()
            val contour2f = org.opencv.core.MatOfPoint2f(*contour.toArray())
            val peri = org.opencv.imgproc.Imgproc.arcLength(contour2f, true)
            org.opencv.imgproc.Imgproc.approxPolyDP(contour2f, approx, 0.04 * peri, true)

            val vertices = approx.toArray().size

            return when (vertices) {
                3 -> "triangle"
                4 -> "rectangle"
                in 5..8 -> "polygone"
                else -> "cercle"
            }
        }

        return "inconnue"
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
