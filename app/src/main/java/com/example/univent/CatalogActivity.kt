package com.example.univent

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.univent.databinding.ActivityCatalogBinding
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query


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

        auth = FirebaseAuth.getInstance()
        if (auth.currentUser == null) {
            redirectToLogin()
            return
        }

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
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupRecyclerView() {
        studentAdapter = StudentEventAdapter(
            onBookmarkClick = { event ->
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

    private fun listenToEvents() {
        db.collection("events")
            .orderBy("date", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) return@addSnapshotListener
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
            } else {
                currentCategory = "All"
            }
            applyFilters()
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
        binding.navProfile.setOnClickListener { startActivity(Intent(this, UserProfileActivity::class.java)) }
        binding.navCalendar.setOnClickListener { startActivity(Intent(this, CalendarActivity::class.java)) }
        binding.navBookmark.setOnClickListener { startActivity(Intent(this, BookmarkActivity::class.java)) }
    }
}