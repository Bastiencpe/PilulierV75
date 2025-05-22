package com.example.pilulier

import org.opencv.core.Point
import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.*
import android.graphics.SurfaceTexture
import android.hardware.camera2.*
import android.os.Bundle
import android.os.Environment
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.widget.*
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

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
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
        previewSize = map?.getOutputSizes(SurfaceTexture::class.java)?.maxByOrNull { it.width * it.height }

        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
                previewSize?.let {
                    textureView.setAspectRatio(it.width, it.height)
                    configureTransform()
                }
                openCamera()
            }

            override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean = true
            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
        }
    }

    private fun openCamera() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) return
        cameraManager.openCamera(cameraId, object : CameraDevice.StateCallback() {
            override fun onOpened(camera: CameraDevice) {
                cameraDevice = camera
                createPreviewSession()
            }

            override fun onDisconnected(camera: CameraDevice) {
                camera.close()
                cameraDevice = null
            }

            override fun onError(camera: CameraDevice, error: Int) {
                camera.close()
                cameraDevice = null
            }
        }, null)
    }

    private fun createPreviewSession() {
        val surfaceTexture = textureView.surfaceTexture ?: return
        previewSize?.let { surfaceTexture.setDefaultBufferSize(it.width, it.height) }
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

        // Sauvegarde de la photo
        val filename = "photo_${System.currentTimeMillis()}.jpg"
        val file = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), filename)
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 100, it) }

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

                val frequence = Regex("(\\d+ ?j(our)? ?sur ?\\d+)").find(texteNettoye)?.value ?: ""
                val duree = Regex("pendant ?\\d+ ?jours").find(texteNettoye)?.value ?: ""

                val moments = listOf("matin", "midi", "soir").filter { texteNettoye.contains(it) }
                val momentsString = moments.joinToString(", ")

                val (forme, couleur) = detecterFormeAvecLogiqueCollegue(bitmap)

                resultTextView.text = "Nom : $nomLigne\nFréquence : $frequence\nDurée : $duree\nMoments : $momentsString\nForme : $forme\nR: ${couleur.first}, G: ${couleur.second}, B: ${couleur.third}"

                val resultIntent = Intent().apply {
                    putExtra("texte_ocr", nomLigne)
                    putExtra("frequence_ocr", frequence)
                    putExtra("duree_ocr", duree)
                    putExtra("moments_ocr", momentsString)
                    putExtra("forme_detectee", forme)
                    putExtra("couleur_r", couleur.first)
                    putExtra("couleur_g", couleur.second)
                    putExtra("couleur_b", couleur.third)
                }
                setResult(RESULT_OK, resultIntent)
                finish()
            }
            .addOnFailureListener { e ->
                resultTextView.text = "Erreur OCR : ${e.message}"
            }
    }

    /**
     * Détection de forme inspirée de la logique Python de ton collègue
     */
    private fun detecterFormeAvecLogiqueCollegue(bitmap: Bitmap): Pair<String, Triple<Double, Double, Double>> {
        if (!OpenCVLoader.initDebug()) {
            return "inconnue" to Triple(0.0, 0.0, 0.0)
        }

        // Conversion Bitmap -> Mat OpenCV
        val src = Mat()
        Utils.bitmapToMat(bitmap, src)
        Imgproc.cvtColor(src, src, Imgproc.COLOR_RGBA2BGR)

        // Convertir en RGB (similaire à python)
        val srcRgb = Mat()
        Imgproc.cvtColor(src, srcRgb, Imgproc.COLOR_BGR2RGB)

        // Conversion en HSV et floutage
        val hsv = Mat()
        Imgproc.cvtColor(srcRgb, hsv, Imgproc.COLOR_RGB2HSV)
        Imgproc.GaussianBlur(hsv, hsv, Size(9.0, 9.0), 4.0)

        // Seuillage sur la teinte (H channel)
        val hChannel = Mat()
        Core.extractChannel(hsv, hChannel, 0)
        val seuillage = Mat()
        Imgproc.threshold(hChannel, seuillage, 50.0, 255.0, Imgproc.THRESH_BINARY)

        // Détection des contours sur l'image seuillée
        val edges = Mat()
        Imgproc.Canny(seuillage, edges, 0.0, 100.0, 5, false)

        // Détection de coins avec cornerHarris
        val dst = Mat()
        Imgproc.cornerHarris(edges, dst, 10, 31, 0.04)

        val dstNorm = Mat()
        Core.normalize(dst, dstNorm, 0.0, 255.0, Core.NORM_MINMAX)

        val test = Mat.zeros(dst.size(), CvType.CV_8U)

        val threshold = 0.2 * Core.minMaxLoc(dst).maxVal

        // Seuillage des coins détectés
        for (i in 0 until dst.rows()) {
            for (j in 0 until dst.cols()) {
                if (dst.get(i, j)[0] > threshold) {
                    test.put(i, j, 255.0)
                }
            }
        }

        // Nettoyage des coins proches (non maximal suppression simple)
        val mask = Mat.zeros(test.size(), CvType.CV_8U)
        for (i in 0 until test.rows()) {
            for (j in 0 until test.cols()) {
                if (test.get(i, j)[0] == 255.0) {
                    for (k in -30..30) {
                        for (l in -30..30) {
                            val x = i + k
                            val y = j + l
                            if ((k != 0 || l != 0) && x in 0 until test.rows() && y in 0 until test.cols()) {
                                mask.put(x, y, 0.0)
                            }
                        }
                    }
                    mask.put(i, j, 255.0)
                }
            }
        }

        // Récupérer les points finaux de coin
        val coinPoints = mutableListOf<Point>()
        for (i in 0 until mask.rows()) {
            for (j in 0 until mask.cols()) {
                if (mask.get(i, j)[0] == 255.0) {
                    coinPoints.add(Point(j.toDouble(), i.toDouble()))
                }
            }
        }

        // Détection de cercles via HoughCircles
        val circles = Mat()
        Imgproc.HoughCircles(
            edges,
            circles,
            Imgproc.HOUGH_GRADIENT,
            1.0,
            edges.rows() / 8.0,
            200.0,
            20.0,
            50,
            500
        )

        var forme = "Rien"
        var couleur = Triple(0.0, 0.0, 0.0)

        when {
            circles.cols() > 0 -> {
                // Cercle détecté
                forme = "rond"
                val circleVec = circles.get(0, 0)
                if (circleVec != null && circleVec.size >= 3) {
                    val centerX = circleVec[0].toInt()
                    val centerY = circleVec[1].toInt()
                    couleur = getCouleurAuCentre(srcRgb, centerX, centerY)
                }
            }
            coinPoints.size == 3 -> {
                forme = "triangle"
                couleur = getCouleurAuCentre(srcRgb, coinPoints[0].x.toInt(), coinPoints[0].y.toInt())
            }
            coinPoints.size in 4..5 -> {
                forme = "rectangle"
                couleur = getCouleurAuCentre(srcRgb, coinPoints[0].x.toInt(), coinPoints[0].y.toInt())
            }
            coinPoints.size == 12 -> {
                forme = "étoile"
                couleur = getCouleurAuCentre(srcRgb, coinPoints[0].x.toInt(), coinPoints[0].y.toInt())
            }
            else -> {
                forme = "forme complexe"
                if (coinPoints.isNotEmpty()) {
                    couleur = getCouleurAuCentre(srcRgb, coinPoints[0].x.toInt(), coinPoints[0].y.toInt())
                }
            }
        }

        // Libération Mats
        src.release()
        srcRgb.release()
        hsv.release()
        hChannel.release()
        seuillage.release()
        edges.release()
        dst.release()
        dstNorm.release()
        test.release()
        mask.release()
        circles.release()

        return forme to couleur
    }

    private fun getCouleurAuCentre(mat: Mat, x: Int, y: Int): Triple<Double, Double, Double> {
        val cols = mat.cols()
        val rows = mat.rows()
        val cx = x.coerceIn(0, cols - 1)
        val cy = y.coerceIn(0, rows - 1)
        val color = mat.get(cy, cx)
        return if (color != null && color.size >= 3) {
            Triple(color[0], color[1], color[2]) // R,G,B dans srcRgb
        } else {
            Triple(0.0, 0.0, 0.0)
        }
    }

    private fun configureTransform() {
        val rotation = windowManager.defaultDisplay.rotation
        val matrix = Matrix()

        val viewRect = RectF(0f, 0f, textureView.width.toFloat(), textureView.height.toFloat())
        val bufferRect = RectF(0f, 0f, previewSize!!.height.toFloat(), previewSize!!.width.toFloat())
        val centerX = viewRect.centerX()
        val centerY = viewRect.centerY()

        bufferRect.offset(centerX - bufferRect.centerX(), centerY - bufferRect.centerY())
        matrix.setRectToRect(viewRect, bufferRect, Matrix.ScaleToFit.FILL)

        val scale = Math.max(
            textureView.height.toFloat() / previewSize!!.height,
            textureView.width.toFloat() / previewSize!!.width
        )
        matrix.postScale(scale, scale, centerX, centerY)

        // Correction de la rotation
        val rotationDegrees = when (rotation) {
            Surface.ROTATION_0 -> 0
            Surface.ROTATION_90 -> 0
            Surface.ROTATION_180 -> 270
            Surface.ROTATION_270 -> 180
            else -> 0
        }
        matrix.postRotate(rotationDegrees.toFloat(), centerX, centerY)

        textureView.setTransform(matrix)
    }
    }



