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
        eventId = intent.getStringExtra("EVENT_ID")

        if (eventId != null) {
            loadEventDetails()
            checkBookmarkStatus()

            // Matches XML ID: ivBookmark (Top-right bookmark icon)
            binding.ivBookmark.setOnClickListener { toggleBookmark() }

            // Matches XML ID: btnJoinEvent (The "JOIN THIS EVENT" button)
            binding.btnJoinEvent.setOnClickListener { joinEvent() }
        }

        binding.btnBack.setOnClickListener { finish() }
    }

    private fun loadEventDetails() {
        val currentEventId = eventId ?: return
        db.collection("events").document(currentEventId).get()
            .addOnSuccessListener { doc ->
                val event = doc.toObject(Event::class.java)
                event?.let {
                    // UPDATED: These match your new activity_event_detail.xml IDs
                    binding.tvEventTitle.text = it.title
                    binding.tvEventDate.text = "${it.date} • ${it.time}"
                    binding.tvEventLocation.text = it.location
                    binding.tvEventDescription.text = it.description
                    binding.chipCategory.text = it.category

                    Glide.with(this)
                        .load(it.imageUrl)
                        .placeholder(R.drawable.event_seminar)
                        .into(binding.ivEventImage)
                }
            }
    }

    /**
     * CALENDAR SYNC: Saves to "joinedEvents" field
     */
    private fun joinEvent() {
        val userId = auth.currentUser?.uid ?: return
        val currentEventId = eventId ?: return

        db.collection("users").document(userId)
            .update("joinedEvents", FieldValue.arrayUnion(currentEventId))
            .addOnSuccessListener {
                Toast.makeText(this, "Event added to your Schedule!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Failed to join event", Toast.LENGTH_SHORT).show()
            }
    }

    /**
     * BOOKMARK SYNC: Uses "bookmarkedEvents" field
     */
    private fun checkBookmarkStatus() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                // SAFE CAST FIX: Handles the "Unchecked cast" warning
                val bookmarks = doc.get("bookmarkedEvents") as? List<*>
                val bookmarkList = bookmarks?.filterIsInstance<String>() ?: emptyList()

                isBookmarked = bookmarkList.contains(eventId)
                updateBookmarkUI(false)
            }
    }

    private fun toggleBookmark() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(userId)
        val currentEventId = eventId ?: return

        isBookmarked = !isBookmarked
        updateBookmarkUI(true)

        if (isBookmarked) {
            userRef.update("bookmarkedEvents", FieldValue.arrayUnion(currentEventId))
        } else {
            userRef.update("bookmarkedEvents", FieldValue.arrayRemove(currentEventId))
        }
    }

    private fun updateBookmarkUI(shouldAnimate: Boolean) {
        val icon = if (isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_outline
        binding.ivBookmark.setImageResource(icon)

        if (shouldAnimate) {
            binding.ivBookmark.animate()
                .scaleX(1.4f).scaleY(1.4f)
                .setDuration(200)
                .setInterpolator(BounceInterpolator())
                .withEndAction {
                    binding.ivBookmark.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
                }.start()
        }
    }
}