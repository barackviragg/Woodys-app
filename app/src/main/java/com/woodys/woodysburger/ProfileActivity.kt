package com.woodys.woodysburger

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.woodys.woodysburger.databinding.ActivityProfileBinding

class ProfileActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var binding: ActivityProfileBinding
    private var isNavBarOpen = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        auth = FirebaseAuth.getInstance()
        displayUserEmail()

        binding.changePasswordButton.setOnClickListener {
            sendPasswordResetEmail()
        }

        binding.back.setOnClickListener {
            finish() // Close ProfileActivity and return to MainMenuActivity
        }
    }

    private fun displayUserEmail() {
        val user = auth.currentUser
        binding.userEmail.text = user?.email ?: "No email found"
    }

    private fun sendPasswordResetEmail() {
        val user = auth.currentUser
        user?.email?.let { email ->
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Password reset email sent to $email", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Failed to send password reset email.", Toast.LENGTH_SHORT).show()
                    }
                }
        } ?: run {
            Toast.makeText(this, "No user logged in.", Toast.LENGTH_SHORT).show()
        }
    }
}
