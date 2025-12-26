package com.example.univent

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.univent.databinding.ActivityCatalogBinding
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

/**
 * Goal: Manage event catalog with persistent login session.
 * Strategy: Check Auth state immediately, use Firestore snapshots for real-time updates.
 */
class CatalogActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCatalogBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var studentAdapter: StudentEventAdapter

    private var allEvents = listOf<Event>()
    private var currentCategory = "All"
    private var currentSearchQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. Session & Persistence Check
        auth = FirebaseAuth.getInstance()

        // If user is null, they aren't logged in. Redirect to Login.
        if (auth.currentUser == null) {
            redirectToLogin()
            return
        }
        // Initialize view binding only after verifying session
        binding = ActivityCatalogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()

        setupRecyclerView()
        setupFilters()
        setupNavigation()
        listenToEvents()
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        // Clear activity stack so user cannot "back" into Catalog without login
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupRecyclerView() {
        studentAdapter = StudentEventAdapter(
            onBookmarkClick = { event ->
                toggleEventBookmark(event.id)
            },
            onItemClick = { event ->
                val intent = Intent(this, EventDetailActivity::class.java)
                intent.putExtra("EVENT_ID", event.id)
                startActivity(intent)
            }
        )
        binding.rvStudentEvents.apply {
            layoutManager = LinearLayoutManager(this@CatalogActivity)
            adapter = studentAdapter
        }
    }

    private fun toggleEventBookmark(eventId: String) {
        val userId = auth.currentUser?.uid ?: return
        val userRef = db.collection("users").document(userId)

        // First, check current state to decide whether to add or remove
        userRef.get().addOnSuccessListener { doc ->
            val bookmarks = doc.get("bookmarks") as? List<String> ?: emptyList()
            if (bookmarks.contains(eventId)) {
                userRef.update("bookmarks", FieldValue.arrayRemove(eventId))
                Toast.makeText(this, "Removed from bookmarks", Toast.LENGTH_SHORT).show()
            } else {
                userRef.update("bookmarks", FieldValue.arrayUnion(eventId))
                Toast.makeText(this, "Added to bookmarks", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun listenToEvents() {
        // Real-time listener: data updates instantly when changed in Firestore
        db.collection("events")
            .orderBy("date", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Toast.makeText(this, "Error loading events", Toast.LENGTH_SHORT).show()
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val events = mutableListOf<Event>()
                    for (doc in snapshots) {
                        val event = doc.toObject(Event::class.java).apply { id = doc.id }
                        events.add(event)
                    }
                    allEvents = events
                    applyFilters()
                }
            }
    }

    private fun setupFilters() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false
            override fun onQueryTextChange(newText: String?): Boolean {
                currentSearchQuery = newText ?: ""
                applyFilters()
                return true
            }
        })

        binding.chipGroupCategories.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                val chip = group.findViewById<Chip>(checkedIds[0])
                currentCategory = chip.text.toString()
                applyFilters()
            } else {
                currentCategory = "All"
                applyFilters()
            }
        }
    }

    private fun applyFilters() {
        val filteredList = allEvents.filter { event ->
            val matchesCategory = (currentCategory == "All") || (event.category.equals(currentCategory, ignoreCase = true))
            val matchesSearch = event.title.contains(currentSearchQuery, ignoreCase = true) ||
                    event.description.contains(currentSearchQuery, ignoreCase = true)

            matchesCategory && matchesSearch
        }
        studentAdapter.submitList(filteredList)
    }

    private fun setupNavigation() {
        // Find the profile layout from the Bottom Navigation Bar
        binding.navProfile.setOnClickListener {
            val intent = Intent(this, UserProfileActivity::class.java)
            startActivity(intent)
        }

        binding.navCalendar.setOnClickListener {
            val intent = Intent(this, CalendarActivity::class.java)
            startActivity(intent)
        }

        // Example: If you have a logout button in Profile, ensure it calls auth.signOut()
    }
}