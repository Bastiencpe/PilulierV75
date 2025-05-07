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
import org.opencv.android.OpenCVLoader
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc


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

        detecterTexte(bitmap)
        Toast.makeText(this, "Photo enregistrée avec succès", Toast.LENGTH_SHORT).show()
    }

    private fun detecterTexte(bitmap: Bitmap) {
        val image = InputImage.fromBitmap(bitmap, 0)
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                resultTextView.text = visionText.text.ifBlank { "Aucun texte détecté." }
            }
            .addOnFailureListener { e ->
                resultTextView.text = "Erreur de détection : ${e.message}"
            }
    }

    private fun stopCamera() {
        captureSession?.close()
        captureSession = null
        cameraDevice?.close()
        cameraDevice = null
    }


    public fun detect_forme(bitmap: Bitmap){
        if (OpenCVLoader.initDebug()) {
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
                    if (test.get(i, j)[0] == 255.0) { // Vérifiez si la valeur est 255
                        for (k in -30..30) {
                            for (l in -30..30) {
                                if (k != 0 || l != 0) {
                                    // Assurez-vous que les indices sont dans les limites
                                    if (i + k in 0 until test.rows() && j + l in 0 until test.cols()) {
                                        test.put(i + k, j + l, 0.0) // Mettre à jour la valeur à 0
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val nonZeroCount = Core.countNonZero(test)
            println("Nombre de coins: $nonZeroCount")

            if (nonZeroCount == 3 || nonZeroCount == 4){
                if (nonZeroCount == 3){
                    println("Forme : triangle")
                }
                if (nonZeroCount == 4){
                    println("Forme : Rectangle")
                }

                val nonZeroPoints = MatOfPoint()
                Core.findNonZero(test, nonZeroPoints)

                var totalR = 0.0
                var totalG = 0.0
                var totalB = 0.0

                for (i in 0 until nonZeroCount) {
                    val point = nonZeroPoints.get(i, 0)
                    val x = point[1].toInt()
                    val y = point[0].toInt()

                    // Accéder aux valeurs RGB à l'indice (x, y)
                    val pixel = img.get(x, y)
                    totalB += pixel[0]
                    totalG += pixel[1]
                    totalR += pixel[2]
                }

                val avgR = totalR / nonZeroCount
                val avgG = totalG / nonZeroCount
                val avgB = totalB / nonZeroCount

                println("Moyenne des valeurs RGB aux indices non nuls :")
                println("R: $avgR, G: $avgG, B: $avgB")
            }

            else {

                val circles = Mat()
                Imgproc.HoughCircles(
                    edges, circles, Imgproc.HOUGH_GRADIENT, 1.0, img.rows() / 8.0,
                    200.0, 10.0, 50, 300
                )

                if (!circles.empty()) {
                    val circledata = circles.get(0, 0)
                    val center = Point(circledata[0], circledata[1])
                    val color = img.get(center.y.toInt() % img.rows(), center.x.toInt() % img.cols())

                    val avgR = color[0]
                    val avgG = color[1]
                    val avgB = color[2]

                    println("Forme : Cercle")
                    println("R: $avgR, G: $avgG, B: $avgB")
                }
            }
        }
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
