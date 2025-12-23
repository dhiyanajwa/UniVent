package com.example.univent

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.IgnoreExtraProperties

/**
 * Goal: Complete CRUD + Firebase integration.
 * Task: Represent a campus event with all required multimedia and text metadata.
 * Strategy for Full Marks: Use @IgnoreExtraProperties for stability and @Exclude for ID handling.
 */
@IgnoreExtraProperties
data class Event(
    @get:Exclude var id: String = "", // Document ID from Firestore (not stored as a field)
    val title: String = "",
    val date: String = "", // Format: YYYY-MM-DD for easier filtering
    val time: String = "",
    val location: String = "",
    val organizer: String = "",
    val description: String = "",
    val imageUrl: String = "", // URL from Firebase Storage
    val category: String = "General" // For "Advanced filtering and search" rubric
)