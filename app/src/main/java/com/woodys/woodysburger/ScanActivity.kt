package com.woodys.woodysburger

import com.google.android.material.snackbar.Snackbar
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
// Add this import

class ScanActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private lateinit var cameraExecutor: ExecutorService
    private val cameraPermissionRequestCode = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                cameraPermissionRequestCode
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraPermissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera()
            } else {
                Snackbar.make(
                    findViewById(android.R.id.content),
                    "Camera permission is required to scan QR codes",
                    Snackbar.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(findViewById<PreviewView>(R.id.viewFinder).surfaceProvider)
            }

            val imageAnalyzer = ImageAnalysis.Builder()
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor) { imageProxy ->
                        processImageProxy(imageProxy)
                    }
                }

            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalyzer)
            } catch (exc: Exception) {
                Log.e("ScanActivity", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val scanner = BarcodeScanning.getClient()
            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue ?: ""
                        handleScanResult(rawValue)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ScanActivity", "Barcode scanning failed: $e")
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        }
    }

    private fun handleScanResult(rawValue: String) {
        val lastEight = extractLastEightCharacters(rawValue)
        if (lastEight.isNotEmpty()) {
            checkCodeValidity(lastEight)
        } else {
            Snackbar.make(
                findViewById(android.R.id.content),
                "Invalid QR code",
                Snackbar.LENGTH_SHORT
            ).show()
        }
    }

    private fun checkCodeValidity(code: String) {
        val url = "https://api.woodysburger.hu/api/codes/$code"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ScanActivity", "Request failed: $e")
                runOnUiThread {
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Failed to validate code",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string() ?: ""
                    try {
                        val jsonResponse = JSONObject(responseData)
                        val scanned = jsonResponse.optInt("scanned", -1)

                        if (scanned == 0) {
                            runOnUiThread {
                                Snackbar.make(
                                    findViewById(android.R.id.content),
                                    "Code is valid",
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                            updateCodeAsScanned(code)
                        } else {
                            runOnUiThread {
                                Snackbar.make(
                                    findViewById(android.R.id.content),
                                    "Code has already been used",
                                    Snackbar.LENGTH_LONG
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ScanActivity", "Error parsing response: $e")
                        runOnUiThread {
                            Snackbar.make(
                                findViewById(android.R.id.content),
                                "Error processing response",
                                Snackbar.LENGTH_LONG
                            ).show()
                        }
                    }
                } else {
                    runOnUiThread {
                        Snackbar.make(
                            findViewById(android.R.id.content),
                            "Failed to validate code",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
        })
    }

    private fun updateCodeAsScanned(code: String) {
        val url = "https://api.woodysburger.hu/api/codes/$code"
        val requestBody = JSONObject().apply {
            put("scanned", 1)
        }

        val request = Request.Builder()
            .url(url)
            .put(requestBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ScanActivity", "Failed to update scanned status: $e")
                runOnUiThread {
                    Snackbar.make(
                        findViewById(android.R.id.content),
                        "Failed to update code status",
                        Snackbar.LENGTH_LONG
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    runOnUiThread {
                        Snackbar.make(
                            findViewById(android.R.id.content),
                            "Code successfully updated",
                            Snackbar.LENGTH_LONG
                        ).show()
                        finish() // Close ScanActivity after success
                    }
                } else {
                    runOnUiThread {
                        Snackbar.make(
                            findViewById(android.R.id.content),
                            "Failed to update code status",
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
                }
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    private fun extractLastEightCharacters(url: String): String {
        return if (url.length >= 8) url.takeLast(8) else ""
    }
}
