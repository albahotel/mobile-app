package dev.rexe.hotelremote.analyzers

import android.annotation.SuppressLint
import android.util.Log
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage

class QRCodeAnalyzer(private val listener: (String) -> Unit) : ImageAnalysis.Analyzer {
    private val barcodeScanner = BarcodeScanning.getClient()

    @OptIn(ExperimentalGetImage::class)
    @SuppressLint("UnsafeOptInUsageConflicts")
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image

        if (mediaImage != null) {
            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)

            barcodeScanner.process(image)
                .addOnSuccessListener { barcodes ->
                    if (barcodes.isNotEmpty()) {
                        val qrCodeValue = barcodes.first().rawValue
                        if (qrCodeValue != null) {
                            listener(qrCodeValue)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("BarcodeAnalyzer", "Ошибка сканирования штрих-кода/QR-кода", e)
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    fun close() {
        barcodeScanner.close()
    }
}