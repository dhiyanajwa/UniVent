package com.example.univent

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.univent.databinding.ActivityBookmarkBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue

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

    private fun listenToBookmarks() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .addSnapshotListener { document, _ ->
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

    private fun fetchEventDetails(ids: List<String>) {
        db.collection("events").whereIn("__name__", ids).get()
            .addOnSuccessListener { snapshots ->
                val events = snapshots.toObjects(Event::class.java).mapIndexed { index, event ->
                    event.apply { id = snapshots.documents[index].id }
                }
                adapter.submitList(events)
            }
    }

    private fun removeBookmark(eventId: String) {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId)
            .update("bookmarkedEvents", FieldValue.arrayRemove(eventId))
            .addOnSuccessListener { Toast.makeText(this, "Removed", Toast.LENGTH_SHORT).show() }
    }
}