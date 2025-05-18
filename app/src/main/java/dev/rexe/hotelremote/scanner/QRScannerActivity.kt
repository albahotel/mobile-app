package dev.rexe.hotelremote.scanner

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.textview.MaterialTextView
import com.google.mlkit.common.sdkinternal.SharedPrefManager
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScanning
import dev.rexe.hotelremote.R
import dev.rexe.hotelremote.analyzers.QRCodeAnalyzer
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import androidx.core.content.edit

class QRScannerActivity : AppCompatActivity() {
    private lateinit var cameraProvider: ProcessCameraProvider
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var barcodeScanner: BarcodeScanner

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_qr_scanner)
        val toolbar = findViewById<MaterialToolbar>(R.id.qr_scanner_toolbar)
        setSupportActionBar(toolbar)
        toolbar.setNavigationOnClickListener { v ->
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(toolbar) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, systemBars.top, 0, 0)
            insets
        }

        WindowCompat.getInsetsController(window, window.decorView).isAppearanceLightStatusBars = false

        cameraExecutor = Executors.newSingleThreadExecutor()
        barcodeScanner = BarcodeScanning.getClient()

        startCamera()
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder()
                .build()
                .also {
                    it.surfaceProvider = findViewById<PreviewView>(R.id.qr_scanner_preview).surfaceProvider
                }

            val imageAnalyzer = ImageAnalysis.Builder()
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, QRCodeAnalyzer { qrCodeValue ->
                        findViewById<MaterialTextView>(R.id.qr_scanner_status).setText(R.string.sts_qr_scanner_validating)
                        if (HotelCodeValidator.validate(qrCodeValue)) {
                            File(applicationContext.filesDir.parent, "shared_prefs/auth.xml").createNewFile()
                            val shr: SharedPreferences = getSharedPreferences("auth", MODE_PRIVATE)
                            shr.edit { putString("keyStroke", qrCodeValue) }
                            val intent: Intent = Intent("finish_activity")
                            sendBroadcast(intent)
                            finish()
                        } else
                            findViewById<MaterialTextView>(R.id.qr_scanner_status).setText(R.string.sts_qr_scanner_invalid)
                    })
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            try {
                cameraProvider.unbindAll()

                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageAnalyzer
                )

            } catch (exc: Exception) {
                Log.e("QR_SCAN", "Ошибка привязки Use Cases CameraX", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

}