package com.woodys.woodysburger

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class MainMenuActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var welcomeTextView: TextView
    private lateinit var pointsTextView: TextView
    private val firestore = FirebaseFirestore.getInstance()
    private var isNavBarOpen = false

    // Constant for the request code
    companion object {
        private const val PROFILE_ACTIVITY_REQUEST_CODE = 1
        private const val SCAN_ACTIVITY_REQUEST_CODE = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main_menu)

        auth = FirebaseAuth.getInstance()
        welcomeTextView = findViewById(R.id.welcome)
        pointsTextView = findViewById(R.id.points)

        // Set welcome message (example: "Üdv, User!")
        val userName = auth.currentUser?.displayName ?: "User"
        welcomeTextView.text = "Üdv, $userName!"

        // Fetch user's points from Firestore and set it in the pointsTextView
        auth.currentUser?.let { user ->
            firestore.collection("users").document(user.uid).get()
                .addOnSuccessListener { document ->
                    val points = document.getLong("points") ?: 0
                    pointsTextView.text = "Pontjaid: $points"
                }
                .addOnFailureListener {
                    pointsTextView.text = "Pontjaid: 0"
                }
        }

        findViewById<AppCompatImageButton>(R.id.button_profile).setOnClickListener {
            navigateToProfileActivity()
        }

        findViewById<AppCompatImageButton>(R.id.button_settings).setOnClickListener {
            val intent = Intent(this, SettingsActivity::class.java)
            startActivity(intent)
        }

        // Set up the scan button
        findViewById<AppCompatImageButton>(R.id.button_scan).setOnClickListener {
            val intent = Intent(this, ScanActivity::class.java)
            startActivityForResult(intent, SCAN_ACTIVITY_REQUEST_CODE)
        }
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
