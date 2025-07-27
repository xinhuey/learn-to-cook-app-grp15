package com.example.learntocook

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.learntocook.databinding.RecipeDetailsBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.json.JSONObject
import java.util.*

class RecipeDetailActivity : AppCompatActivity() {
    private lateinit var binding: RecipeDetailsBinding
    private lateinit var reviewAdapter: ReviewAdapter
    private val gson = Gson()
    private var currentRecipe: Recipe? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = RecipeDetailsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val recipeJson = intent.getStringExtra("recipe_json")
        if (recipeJson == null) {
            finish() // no data, close activity
            return
        }

        val recipe = Gson().fromJson(recipeJson, Recipe::class.java)
        currentRecipe = recipe
        bindRecipe(recipe)
        
        // Fetch reviews separately
        fetchReviews(recipe.id)
    }

    private fun bindRecipe(recipe: Recipe) {
        binding.buttonBack.setOnClickListener {
            finish()
        }

        binding.textTitle.text = recipe.title
        binding.textCuisine.text = recipe.cuisine.ifEmpty { "Unknown Cuisine" }
        binding.textDifficulty.text = recipe.difficulty.ifEmpty { "Difficulty N/A" }
        binding.textDescription.text = recipe.description ?: ""
        binding.textAuthor.text = recipe.author?.full_name?.let { "By $it" } ?: ""

        binding.textPrepTime.text = recipe.prepTime?.let { "Prep Time: $it min" } ?: ""
        binding.textServings.text = recipe.servings?.let { "Servings: $it" } ?: ""

        binding.textIngredients.text = recipe.ingredients.joinToString(separator = "\n• ", prefix = "• ")
        binding.textInstructions.text = recipe.instructions.mapIndexed { i, step -> "${i + 1}. $step" }.joinToString("\n")

        if (!recipe.tags.isNullOrEmpty()) {
            binding.textTags.text = "Tags: ${recipe.tags.joinToString(", ")}"
            binding.textTags.visibility = android.view.View.VISIBLE
        } else {
            binding.textTags.visibility = android.view.View.GONE
        }

        // Setup empty reviews RecyclerView initially
        reviewAdapter = ReviewAdapter(emptyList())
        binding.recyclerViewReviews.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewReviews.adapter = reviewAdapter

        // Setup review submission
        binding.buttonSubmitReview.setOnClickListener {
            submitReview(recipe.id)
        }
    }

    private fun fetchReviews(recipeId: String) {
        ApiClient.makeRequest(
            context = this,
            endpoint = "/recipes/$recipeId/reviews",
            onSuccess = { response ->
                try {
                    val reviewListType = object : TypeToken<List<Review>>() {}.type
                    val reviews: List<Review> = gson.fromJson(response, reviewListType)
                    runOnUiThread {
                        reviewAdapter.updateReviews(reviews)
                        Log.d("RecipeDetailActivity", "Loaded ${reviews.size} reviews")
                    }
                } catch (e: Exception) {
                    Log.e("RecipeDetailActivity", "Failed to parse reviews", e)
                }
            },
            onError = { error ->
                Log.e("RecipeDetailActivity", "Failed to fetch reviews: $error")
                // Don't show error toast - reviews are optional
            }
        )
    }

    private fun submitReview(recipeId: String) {
        val rating = binding.ratingBarReview.rating.toInt()
        val comment = binding.editTextReviewComment.text.toString().trim()

        if (comment.isEmpty()) {
            Toast.makeText(this, "Please write a comment", Toast.LENGTH_SHORT).show()
            return
        }

        if (rating == 0) {
            Toast.makeText(this, "Please select a rating", Toast.LENGTH_SHORT).show()
            return
        }

        // Show loading state
        binding.buttonSubmitReview.isEnabled = false
        binding.buttonSubmitReview.text = "Submitting..."

        val reviewData = JSONObject().apply {
            put("rating", rating)
            put("comment", comment)
        }

        ApiClient.makeRequest(
            context = this,
            endpoint = "/recipes/$recipeId/reviews",
            method = "POST",
            body = reviewData.toString(),
            onSuccess = { response ->
                runOnUiThread {
                    binding.buttonSubmitReview.isEnabled = true
                    binding.buttonSubmitReview.text = "Submit Review"

                    try {
                        val newReview = Review(
                            id = "", // set in the backend later
                            recipeId = recipeId,
                            userId = UserManager.getCurrentUserId(this) ?: "",
                            rating = rating,
                            comment = comment,
                            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault()).format(java.util.Date()),
                            updatedAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault()).format(java.util.Date()),
                            user = Author(
                                id = UserManager.getCurrentUserId(this) ?: "",
                                full_name = UserManager.getCurrentUserName(this) ?: "Demo User",
                                profile_image_url = null
                            )
                        )
                        
                        // add new review immediately
                        reviewAdapter.addReview(newReview)
                        
                        binding.editTextReviewComment.text.clear()
                        binding.ratingBarReview.rating = 5.0f

                        Toast.makeText(this, "Review submitted successfully!", Toast.LENGTH_SHORT).show()
                        
                        fetchReviews(recipeId)
                    } catch (e: Exception) {
                        Log.e("RecipeDetailActivity", "Error creating review object", e)
                        Toast.makeText(this, "Review submitted successfully!", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onError = { error ->
                runOnUiThread {
                    binding.buttonSubmitReview.isEnabled = true
                    binding.buttonSubmitReview.text = "Submit Review"
                    Toast.makeText(this, "Failed to submit review: $error", Toast.LENGTH_SHORT).show()
                    Log.e("RecipeDetailActivity", "Failed to submit review: $error")
                }
            }
        )
    }
}