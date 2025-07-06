package com.example.learntocook

import com.google.gson.annotations.SerializedName

data class Review(
    val id: String,
    @SerializedName("recipe_id")
    val recipeId: String,
    @SerializedName("user_id")
    val userId: String,
    val rating: Int,
    val comment: String,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    val user: Author
) 