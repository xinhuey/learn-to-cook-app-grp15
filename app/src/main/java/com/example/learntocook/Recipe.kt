package com.example.learntocook

import java.time.LocalDateTime
import java.util.UUID

data class Recipe(
    val id: UUID,
    val title: String,
    val description: String?,
    val ingredients: List<String>,
    val instructions: List<String>,
    val cuisine: String,
    val difficulty: String,
    val prepTime: Int?,
    val cookTime: Int?,
    val servings: Int?,
    val imageUrls: List<String>?,
    val tags: List<String>?,
    val author: Author?,
    val isPublic: Boolean = true,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)