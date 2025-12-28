package com.example.univent

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.animation.BounceInterpolator
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.bumptech.glide.Glide
import com.example.univent.databinding.ActivityEventDetailBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class EventDetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEventDetailBinding
    private lateinit var db: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private var eventId: String? = null
    private var isBookmarked = false

    private var eventTitle: String = ""
    private var eventDateStr: String = ""
    private var eventTimeStr: String = ""

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

            binding.ivBookmark.setOnClickListener {
                toggleBookmark()
            }

            binding.btnJoinEvent.setOnClickListener {
                joinEvent()
            }
        }

        binding.btnBack.setOnClickListener {
            handleBackNavigation()
        }
    }

    private fun loadEventDetails() {
        val currentEventId = eventId ?: return
        db.collection("events").document(currentEventId).get()
            .addOnSuccessListener { doc ->
                val event = doc.toObject(Event::class.java)
                event?.let {
                    eventTitle = it.title
                    eventDateStr = it.date
                    eventTimeStr = it.time

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

    private fun joinEvent() {
        val userId = auth.currentUser?.uid ?: return
        val currentEventId = eventId ?: return

        db.collection("users").document(userId)
            .update("joinedEvents", FieldValue.arrayUnion(currentEventId))
            .addOnSuccessListener {
                handleFCMSubscriptions(currentEventId, isJoined = true, isBookmarked = false)
                scheduleLocalReminder(eventTitle, eventDateStr, eventTimeStr, currentEventId, false)
                Toast.makeText(this, "Joined!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Failed to join: ${e.message}", Toast.LENGTH_SHORT).show()
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
                .addOnSuccessListener {
                    handleFCMSubscriptions(currentEventId, isJoined = false, isBookmarked = true)
                    scheduleLocalReminder(eventTitle, eventDateStr, eventTimeStr, currentEventId, true)
                    Toast.makeText(this, "Bookmarked!", Toast.LENGTH_SHORT).show()
                }
        } else {
            userRef.update("bookmarkedEvents", FieldValue.arrayRemove(currentEventId))
                .addOnSuccessListener {
                    FirebaseMessaging.getInstance().unsubscribeFromTopic("bookmark_$currentEventId")
                    WorkManager.getInstance(this).cancelAllWorkByTag("bookmark_rem_$currentEventId")
                }
        }
    }

    private fun scheduleLocalReminder(title: String, date: String, time: String, eventId: String, isForBookmark: Boolean) {
        val userId = auth.currentUser?.uid ?: return

        db.collection("users").document(userId).get().addOnSuccessListener { document ->
            val isPushEnabled = document.getBoolean("pushEnabled") ?: false

            if (!isPushEnabled) {
                Log.d("NOTIF_LOG", "Push disabled. Skipping reminder.")
                return@addOnSuccessListener
            }

            try {
                val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                val eventDateObj = dateTimeFormat.parse("$date $time")

                if (eventDateObj != null) {
                    val currentTime = System.currentTimeMillis()
                    val eventTime = eventDateObj.time
                    val twentyFourHoursInMs = 24 * 60 * 60 * 1000L

                    // Calculation: (Event Time - 24 Hours) - Current Time
                    val delay = (eventTime - twentyFourHoursInMs) - currentTime

                    if (delay > 0) {
                        val message = if (isForBookmark) {
                            "Your bookmarked event '$title' is tomorrow!"
                        } else {
                            "Get ready! Event '$title' is tomorrow."
                        }

                        val workTag = if (isForBookmark) "bookmark_rem_$eventId" else "join_rem_$eventId"
                        val data = workDataOf("title" to message, "eventId" to eventId)

                        WorkManager.getInstance(this).cancelAllWorkByTag(workTag)

                        val reminderRequest = OneTimeWorkRequestBuilder<EventWorker>()
                            .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                            .setInputData(data)
                            .addTag(workTag)
                            .build()

                        WorkManager.getInstance(this).enqueue(reminderRequest)
                        Log.d("NOTIF_LOG", "Reminder set for 24h before: $title")
                    } else {
                        Log.d("NOTIF_LOG", "Event is less than 24h away. No reminder needed.")
                    }
                }
            } catch (e: Exception) {
                Log.e("NOTIF_LOG", "Error: ${e.message}")
            }
        }
    }

    private fun handleFCMSubscriptions(eventId: String, isJoined: Boolean, isBookmarked: Boolean) {
        val fm = FirebaseMessaging.getInstance()
        if (isJoined) {
            fm.subscribeToTopic("joined_$eventId")
            fm.unsubscribeFromTopic("bookmark_$eventId")
        } else if (isBookmarked) {
            fm.subscribeToTopic("bookmark_$eventId")
            fm.unsubscribeFromTopic("joined_$eventId")
        }
    }

    private fun checkBookmarkStatus() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("users").document(userId).get()
            .addOnSuccessListener { doc ->
                val bookmarks = doc.get("bookmarkedEvents") as? List<*>
                val bookmarkList = bookmarks?.filterIsInstance<String>() ?: emptyList()
                isBookmarked = bookmarkList.contains(eventId)
                updateBookmarkUI(false)
            }
    }

    private fun updateBookmarkUI(shouldAnimate: Boolean) {
        val icon = if (isBookmarked) R.drawable.ic_bookmark_filled else R.drawable.ic_bookmark_outline
        binding.ivBookmark.setImageResource(icon)

        if (shouldAnimate) {
            binding.ivBookmark.animate().scaleX(1.4f).scaleY(1.4f).setDuration(200)
                .setInterpolator(BounceInterpolator())
                .withEndAction {
                    binding.ivBookmark.animate().scaleX(1.0f).scaleY(1.0f).setDuration(200).start()
                }.start()
        }
    }

    private fun handleBackNavigation() {
        if (isTaskRoot) {
            val intent = Intent(this, CatalogActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            startActivity(intent)
        }
        finish()
    }

    override fun onBackPressed() {
        handleBackNavigation()
        super.onBackPressed()
    }
}