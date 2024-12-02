package com.woodys.woodysburger

import android.os.Handler
import android.os.Looper
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import com.google.firebase.auth.FirebaseAuth
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException

class MainMenuActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var welcomeTextView: TextView
    private lateinit var pointsTextView: TextView
    private val handler = Handler(Looper.getMainLooper()) // Handler az ismétlődő feladatokhoz
    private val client = OkHttpClient() // HTTP kliens

    companion object {
        private const val PROFILE_ACTIVITY_REQUEST_CODE = 1
        private const val SCAN_ACTIVITY_REQUEST_CODE = 2
        private const val BASE_URL = "https://api.woodysburger.hu/api/users/"
        private const val TAG = "MainMenuActivity" // Log tag
        private const val TEST_UID = "GJb1SRENB4bCTb9fcU7LHBbJVAP2" // Teszt UID
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        auth = FirebaseAuth.getInstance()
        welcomeTextView = findViewById(R.id.welcome)
        pointsTextView = findViewById(R.id.points)

        // Start periodic point updates
        startPointUpdates()

        findViewById<AppCompatImageButton>(R.id.button_profile).setOnClickListener {
            navigateToProfileActivity()
        }

        findViewById<AppCompatImageButton>(R.id.button_settings).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        findViewById<AppCompatImageButton>(R.id.button_scan).setOnClickListener {
            val intent = Intent(this, ScanActivity::class.java)
            startActivityForResult(intent, SCAN_ACTIVITY_REQUEST_CODE)
        }
    }

    private fun startPointUpdates() {
        val updateTask = object : Runnable {
            override fun run() {
                updatePoints()
                handler.postDelayed(this, 5000) // 5 másodpercenként újrahívja magát
            }
        }
        handler.post(updateTask)
    }

    private fun updatePoints() {
        val firebaseUserId = auth.currentUser?.uid ?: TEST_UID // Használja a teszt UID-t, ha nincs bejelentkezett felhasználó
        Log.d(TAG, "Firebase User ID: $firebaseUserId") // Naplózza az aktuális UID-t

        if (firebaseUserId.isNotBlank()) {
            val url = "$BASE_URL$firebaseUserId"
            Log.d(TAG, "API URL: $url") // Naplózza az API URL-t

            val request = Request.Builder()
                .url(url)
                .build()

            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    Log.e(TAG, "API hívás sikertelen", e)
                    runOnUiThread {
                        pointsTextView.text = "ERROR IN POINTS!"
                    }
                }

                override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                    val responseBody = response.body?.string() ?: ""
                    Log.d(TAG, "API válasz: $responseBody")

                    if (response.isSuccessful) {
                        try {
                            val jsonObject = JSONObject(responseBody)
                            val userName = jsonObject.getString("name")
                            val points = jsonObject.getInt("points")

                            Log.d(TAG, "Felhasználó: $userName, Pontok: $points")

                            runOnUiThread {
                                welcomeTextView.text = "Üdv, $userName!"
                                pointsTextView.text = "Pontjaid: $points"
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Hiba az API válasz feldolgozásakor", e)
                            runOnUiThread {
                                pointsTextView.text = "ERROR IN PARSING!"
                            }
                        }
                    } else {
                        Log.e(TAG, "API hiba: ${response.code}")
                        runOnUiThread {
                            pointsTextView.text = "API ERROR: ${response.code}"
                        }
                    }
                }
            })
        } else {
            Log.e(TAG, "Nincs Firebase UID!")
            runOnUiThread {
                pointsTextView.text = "User not authenticated!"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null) // Megállítja az ismétlődő feladatokat az Activity lezárásakor
    }

    private fun navigateToProfileActivity() {
        val intent = Intent(this, ProfileActivity::class.java)
        startActivityForResult(intent, PROFILE_ACTIVITY_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PROFILE_ACTIVITY_REQUEST_CODE) {
            // Handle any actions when returning from ProfileActivity if needed
        } else if (requestCode == SCAN_ACTIVITY_REQUEST_CODE) {
            // Handle any actions when returning from ScanActivity if needed
        }
    }
}
