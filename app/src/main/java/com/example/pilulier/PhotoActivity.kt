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
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc
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

                val texteNettoye = texte.lowercase(Locale.FRENCH).replace("\\s+".toRegex(), " ")

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

                val (forme, couleur) = detecterForme(bitmap)

                resultTextView.text = "$nomLigne\n$frequence\n$momentsString\nForme : $forme\nR: ${couleur.first}, G: ${couleur.second}, B: ${couleur.third}"

                val resultIntent = Intent()
                resultIntent.putExtra("texte_ocr", nomLigne)
                resultIntent.putExtra("frequence_ocr", frequence)
                resultIntent.putExtra("moments_ocr", momentsString)
                resultIntent.putExtra("forme_detectee", forme)
                resultIntent.putExtra("couleur_r", couleur.first)
                resultIntent.putExtra("couleur_g", couleur.second)
                resultIntent.putExtra("couleur_b", couleur.third)
                setResult(RESULT_OK, resultIntent)
                finish()
            }
            .addOnFailureListener { e ->
                resultTextView.text = "Erreur OCR : ${e.message}"
            }
    }

    private fun detecterForme(bitmap: Bitmap): Pair<String, Triple<Double, Double, Double>> {
        if (!OpenCVLoader.initDebug()) return "inconnue" to Triple(0.0, 0.0, 0.0)

        val img = Mat()
        Utils.bitmapToMat(bitmap, img)

        val hsv = Mat()
        Imgproc.cvtColor(img, hsv, Imgproc.COLOR_BGR2HSV)
        Imgproc.GaussianBlur(hsv, hsv, Size(9.0, 9.0), 4.0)

        val hsvChannel = Mat()
        Core.extractChannel(hsv, hsvChannel, 0)

        val thresholded = Mat()
        Imgproc.threshold(hsvChannel, thresholded, 50.0, 255.0, Imgproc.THRESH_BINARY)

        val edges = Mat()
        Imgproc.Canny(thresholded, edges, 0.0, 100.0, 5)

        val dst = Mat()
        Imgproc.cornerHarris(edges, dst, 10, 31, 0.04)

        val test = Mat.zeros(dst.size(), CvType.CV_8U)
        Core.compare(dst, Scalar(0.2 * Core.minMaxLoc(dst).maxVal), test, Core.CMP_GT)

        val rows = test.rows()
        val cols = test.cols()

        for (i in 0 until rows) {
            for (j in 0 until cols) {
                if (test.get(i, j)[0] == 255.0) {
                    for (k in -30..30) {
                        for (l in -30..30) {
                            if ((k != 0 || l != 0) && (i + k in 0 until rows) && (j + l in 0 until cols)) {
                                test.put(i + k, j + l, 0.0)
                            }
                        }
                    }
                }
            }
        }

        val nonZeroCount = Core.countNonZero(test)

        if (nonZeroCount == 3 || nonZeroCount == 4) {
            val forme = if (nonZeroCount == 3) "triangle" else "rectangle"

            val nonZeroPoints = MatOfPoint()
            Core.findNonZero(test, nonZeroPoints)

            var totalR = 0.0
            var totalG = 0.0
            var totalB = 0.0

            for (i in 0 until nonZeroCount) {
                val point = nonZeroPoints.get(i, 0)
                val x = point[1].toInt().coerceIn(0, img.rows() - 1)
                val y = point[0].toInt().coerceIn(0, img.cols() - 1)

                val pixel = img.get(x, y)
                totalB += pixel[0]
                totalG += pixel[1]
                totalR += pixel[2]
            }

            val avgR = totalR / nonZeroCount
            val avgG = totalG / nonZeroCount
            val avgB = totalB / nonZeroCount

            return forme to Triple(avgR, avgG, avgB)
        } else {
            val circles = Mat()
            Imgproc.HoughCircles(
                edges, circles, Imgproc.HOUGH_GRADIENT, 1.0, img.rows() / 8.0,
                200.0, 20.0, 50, 300
            )
            if (!circles.empty()) {
                val circleData = circles.get(0, 0)
                val center = Point(circleData[0], circleData[1])
                val color = img.get(center.y.toInt() % img.rows(), center.x.toInt() % img.cols())
                return "cercle" to Triple(color[2], color[1], color[0]) // RGB = BGR reverse
            }
        }

        return "inconnue" to Triple(0.0, 0.0, 0.0)
    }

    override fun onPause() {
        super.onPause()
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
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
