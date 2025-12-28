package com.example.univent

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class EventWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val message = inputData.getString("title") ?: "You have an upcoming event!"
        val eventId = inputData.getString("eventId") ?: ""

        val db = FirebaseFirestore.getInstance()
        val auth = FirebaseAuth.getInstance()
        val userId = auth.currentUser?.uid ?: return Result.failure()

        try {
            val document = db.collection("users").document(userId).get().await()
            val isPushEnabled = document.getBoolean("pushEnabled") ?: false

            if (!isPushEnabled) {
                Log.d("WORKER_CHECK", "Notification aborted: pushEnabled is FALSE in Firestore.")
                return Result.success()
            }

            sendNotification(message, eventId)
            return Result.success()

        } catch (e: Exception) {
            Log.e("WORKER_CHECK", "Error checking settings: ${e.message}")
            return Result.retry()
        }
    }

    private fun sendNotification(message: String, eventId: String) {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "univent_notifications"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId, "Event Notifications", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for events"
                enableVibration(true)
            }
            manager.createNotificationChannel(channel)
        }

        val intent = Intent(applicationContext, EventDetailActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("EVENT_ID", eventId)
        }

        val pendingIntent = PendingIntent.getActivity(
            applicationContext, eventId.hashCode(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, channelId)
            .setSmallIcon(R.drawable.ic_alarm)
            .setContentTitle("Event Reminder")
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        manager.notify(eventId.hashCode(), notification)
    }
}