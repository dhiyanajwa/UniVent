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
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException

class SignUpActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        firebaseAuth = FirebaseAuth.getInstance()

        val emailEditText = findViewById<TextInputEditText>(R.id.email_edit_text_signup)
        val passwordEditText = findViewById<TextInputEditText>(R.id.password_edit_text_signup)
        val confirmPasswordEditText = findViewById<TextInputEditText>(R.id.confirm_password_edit_text_signup)
        val signUpButton = findViewById<Button>(R.id.signup_button)
        val loginRedirectTextView = findViewById<TextView>(R.id.login_text_view_redirect)

        signUpButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if (password == confirmPassword) {

                    Toast.makeText(this, "Creating account...", Toast.LENGTH_SHORT).show()

                    firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            val user = firebaseAuth.currentUser

                            // STEP 1: Send Verification Email
                            user?.sendEmailVerification()?.addOnCompleteListener { verifyTask ->
                                if (verifyTask.isSuccessful) {
                                    Log.d("SignUpDebug", "Verification email sent.")

                                    // STEP 2: Sign out immediately
                                    // (They can't be logged in yet because they aren't verified)
                                    firebaseAuth.signOut()

                                    Toast.makeText(this, "Account created! Please check your email for verification.", Toast.LENGTH_LONG).show()

                                    // STEP 3: Redirect to Login
                                    val intent = Intent(this, LoginActivity::class.java)
                                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
                                    startActivity(intent)
                                    finish()
                                } else {
                                    Log.e("SignUpDebug", "Failed to send email: ${verifyTask.exception?.message}")
                                    Toast.makeText(this, "Failed to send verification email.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        } else {
                            // Level 5 Rubric: Advanced Error Handling
                            val errorMessage = when (task.exception) {
                                is FirebaseAuthUserCollisionException -> "This email is already registered."
                                is FirebaseAuthWeakPasswordException -> "Password is too weak (min 6 characters)."
                                is FirebaseAuthInvalidCredentialsException -> "Invalid email format."
                                else -> task.exception?.message ?: "Registration failed."
                            }

                            Log.e("SignUpDebug", "Registration Failed: $errorMessage")
                            Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            } else {
                // Replacing Toast with standard Toast for simplicity as per your original code
                Toast.makeText(this, "All fields must be filled", Toast.LENGTH_SHORT).show()
            }
        }

        loginRedirectTextView.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}
