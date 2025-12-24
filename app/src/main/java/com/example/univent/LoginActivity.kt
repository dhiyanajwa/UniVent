package com.example.univent

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import com.google.firebase.firestore.FirebaseFirestore

class LoginActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onStart() {
        super.onStart()
        // If user is already logged in, skip login screen
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            Log.d("LoginDebug", "Session found for: ${currentUser.email}")
            checkUserRoleAndNavigate(currentUser.uid)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        val emailEditText = findViewById<TextInputEditText>(R.id.email_edit_text)
        val passwordEditText = findViewById<TextInputEditText>(R.id.password_edit_text)
        val loginButton = findViewById<Button>(R.id.login_button)
        val signUpRedirectTextView = findViewById<TextView>(R.id.sign_up_text_view)
        val forgotPasswordTextView = findViewById<TextView>(R.id.forgot_password_text_view)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginButton.isEnabled = false // Prevent double clicks
                Toast.makeText(this, "Authenticating...", Toast.LENGTH_SHORT).show()

                firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = firebaseAuth.currentUser
                        if (user != null) {
                            Log.d("LoginDebug", "Auth Success: ${user.uid}")
                            checkUserRoleAndNavigate(user.uid)
                        }
                    } else {
                        loginButton.isEnabled = true
                        val errorMessage = when (task.exception) {
                            is FirebaseAuthInvalidUserException -> "No account found."
                            is FirebaseAuthInvalidCredentialsException -> "Wrong password."
                            else -> task.exception?.message ?: "Login failed."
                        }
                        Log.e("LoginDebug", "Auth Error: $errorMessage")
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "Please enter all fields", Toast.LENGTH_SHORT).show()
            }
        }

        signUpRedirectTextView.setOnClickListener {
            startActivity(Intent(this, SignUpActivity::class.java))
        }

        forgotPasswordTextView.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            if (email.isNotEmpty()) {
                firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Reset link sent!", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun checkUserRoleAndNavigate(uid: String) {
        Log.d("LoginDebug", "Fetching role for UID: $uid")

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                // Determine destination
                val intent = if (document != null && document.exists()) {
                    val role = document.getString("role") ?: "student"
                    Log.d("LoginDebug", "Role found: $role")

                    if (role.equals("admin", ignoreCase = true)) {
                        Intent(this, AdminDashboardActivity::class.java)
                    } else {
                        Intent(this, CatalogActivity::class.java)
                    }
                } else {
                    // Fallback to Catalog if user document is missing
                    Log.e("LoginDebug", "Document does not exist for UID: $uid. Defaulting to Catalog.")
                    Intent(this, CatalogActivity::class.java)
                }

                startIntent(intent)
            }
            .addOnFailureListener { e ->
                Log.e("LoginDebug", "Firestore Error: ${e.message}")
                Toast.makeText(this, "Login Sync Error. Opening Catalog...", Toast.LENGTH_SHORT).show()
                // Fallback to Catalog on network error
                startIntent(Intent(this, CatalogActivity::class.java))
            }
    }

    private fun startIntent(intent: Intent) {
        try {
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        } catch (e: ActivityNotFoundException) {
            // CRITICAL: This catches missing Manifest entries
            Log.e("LoginDebug", "CRITICAL ERROR: Activity not found in Manifest!", e)
            Toast.makeText(this, "Error: Screen not registered in AndroidManifest.xml", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            // Catches crashes inside the next Activity's onCreate
            Log.e("LoginDebug", "Navigation Crashed: ${e.message}", e)
            Toast.makeText(this, "Error opening screen: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}