package com.woodys.woodysburger

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class ScanActivity : AppCompatActivity() {

    private val client = OkHttpClient()
    private lateinit var cameraExecutor: ExecutorService
    private val cameraPermissionRequestCode = 101

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Check if the camera permission is granted
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startCamera() // Start the camera preview and analysis
        } else {
            // Request camera permission if not granted
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                cameraPermissionRequestCode
            )
        }
    }

    // Handle the result of permission request
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == cameraPermissionRequestCode) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera() // Start camera if permission is granted
            } else {
                Toast.makeText(this, "Camera permission is required to scan QR codes", Toast.LENGTH_SHORT).show()
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

    private fun extractLastEightCharacters(url: String): String {
        return if (url.length >= 8) url.takeLast(8) else ""
    }

    private fun handleScanResult(rawValue: String) {
        Log.d("ScanActivity", "QR Code Scanned: $rawValue")

        val lastEight = extractLastEightCharacters(rawValue)
        if (lastEight.isNotEmpty()) {
            checkCodeValidity(lastEight) // Check if the extracted last 8 characters are valid
        } else {
            Toast.makeText(this, "Invalid QR code", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkCodeValidity(code: String) {
        val url = "https://api.woodysburger.hu/api/codes/$code" // API URL with the scanned code

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ScanActivity", "Request failed: $e")
                runOnUiThread {
                    Toast.makeText(this@ScanActivity, "Failed to validate code", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string() ?: ""
                    try {
                        val jsonResponse = JSONObject(responseData)
                        val scanned = jsonResponse.optInt("scanned", -1)

                        if (scanned == 0) {
                            // Code is valid and not scanned yet
                            Log.d("ScanActivity", "Code is valid and not scanned yet")
                            runOnUiThread {
                                Toast.makeText(this@ScanActivity, "Code is valid", Toast.LENGTH_SHORT).show()
                            }

                            // Now, update the scanned status to 1 to mark it as used
                            updateCodeAsScanned(code)
                        } else {
                            // Code has already been scanned
                            Log.d("ScanActivity", "Code has already been scanned")
                            runOnUiThread {
                                Toast.makeText(this@ScanActivity, "Code has already been used", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("ScanActivity", "Error parsing response: $e")
                        runOnUiThread {
                            Toast.makeText(this@ScanActivity, "Error processing response", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Log.e("ScanActivity", "Failed to validate code: ${response.message}")
                    runOnUiThread {
                        Toast.makeText(this@ScanActivity, "Failed to validate code", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun updateCodeAsScanned(code: String) {
        val url = "https://api.woodysburger.hu/api/codes/$code" // Correct API endpoint

        // Create the request body with the scanned field set to 1
        val requestBody = JSONObject().apply {
            put("scanned", 1)
        }

        val request = Request.Builder()
            .url(url)
            .put(requestBody.toString().toRequestBody("application/json".toMediaTypeOrNull())) // Changed to PUT
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("ScanActivity", "Failed to update scanned status: $e")
                runOnUiThread {
                    Toast.makeText(this@ScanActivity, "Failed to update code status", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    Log.d("ScanActivity", "Code status updated to scanned")
                    runOnUiThread {
                        Toast.makeText(this@ScanActivity, "Code has been successfully updated", Toast.LENGTH_SHORT).show()
                    }
                    // Optionally, perform further actions, such as awarding points or saving data
                } else {
                    Log.e("ScanActivity", "Failed to update code status: ${response.message}")
                    runOnUiThread {
                        Toast.makeText(this@ScanActivity, "Failed to update code status", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }



    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }
}
