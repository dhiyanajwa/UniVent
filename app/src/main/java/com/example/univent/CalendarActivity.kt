package com.example.univent

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.univent.databinding.ActivityCalendarBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.*

class CalendarActivity : AppCompatActivity() {

    private lateinit var binding: ActivityCalendarBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var adapter: StudentEventAdapter

    private var syncListener: ListenerRegistration? = null
    private var joinedEventsList = mutableListOf<Event>()
    private var lastSelectedDate: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityCalendarBinding.inflate(layoutInflater)
        setContentView(binding.root)

        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        setupRecyclerView()

        binding.btnBack.setOnClickListener { finish() }

        // Set initial date to today
        lastSelectedDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        binding.tvSelectedDate.text = "Events on: $lastSelectedDate"

        binding.calendarView.setOnDateChangeListener { _, year, month, dayOfMonth ->
            lastSelectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            binding.tvSelectedDate.text = "Events on: $lastSelectedDate"
            filterEventsByDate(lastSelectedDate)
        }

        startRealTimeSync()
    }

    private fun setupRecyclerView() {
        // FIX: Removed 'onJoinClick' and added 'onBookmarkClick' to match your actual Adapter
        adapter = StudentEventAdapter(
            onItemClick = { event ->
                val intent = Intent(this, EventDetailActivity::class.java)
                intent.putExtra("EVENT_ID", event.id)
                startActivity(intent)
            },
            onBookmarkClick = { event ->
                // Handle bookmark logic or leave empty
            }
        )
        binding.rvCalendarEvents.layoutManager = LinearLayoutManager(this)
        binding.rvCalendarEvents.adapter = adapter
    }

    private fun startRealTimeSync() {
        val userId = auth.currentUser?.uid ?: return

        syncListener = db.collection("users").document(userId)
            .addSnapshotListener { document, error ->
                if (error != null) {
                    Log.e("CalendarSync", "Sync failed: ${error.message}")
                    return@addSnapshotListener
                }

                val joinedIds = document?.get("joinedEvents") as? List<String> ?: emptyList()

                if (joinedIds.isNotEmpty()) {
                    fetchEventDetailsSync(joinedIds)
                } else {
                    joinedEventsList.clear()
                    filterEventsByDate(lastSelectedDate)
                }
            }
    }

    private fun fetchEventDetailsSync(joinedIds: List<String>) {
        db.collection("events")
            .whereIn("__name__", joinedIds)
            .addSnapshotListener { snapshots, error ->
                if (error != null) return@addSnapshotListener

                if (snapshots != null) {
                    joinedEventsList = snapshots.toObjects(Event::class.java).toMutableList()
                    filterEventsByDate(lastSelectedDate)
                }
            }
    }

    private fun filterEventsByDate(date: String) {
        val filtered = joinedEventsList.filter { it.date == date }
        adapter.submitList(filtered)

        if (filtered.isEmpty()) {
            binding.layoutEmptyState.visibility = View.VISIBLE
            binding.rvCalendarEvents.visibility = View.GONE
        } else {
            binding.layoutEmptyState.visibility = View.GONE
            binding.rvCalendarEvents.visibility = View.VISIBLE
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        syncListener?.remove()
    }
}