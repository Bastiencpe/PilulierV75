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

                // Ici, associe forme à nom médicament
                val nomMedicament = when (forme) {
                    "rectangle", "carré" -> "Doliprane"
                    "triangle" -> "Aspirine"     // Exemple
                    "rond" -> "Paracetamol"      // Exemple
                    else -> nomLigne             // Texte OCR sinon
                }

                resultTextView.text =
                    "Nom : $nomMedicament\nFréquence : $frequence\nDurée : $duree\nMoments : $momentsString\nForme : $forme\nR: ${couleur.first}, G: ${couleur.second}, B: ${couleur.third}"

                retournerResultats(nomMedicament, frequence, momentsString, forme,
                    couleur.first, couleur.second, couleur.third)
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

        val src = Mat()
        Utils.bitmapToMat(bitmap, src)

        val gray = Mat()
        Imgproc.cvtColor(src, gray, Imgproc.COLOR_RGBA2GRAY)
        Imgproc.GaussianBlur(gray, gray, Size(5.0, 5.0), 0.0)
        Imgproc.Canny(gray, gray, 50.0, 150.0)

        val contours = mutableListOf<MatOfPoint>()
        val hierarchy = Mat()
        Imgproc.findContours(gray, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE)

        var formeDetectee = "inconnue"
        var couleur = Triple(0.0, 0.0, 0.0)

        val output = Mat()
        Imgproc.cvtColor(gray, output, Imgproc.COLOR_GRAY2BGR)

        for (contour in contours) {
            val peri = Imgproc.arcLength(MatOfPoint2f(*contour.toArray()), true)
            val approx = MatOfPoint2f()
            Imgproc.approxPolyDP(MatOfPoint2f(*contour.toArray()), approx, 0.01 * peri, true)

            val pts = approx.toArray()
            val m = Imgproc.moments(contour)
            val cx = (m.m10 / m.m00).toInt()
            val cy = (m.m01 / m.m00).toInt()

            val formName = when (pts.size) {
                3 -> "triangle"
                4 -> {
                    // Vérifie si c’est un rectangle (en testant les angles ou aspect ratio)
                    val rect = Imgproc.boundingRect(MatOfPoint(*pts))
                    val ar = rect.width.toFloat() / rect.height.toFloat()
                    if (ar in 0.8..1.2) "carré" else "rectangle"
                }
                else -> {
                    // Vérifie si la forme est un cercle
                    val area = Imgproc.contourArea(contour)
                    val circularity = 4 * Math.PI * area / (peri * peri)
                    if (circularity > 0.8) "rond" else "inconnue"
                }

            }



            if (formName != "inconnue") {
                // Dessine le contour et le nom
                Imgproc.drawContours(src, listOf(MatOfPoint(*pts)), -1, Scalar(0.0, 255.0, 0.0), 5)
                Imgproc.putText(src, formName, Point(cx.toDouble(), cy.toDouble()), Imgproc.FONT_HERSHEY_SIMPLEX, 1.0, Scalar(255.0, 0.0, 0.0), 2)

                // Prendre couleur au centre de la forme détectée
                couleur = getCouleurAuCentre(src, cx, cy)
                formeDetectee = formName
                break  // On arrête après la première forme détectée valide
            }
        }

        // Convertir Mat annoté → Bitmap et afficher
        val annotatedBitmap = Bitmap.createBitmap(src.cols(), src.rows(), Bitmap.Config.ARGB_8888)
        Utils.matToBitmap(src, annotatedBitmap)
        runOnUiThread {
            photoPreview.setImageBitmap(annotatedBitmap)
        }

        // Libération mémoire
        src.release()
        gray.release()
        hierarchy.release()

        return formeDetectee to couleur
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
    private fun retournerResultats(
        texteOcr: String,
        frequence: String,
        moments: String,
        forme: String,
        couleurR: Double,
        couleurG: Double,
        couleurB: Double
    ) {
        val resultIntent = Intent()
        resultIntent.putExtra("texte_ocr", texteOcr)
        resultIntent.putExtra("frequence_ocr", frequence)
        resultIntent.putExtra("moments_ocr", moments)
        resultIntent.putExtra("forme_detectee", forme)
        resultIntent.putExtra("couleur_r", couleurR)
        resultIntent.putExtra("couleur_g", couleurG)
        resultIntent.putExtra("couleur_b", couleurB)

        setResult(RESULT_OK, resultIntent)
        finish()  // Ferme l'activité et retourne les données
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



