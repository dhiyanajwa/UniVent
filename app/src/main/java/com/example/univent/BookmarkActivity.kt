package com.example.univent

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.univent.databinding.ActivityBookmarkBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.messaging.FirebaseMessaging

class BookmarkActivity : AppCompatActivity() {

    private lateinit var binding: ActivityBookmarkBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: StudentEventAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBookmarkBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupRecyclerView()
        binding.btnBack.setOnClickListener { finish() }
        listenToBookmarks()
    }

    private fun setupRecyclerView() {
        adapter = StudentEventAdapter(
            onBookmarkClick = { event -> removeBookmark(event.id) },
            onItemClick = { event ->
                val intent = Intent(this, EventDetailActivity::class.java)
                intent.putExtra("EVENT_ID", event.id)
                startActivity(intent)
            }
        )
        binding.rvBookmarkedEvents.layoutManager = LinearLayoutManager(this)
        binding.rvBookmarkedEvents.adapter = adapter
    }

    /**
     * Listen for real-time changes in the user's bookmarkedEvents list
     */
    private fun listenToBookmarks() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .addSnapshotListener { document, error ->
                if (error != null) {
                    Log.e("BOOKMARK_ERROR", "Error listening to bookmarks", error)
                    return@addSnapshotListener
                }

                val bookmarkIds = document?.get("bookmarkedEvents") as? List<String> ?: emptyList()

                if (bookmarkIds.isNotEmpty()) {
                    fetchEventDetails(bookmarkIds)
                    binding.layoutEmptyBookmark.visibility = View.GONE
                    binding.rvBookmarkedEvents.visibility = View.VISIBLE
                } else {
                    adapter.submitList(emptyList())
                    binding.layoutEmptyBookmark.visibility = View.VISIBLE
                    binding.rvBookmarkedEvents.visibility = View.GONE
                }
            }
    }

    /**
     * Fetch the full details for each bookmarked event ID
     */
    private fun fetchEventDetails(ids: List<String>) {
        db.collection("events").whereIn("__name__", ids).get()
            .addOnSuccessListener { snapshots ->
                val events = snapshots.toObjects(Event::class.java).mapIndexed { index, event ->
                    event.apply { id = snapshots.documents[index].id }
                }
                adapter.submitList(events)
            }
            .addOnFailureListener { e ->
                Log.e("FETCH_ERROR", "Error fetching event details", e)
            }
    }

    /**
     * UPDATED: Removes bookmark from Firestore AND unsubscribes from FCM Topic
     */
    private fun removeBookmark(eventId: String) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId)
            .update("bookmarkedEvents", FieldValue.arrayRemove(eventId))
            .addOnSuccessListener {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("bookmark_$eventId")
                    .addOnSuccessListener {
                        Log.d("FCM_TOPIC", "Successfully unsubscribed from bookmark_$eventId")
                    }

                Toast.makeText(this, "Removed from Bookmarks", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to remove bookmark: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}