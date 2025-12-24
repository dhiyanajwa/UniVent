package com.example.univent

import android.os.Bundle
import android.view.animation.BounceInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.univent.databinding.ActivityEventDetailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore

class EventDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventDetailBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private var eventId: String? = null
    private var isBookmarked = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEventDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Retrieve the ID passed from CatalogActivity
        eventId = intent.getStringExtra("EVENT_ID")

        // 1. Back Button Navigation
        binding.btnBack.setOnClickListener {
            // Finishes current activity to return to the previous one (Catalog)
            finish()
        }

        // 2. Load Event Data
        if (eventId != null) {
            loadEventDetails()
            checkBookmarkStatus()
        } else {
            Toast.makeText(this, "Error: Event details not found.", Toast.LENGTH_SHORT).show()
            finish()
        }

        // 3. Animated Bookmark Toggle
        binding.ivBookmark.setOnClickListener {
            toggleBookmark()
        }

        binding.btnJoinEvent.setOnClickListener {
            Toast.makeText(this, "Successfully registered for this event!", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadEventDetails() {
        db.collection("events").document(eventId!!).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val event = document.toObject(Event::class.java)
                    event?.let {
                        binding.tvDetailTitle.text = it.title
                        binding.tvDetailDate.text = "${it.date}, ${it.time}"
                        binding.tvDetailLocation.text = it.location
                        binding.tvDetailDescription.text = it.description
                        binding.chipCategory.text = it.category

                        // Load image using Glide
                        if (it.imageUrl.isNotEmpty()) {
                            Glide.with(this)
                                .load(it.imageUrl)
                                .placeholder(R.drawable.event_seminar)
                                .into(binding.ivDetailImage)
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to load: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun checkBookmarkStatus() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val bookmarks = doc.get("bookmarks") as? List<String> ?: emptyList()
                isBookmarked = bookmarks.contains(eventId)
                updateBookmarkUI(false) // Update UI without animating on initial load
            }
    }

    private fun toggleBookmark() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(userId)

        isBookmarked = !isBookmarked
        updateBookmarkUI(true) // Animate on click

        if (isBookmarked) {
            userRef.update("bookmarks", FieldValue.arrayUnion(eventId))
        } else {
            userRef.update("bookmarks", FieldValue.arrayRemove(eventId))
        }
    }

    private fun updateBookmarkUI(shouldAnimate: Boolean) {
        val icon = if (isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_outline
        binding.ivBookmark.setImageResource(icon)

        if (shouldAnimate) {
            binding.ivBookmark.animate()
                .scaleX(1.4f)
                .scaleY(1.4f)
                .setDuration(200)
                .setInterpolator(BounceInterpolator())
                .withEndAction {
                    binding.ivBookmark.animate().scaleX(1.0f).scaleY(1.0f).setDuration(100).start()
                }.start()
        }
    }
}