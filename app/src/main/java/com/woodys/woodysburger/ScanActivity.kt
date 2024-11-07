package com.woodys.woodysburger

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.barcode.Barcode
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ScanActivity : AppCompatActivity() {

    private lateinit var cameraExecutor: ExecutorService
    private lateinit var webView: WebView
    private lateinit var userId: String
    private val firestore = FirebaseFirestore.getInstance()
    private var isScanning = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        // Enable WebView debugging
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            WebView.setWebContentsDebuggingEnabled(true)
        }

        webView = findViewById(R.id.webView)
        setupWebView()

        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        userId = currentUser.uid
        requestCameraPermission()
        cameraExecutor = Executors.newSingleThreadExecutor()
        startCamera()
    }

    private fun requestCameraPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
                if (isGranted) {
                    startCamera()
                } else {
                    Log.e("ScanActivity", "Camera permission not granted")
                }
            }.launch(Manifest.permission.CAMERA)
        } else {
            startCamera()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener(Runnable {
            val cameraProvider = cameraProviderFuture.get()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(findViewById<PreviewView>(R.id.preview_view).surfaceProvider)
            }
            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

            val imageAnalysis = ImageAnalysis.Builder()
                .setTargetResolution(android.util.Size(1280, 720))
                .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                .build()
                .also {
                    it.setAnalyzer(cameraExecutor, ImageAnalysis.Analyzer { imageProxy ->
                        processImageProxy(imageProxy)
                    })
                }

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageAnalysis)
            } catch (exc: Exception) {
                Log.e("ScanActivity", "Use case binding failed", exc)
            }
        }, ContextCompat.getMainExecutor(this))
    }

    @OptIn(ExperimentalGetImage::class)
    private fun processImageProxy(imageProxy: ImageProxy) {
        if (!isScanning) {
            imageProxy.close()
            return
        }

        val mediaImage = imageProxy.image
        if (mediaImage != null) {
            val inputImage = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            val options = BarcodeScannerOptions.Builder().setBarcodeFormats(Barcode.FORMAT_QR_CODE).build()
            val scanner = BarcodeScanning.getClient(options)
            scanner.process(inputImage)
                .addOnSuccessListener { barcodes ->
                    for (barcode in barcodes) {
                        val rawValue = barcode.rawValue ?: ""
                        handleScanResult(rawValue)
                        isScanning = false
                        break
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("ScanActivity", "Barcode scanning failed: $e")
                }
                .addOnCompleteListener {
                    imageProxy.close()
                }
        } else {
            imageProxy.close()
        }
    }

    private fun handleScanResult(code: String) {
        Log.d("ScanActivity", "QR Code Scanned: $code")
        if (isValidUrl(code)) {
            webView.visibility = WebView.VISIBLE
            webView.loadUrl(code)
        } else {
            Log.e("ScanActivity", "Invalid QR Code URL: $code")
            isScanning = true // Allow new scans after an invalid scan
        }
    }

    private fun setupWebView() {
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = object : WebViewClient() {
            override fun onPageFinished(view: WebView, url: String) {
                view.postDelayed({
                    view.evaluateJavascript(
                        "(function() { " +
                                "var totalAmountElement = document.querySelector('#root > div > div.w-\\[100vw\\].h-\\[100vh\\].overflow-hidden.flex.flex-col > div.grow.overflow-hidden.flex.flex-col > div > div > div:nth-child(6) > p');" +
                                "var totalAmount = totalAmountElement ? totalAmountElement.innerText : '0';" +
                                "totalAmount;" +
                                "})()"
                    ) { totalAmount ->
                        // Extract numeric value from the returned string
                        processTotalAmount(totalAmount.replace("\"", "").replace(" Ft", "").trim())
                    }
                }, 2000)
            }
        }
    }

    private fun processTotalAmount(amount: String) {
        val spentAmount = amount.toIntOrNull() ?: 0
        if (spentAmount > 0) {
            awardPoints(spentAmount)
        } else {
            Log.e("ScanActivity", "Invalid amount: $amount")
            isScanning = true
        }
    }

    private fun awardPoints(spentAmount: Int) {
        CoroutineScope(Dispatchers.Main).launch {
            val points = spentAmount / 100
            if (points > 0) {
                updateUserPoints(userId, points)
            } else {
                Log.d("ScanActivity", "No points to award for spent amount: $spentAmount")
                isScanning = true // Reset scanning state if no points are awarded
            }
        }
    }

    private fun updateUserPoints(userId: String, points: Int) {
        val userRef = firestore.collection("users").document(userId)
        userRef.get().addOnSuccessListener { document ->
            val currentPoints = document.getLong("points") ?: 0
            val updatedPoints = currentPoints + points
            userRef.update("points", updatedPoints)
                .addOnSuccessListener {
                    Log.d("ScanActivity", "User points updated: $updatedPoints")
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e("ScanActivity", "Error updating user points: $e")
                    isScanning = true
                }
        }.addOnFailureListener { e ->
            Log.e("ScanActivity", "Error retrieving user document: $e")
            isScanning = true
        }
    }

    private fun isValidUrl(url: String): Boolean {
        return android.util.Patterns.WEB_URL.matcher(url).matches()
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
        webView.destroy()
    }
}
