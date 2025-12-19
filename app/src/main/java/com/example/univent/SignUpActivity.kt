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

            Log.d("SignUpDebug", "Button Clicked! Email: $email")

            if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty()) {
                if (password == confirmPassword) {
                    
                    Toast.makeText(this, "Connecting to Firebase...", Toast.LENGTH_SHORT).show()
                    
                    firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.d("SignUpDebug", "Registration Success!")
                            Toast.makeText(this, "Sign Up Successful!", Toast.LENGTH_SHORT).show()
                            val intent = Intent(this, CatalogActivity::class.java)
                            startActivity(intent)
                            finish()
                        } else {
                            Log.e("SignUpDebug", "Registration Failed: ${task.exception?.message}")
                            Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Passwords do not match", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "All fields must be filled", Toast.LENGTH_SHORT).show()
            }
        }

        loginRedirectTextView.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
        }
    }
}