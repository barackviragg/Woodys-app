package com.woodys.woodysburger

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        // Back button
        findViewById<ImageButton>(R.id.back).setOnClickListener {
            finish() // Close the current activity
        }

        // Setting button listeners
        findViewById<Button>(R.id.button_notification_preferences).setOnClickListener {
            // Handle Notification Preferences
        }

        val accountManagementButton: Button = findViewById(R.id.button_account_management)
        accountManagementButton.setOnClickListener {
            val intent = Intent(this, AccountManagementActivity::class.java)
            startActivity(intent)
        }


        findViewById<Button>(R.id.button_help_support).setOnClickListener {
            // Handle Help & Support
        }

        findViewById<Button>(R.id.button_about).setOnClickListener {
            val facebookPageUrl = "https://www.facebook.com/woodysburgerhu" // Replace with your Facebook page URL
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(facebookPageUrl))
            startActivity(intent)
        }
    }
}
