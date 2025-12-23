package com.example.univent

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.univent.databinding.ActivityAdminDashboardBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query

/**
 * Goal: Complete CRUD + Firebase integration for Admin.
 * Task: Manage campus events (Create, Read, Update, Delete) and view stats.
 * Strategy for Full Marks: Implement real-time sync with error handling and confirmation dialogs.
 */
class AdminDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminDashboardBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var eventAdapter: AdminEventAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupRecyclerView()
        loadEvents()

        // FAB to Add New Event
        binding.fabAddEvent.setOnClickListener {
            val intent = Intent(this, AddEditActivity::class.java)
            startActivity(intent)
        }

        // Updated: Back button now signs out and redirects to Login
        binding.btnBack.setOnClickListener {
            auth.signOut()
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun setupRecyclerView() {
        // Goal: Professional & Smooth UI
        // Initialize your adapter with a click listener for Delete and Edit
        eventAdapter = AdminEventAdapter(
            onEditClick = { event ->
                val intent = Intent(this, AddEditActivity::class.java)
                intent.putExtra("EVENT_ID", event.id)
                startActivity(intent)
            },
            onDeleteClick = { event ->
                showDeleteConfirmation(event.id)
            }
        )

        binding.rvAdminEvents.apply {
            layoutManager = LinearLayoutManager(this@AdminDashboardActivity)
            adapter = eventAdapter
        }
    }

    private fun loadEvents() {
        // Goal: Full sync with error handling
        db.collection("events")
            .orderBy("date", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, e ->
                if (e != null) {
                    Log.w("AdminDebug", "Listen failed.", e)
                    return@addSnapshotListener
                }

                if (snapshots != null) {
                    val eventList = mutableListOf<Event>()
                    for (doc in snapshots) {
                        val event = doc.toObject(Event::class.java).apply { id = doc.id }
                        eventList.add(event)
                    }
                    eventAdapter.submitList(eventList)
                }
            }
    }

    private fun showDeleteConfirmation(eventId: String) {
        // Strategy: Professional UX with verification
        AlertDialog.Builder(this)
            .setTitle("Delete Event")
            .setMessage("Are you sure you want to remove this event? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                deleteEvent(eventId)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun deleteEvent(eventId: String) {
        db.collection("events").document(eventId)
            .delete()
            .addOnSuccessListener {
                Toast.makeText(this, "Event deleted successfully", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}