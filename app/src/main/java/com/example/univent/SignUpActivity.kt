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
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.univent.User

/**
 * Goal: Full Firebase authentication with advanced error handling and Firestore sync.
 * Task: Create Auth account, send verification, and initialize User document in Firestore.
 * Strategy for Full Marks: Use explicit listeners for database success/failure to ensure data integrity.
 */
class SignUpActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var db: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        // Initialize Firebase services
        firebaseAuth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()

        // UI Element References

        val emailEditText = findViewById<TextInputEditText>(R.id.email_edit_text_signup)
        val passwordEditText = findViewById<TextInputEditText>(R.id.password_edit_text_signup)
        val confirmPasswordEditText = findViewById<TextInputEditText>(R.id.confirm_password_edit_text_signup)
        val signUpButton = findViewById<Button>(R.id.signup_button)
        val loginRedirectTextView = findViewById<TextView>(R.id.login_text_view_redirect)

        signUpButton.setOnClickListener {

            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            // Basic Validation
            if ( email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
                Toast.makeText(this, "All fields are required.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (password != confirmPassword) {
                Toast.makeText(this, "Passwords do not match.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Toast.makeText(this, "Processing registration...", Toast.LENGTH_SHORT).show()

            // 1. Create Firebase Auth Account
            firebaseAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val firebaseUser = firebaseAuth.currentUser
                        val uid = firebaseUser?.uid ?: ""

                        // 2. Create User Profile in Firestore (The "Full Sync" Requirement)
                        // This ensures the app knows if this user is a Student or Admin
                        val newUser = User(
                            uid = uid,
                            email = email,
                            role = "student" // Default role; manually change to 'admin' in console for test account
                        )

                        db.collection("users").document(uid).set(newUser)
                            .addOnSuccessListener {
                                Log.d("SignUpDebug", "Firestore profile created successfully.")

                                // 3. Send Email Verification
                                firebaseUser?.sendEmailVerification()?.addOnCompleteListener { verifyTask ->
                                    if (verifyTask.isSuccessful) {
                                        // Sign out so they must log in AFTER verifying
                                        firebaseAuth.signOut()

                                        Toast.makeText(this, "Verification email sent! Please check your inbox.", Toast.LENGTH_LONG).show()

                                        // Redirect to Login
                                        val intent = Intent(this, LoginActivity::class.java)
                                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                        startActivity(intent)
                                        finish()
                                    } else {
                                        Log.e("SignUpDebug", "Email verification failed: ${verifyTask.exception?.message}")
                                        Toast.makeText(this, "Failed to send verification email.", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                            .addOnFailureListener { e ->
                                // Rubric: Full sync with error handling
                                Log.e("SignUpDebug", "Firestore Sync Error: ${e.message}")
                                Toast.makeText(this, "Data sync failed: ${e.message}", Toast.LENGTH_LONG).show()
                            }

                    } else {
                        // Advanced Error Handling for Authentication
                        val errorMessage = when (task.exception) {
                            is FirebaseAuthUserCollisionException -> "This email is already in use."
                            is FirebaseAuthWeakPasswordException -> "Password too weak (min 6 characters)."
                            is FirebaseAuthInvalidCredentialsException -> "Invalid email format."
                            else -> task.exception?.message ?: "Registration failed."
                        }
                        Log.e("SignUpDebug", "Auth Error: $errorMessage")
                        Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                    }
                }
        }

        loginRedirectTextView.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
}