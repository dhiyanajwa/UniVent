package com.example.univent

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.auth.FirebaseAuth

class CatalogActivity : AppCompatActivity() {

    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_catalog)

        firebaseAuth = FirebaseAuth.getInstance()

        // 1. Set up the Header (Toolbar)
        val toolbar = findViewById<MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        
        // 2. Add a simple click listener to the tiles (placeholders)
        // This is where you would eventually add logic to open event details
        Toast.makeText(this, "Welcome to UniVent!", Toast.LENGTH_SHORT).show()
    }

    override fun onStart() {
        super.onStart()
        // 3. Security Check: If user is not logged in, send them back to Login
        if (firebaseAuth.currentUser == null) {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}