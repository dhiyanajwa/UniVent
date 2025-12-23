package com.example.univent

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
        // LEVEL 5 UX: Session Persistence with Role Check
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null) {
            checkUserRoleAndNavigate(currentUser.uid)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // UI Element References
        val emailEditText = findViewById<TextInputEditText>(R.id.email_edit_text)
        val passwordEditText = findViewById<TextInputEditText>(R.id.password_edit_text)
        val loginButton = findViewById<Button>(R.id.login_button)
        val signUpRedirectTextView = findViewById<TextView>(R.id.sign_up_text_view)
        val forgotPasswordTextView = findViewById<TextView>(R.id.forgot_password_text_view)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                Toast.makeText(this, "Logging in...", Toast.LENGTH_SHORT).show()

                firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val user = firebaseAuth.currentUser
                        if (user != null) {
                            // Check if verified (optional check, but we still proceed to role check)
                            if (user.isEmailVerified) {
                                Log.d("LoginDebug", "Login Success & Verified")
                            } else {
                                Log.d("LoginDebug", "Login Success (Unverified)")
                            }

                            // Essential: Check Firestore role before navigating
                            checkUserRoleAndNavigate(user.uid)
                        }
                    } else {
                        val errorMessage = when (task.exception) {
                            is FirebaseAuthInvalidUserException -> "No account found with this email."
                            is FirebaseAuthInvalidCredentialsException -> "Incorrect password or email format."
                            else -> task.exception?.message ?: "Login failed."
                        }
                        Log.e("LoginDebug", "Login Failed: $errorMessage")
                        Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                }
            } else {
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
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
                        Toast.makeText(this, "Password reset email sent to $email", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Enter your email first to reset password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    /**
     * Rubric: Full sync with Firebase.
     * Fetches the user role from Firestore and navigates to the appropriate dashboard.
     */
    private fun checkUserRoleAndNavigate(uid: String) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val role = document.getString("role") ?: "student"

                    val intent = if (role.lowercase() == "admin") {
                        Log.d("LoginDebug", "Admin detected, heading to Dashboard")
                        Intent(this, AdminDashboardActivity::class.java)
                    } else {
                        Log.d("LoginDebug", "Student detected, heading to Catalog")
                        Intent(this, CatalogActivity::class.java)
                    }

                    startActivity(intent)
                    finish()
                } else {
                    // Fallback if user doc doesn't exist yet (Safety default)
                    startActivity(Intent(this, CatalogActivity::class.java))
                    finish()
                }
            }
            .addOnFailureListener { e ->
                Log.e("LoginDebug", "Error fetching role: ${e.message}")
                Toast.makeText(this, "Sync Error: Loading Catalog as guest.", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, CatalogActivity::class.java))
                finish()
            }
    }
}