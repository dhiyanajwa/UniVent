package com.yourpackage.name // Replace with your actual package name

import com.google.firebase.firestore.IgnoreExtraProperties


@IgnoreExtraProperties
data class User(
    val uid: String = "",
    val email: String = "",
    val role: String = "student", // Default role is student
    val bookmarkedEvents: List<String> = emptyList() // To satisfy "Bookmark" feature
)