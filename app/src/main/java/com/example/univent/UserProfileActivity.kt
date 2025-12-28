package com.example.univent

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Base64
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.google.android.material.materialswitch.MaterialSwitch
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging

class UserProfileActivity : AppCompatActivity() {

    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private val CHANNEL_ID = "univent_notifications"
    private var userListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_user_profile)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupWindowInsets()
        checkNotificationPermission()

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnEditProfile = findViewById<ImageButton>(R.id.btnEditProfile)
        val switchPush = findViewById<MaterialSwitch>(R.id.switchPush)
        val btnSignOut = findViewById<Button>(R.id.btnSignOut)

        startUserListener()

        btnBack.setOnClickListener { finish() }

        btnEditProfile.setOnClickListener {
            val intent = Intent(this, EditProfileActivity::class.java)
            startActivity(intent)
        }

        switchPush.setOnCheckedChangeListener { buttonView, isChecked ->
            updatePreference("pushEnabled", isChecked)
            if (isChecked) {
                if (buttonView.isPressed) {
                    showTopPanelNotification("Notifications Active", "You will now receive alerts!")
                }
            } else {
                androidx.work.WorkManager.getInstance(this).cancelAllWork()
            }
        }

        btnSignOut.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun startUserListener() {
        val userId = auth.currentUser?.uid ?: return
        val imgProfile = findViewById<ImageView>(R.id.ivProfilePic)

        userListener = db.collection("users").document(userId)
            .addSnapshotListener { document, error ->
                if (error != null) return@addSnapshotListener

                if (document != null && document.exists()) {
                    findViewById<TextView>(R.id.tvName).text = document.getString("name") ?: ""
                    findViewById<TextView>(R.id.tvEmail).text = auth.currentUser?.email ?: ""
                    findViewById<TextView>(R.id.tvStudentId).text = document.getString("studentId") ?: ""
                    findViewById<TextView>(R.id.tvProgram).text = document.getString("program") ?: ""

                    findViewById<MaterialSwitch>(R.id.switchPush).isChecked = document.getBoolean("pushEnabled") ?: false

                    val base64String = document.getString("profileImageUrl")
                    if (!base64String.isNullOrEmpty()) {
                        try {
                            val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                            Glide.with(this)
                                .asBitmap()
                                .load(imageBytes)
                                .diskCacheStrategy(DiskCacheStrategy.NONE)
                                .skipMemoryCache(true)
                                .placeholder(R.drawable.ic_user_placeholder)
                                .circleCrop()
                                .into(imgProfile)
                        } catch (e: Exception) {
                            imgProfile.setImageResource(R.drawable.ic_user_placeholder)
                        }
                    } else {
                        imgProfile.setImageResource(R.drawable.ic_user_placeholder)
                    }
                }
            }

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result
                db.collection("users").document(userId).set(mapOf("fcmToken" to token), SetOptions.merge())
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        userListener?.remove()
    }

    private fun updatePreference(key: String, value: Boolean) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).set(mapOf(key to value), SetOptions.merge())
    }

    private fun showTopPanelNotification(title: String, message: String) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Event Reminders", NotificationManager.IMPORTANCE_HIGH)
            notificationManager.createNotificationChannel(channel)
        }
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
            }
        }
    }

    private fun setupWindowInsets() {
        val mainView = findViewById<android.view.View>(R.id.main)
        mainView?.let {
            ViewCompat.setOnApplyWindowInsetsListener(it) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
                insets
            }
        }
    }
}