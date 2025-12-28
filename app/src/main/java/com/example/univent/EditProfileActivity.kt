package com.example.univent

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import java.io.ByteArrayOutputStream

class EditProfileActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var selectedImageUri: Uri? = null
    private var isPhotoRemoved = false

    private val pickImage = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            isPhotoRemoved = false
            findViewById<ImageView>(R.id.imgProfile).setImageURI(it)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        val imgProfile = findViewById<ImageView>(R.id.imgProfile)
        val btnEditPhotoOptions = findViewById<Button>(R.id.btnEditPhotoOptions)
        val btnSave = findViewById<MaterialButton>(R.id.btnSaveChanges)
        val btnBack = findViewById<ImageButton>(R.id.btnBack)

        val editName = findViewById<TextInputEditText>(R.id.EditName)
        val editEmail = findViewById<TextInputEditText>(R.id.EditEmail)
        val editStudentId = findViewById<TextInputEditText>(R.id.EditStudentId)
        val editProgram = findViewById<TextInputEditText>(R.id.EditProgram)

        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                editName.setText(doc.getString("name"))
                editEmail.setText(doc.getString("email"))
                editStudentId.setText(doc.getString("studentId"))
                editProgram.setText(doc.getString("program"))

                val base64String = doc.getString("profileImageUrl")
                if (!base64String.isNullOrEmpty()) {
                    try {
                        val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                        Glide.with(this).load(imageBytes).circleCrop().into(imgProfile)
                    } catch (e: Exception) {
                        Log.e("EditProfile", "Error decoding image", e)
                    }
                }
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Failed to load data: ${e.message}", Toast.LENGTH_SHORT).show()
        }

        btnBack.setOnClickListener {
            handleBackNavigation()
        }

        btnEditPhotoOptions.setOnClickListener {
            val options = arrayOf("Add/Change Photo", "Delete Photo")
            val builder = AlertDialog.Builder(this)
            builder.setTitle("Profile Photo")
            builder.setItems(options) { _, which ->
                when (which) {
                    0 -> pickImage.launch("image/*")
                    1 -> {
                        selectedImageUri = null
                        isPhotoRemoved = true
                        imgProfile.setImageResource(R.drawable.ic_user_placeholder)
                        Toast.makeText(this, "Photo removed. Save to confirm.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            builder.show()
        }

        btnSave.setOnClickListener {
            btnSave.isEnabled = false

            val updatedData = mutableMapOf<String, Any?>(
                "name" to editName.text.toString().trim(),
                "email" to editEmail.text.toString().trim(),
                "studentId" to editStudentId.text.toString().trim(),
                "program" to editProgram.text.toString().trim()
            )

            if (selectedImageUri != null) {
                updatedData["profileImageUrl"] = uriToBase64(selectedImageUri!!)
            } else if (isPhotoRemoved) {
                updatedData["profileImageUrl"] = null
            }

            db.collection("users").document(userId)
                .set(updatedData, SetOptions.merge())
                .addOnSuccessListener {
                    Toast.makeText(this, "Profile Updated!", Toast.LENGTH_SHORT).show()
                    handleBackNavigation()
                }
                .addOnFailureListener { e ->
                    btnSave.isEnabled = true
                    Toast.makeText(this, "Update failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun handleBackNavigation() {
        if (isTaskRoot) {
            val intent = Intent(this, CatalogActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }
        finish()
    }

    override fun onBackPressed() {
        handleBackNavigation()
        super.onBackPressed()
    }

    private fun uriToBase64(uri: Uri): String? {
        return try {
            val inputStream = contentResolver.openInputStream(uri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            val scaledBitmap = Bitmap.createScaledBitmap(bitmap, 300, 300, true)
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            Base64.encodeToString(outputStream.toByteArray(), Base64.DEFAULT)
        } catch (e: Exception) { null }
    }
}