package com.woodys.woodysburger

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import org.json.JSONObject
import java.io.IOException

class LoginActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    // Register the activity result launcher for Google Sign-In
    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val signInIntent = result.data
        val task = GoogleSignIn.getSignedInAccountFromIntent(signInIntent)
        handleSignInResult(task)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        auth = FirebaseAuth.getInstance()
        configureGoogleSignIn()

        findViewById<Button>(R.id.login_button).setOnClickListener { login() }
        findViewById<Button>(R.id.register_button).setOnClickListener { navigateToRegistration() }
        findViewById<Button>(R.id.google_login_button).setOnClickListener { googleSignIn() }

        checkUserLoggedIn()
    }

    private fun configureGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)) // Ensure this is set up in your strings.xml
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun login() {
        val email = findViewById<EditText>(R.id.email).text.toString()
        val password = findViewById<EditText>(R.id.password).text.toString()

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                startMainMenu()
            } else {
                Toast.makeText(this, "Login failed: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun navigateToRegistration() {
        val intent = Intent(this, RegistrationActivity::class.java)
        startActivity(intent)
    }

    private fun googleSignIn() {
        val signInIntent = googleSignInClient.signInIntent
        googleSignInLauncher.launch(signInIntent)
    }

    private fun handleSignInResult(completedTask: Task<GoogleSignInAccount>) {
        try {
            val account = completedTask.getResult(ApiException::class.java)!!
            firebaseAuthWithGoogle(account.idToken!!)
        } catch (e: ApiException) {
            Toast.makeText(this, "Google sign-in failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            Log.e("LoginActivity", "Google sign-in failed: ${e.localizedMessage}")
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                user?.let {
                    Log.d("LoginActivity", "User authenticated: ${it.uid}, ${it.email}")
                    sendUserDataToApi(it.uid, it.email)
                }
                startMainMenu()
            } else {
                Toast.makeText(this, "Authentication Failed: ${task.exception?.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun sendUserDataToApi(uid: String, email: String?) {
        val client = OkHttpClient()
        val json = JSONObject()

        // Setting both 'name' and 'userId' to the uid
        json.put("name", uid)   // Set the user's UID as the name
        json.put("userId", uid) // Populate the userId field with the uid
        json.put("email", email)
        json.put("points", 0)    // Default points, can be adjusted as needed

        val body = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            json.toString()
        )

        val request = Request.Builder()
            .url("https://api.woodysburger.hu/api/users")
            .post(body)
            .addHeader("Content-Type", "application/json")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@LoginActivity, "Failed to send user data", Toast.LENGTH_LONG).show()
                }
                Log.e("LoginActivity", "Error sending user data: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Log.d("LoginActivity", "User data sent successfully: ${response.body?.string()}")
                    } else {
                        Toast.makeText(this@LoginActivity, "Error sending user data: ${response.message}", Toast.LENGTH_LONG).show()
                        Log.e("LoginActivity", "Error sending user data: ${response.message}")
                    }
                }
            }
        })
    }


    private fun startMainMenu() {
        val intent = Intent(this, MainMenuActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun checkUserLoggedIn() {
        if (auth.currentUser != null) {
            startMainMenu()
        }
    }
}
