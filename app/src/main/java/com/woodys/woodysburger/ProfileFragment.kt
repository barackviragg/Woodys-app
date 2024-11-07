package com.woodys.woodysburger

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth

class ProfileFragment : Fragment() {

    private lateinit var auth: FirebaseAuth
    private lateinit var userEmailTextView: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        auth = FirebaseAuth.getInstance()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        userEmailTextView = view.findViewById(R.id.user_email) // Replace with your actual ID
        displayUserEmail()

        view.findViewById<Button>(R.id.change_password_button).setOnClickListener {
            sendPasswordResetEmail()
        }

        return view
    }

    private fun displayUserEmail() {
        val user = auth.currentUser
        userEmailTextView.text = user?.email ?: "No email found" // Set user email or default message
    }

    private fun sendPasswordResetEmail() {
        val user = auth.currentUser
        user?.email?.let { email ->
            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(requireContext(), "Password reset email sent to $email", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Failed to send password reset email.", Toast.LENGTH_SHORT).show()
                    }
                }
        } ?: run {
            Toast.makeText(requireContext(), "No user logged in.", Toast.LENGTH_SHORT).show()
        }
    }
}
