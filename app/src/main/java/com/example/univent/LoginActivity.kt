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

class LoginActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onStart() {
        super.onStart()
        // LEVEL 5 UX: Session Persistence
        // If user is already logged in AND verified, go straight to Catalog
        val currentUser = firebaseAuth.currentUser
        if (currentUser != null && currentUser.isEmailVerified) {
            startActivity(Intent(this, CatalogActivity::class.java))
            finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        firebaseAuth = FirebaseAuth.getInstance()

        // Updated IDs to match your activity_login.xml
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

                        // STEP 1: Check if Email is Verified
                        if (user != null && user.isEmailVerified) {
                            Log.d("LoginDebug", "Login Success & Verified")
                            Toast.makeText(this, "Welcome back!", Toast.LENGTH_SHORT).show()

                            val intent = Intent(this, CatalogActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            // STEP 2: Handle Unverified Email
                            firebaseAuth.signOut() // Log them out immediately
                            Log.w("LoginDebug", "Login Attempt: Email not verified")
                            Toast.makeText(this, "Please verify your email before logging in. Check your inbox.", Toast.LENGTH_LONG).show()
                        }
                    } else {
                        // STEP 3: Level 5 Advanced Error Handling
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
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }

        // Added Forgot Password logic to match your XML layout
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
}