package com.example.learntocook

import com.google.gson.annotations.SerializedName
//need this serialized name for the api req/responses stuff
data class Recipe(
    val id: String,
    val title: String,
    val description: String?,
    val ingredients: List<String>,
    val instructions: List<String>,
    val cuisine: String,
    val difficulty: String,
    @SerializedName("prep_time")
    val prepTime: Int?,
    @SerializedName("cook_time")
    val cookTime: Int?,
    val servings: Int?,
    @SerializedName("image_urls")
    val imageUrls: List<String>?,
    val tags: List<String>?,
    val author: Author?,
    @SerializedName("author_id")
    val authorId: String?,
    @SerializedName("is_public")
    val isPublic: Boolean = true,
    @SerializedName("created_at")
    val createdAt: String,
    @SerializedName("updated_at")
    val updatedAt: String,
    @SerializedName("average_rating")
    val averageRating: Double? = null,
    @SerializedName("review_count")
    val reviewCount: Int? = null,
    @SerializedName("recipe_reviews")
    val recipeReviews: List<Review>? = null
)