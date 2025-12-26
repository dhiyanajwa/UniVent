package com.example.univent

import com.google.firebase.firestore.IgnoreExtraProperties


@IgnoreExtraProperties
data class User(
    val uid: String = "",
    val email: String = "",
    val role: String = "student", // Default role is student
    val bookmarkedEvents: List<String> = emptyList(), // To satisfy "Bookmark" feature
    val joinedEvents: List<String> = emptyList() // For calendar sync
)