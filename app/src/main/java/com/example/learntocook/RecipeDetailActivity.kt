package com.example.learntocook

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.learntocook.databinding.RecipeDetailsBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.picasso.Picasso
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.*

class RecipeDetailActivity : AppCompatActivity() {
    private lateinit var binding: RecipeDetailsBinding
    private lateinit var reviewAdapter: ReviewAdapter
    private val client = OkHttpClient()
    private val gson = Gson()
    private var currentRecipe: Recipe? = null

    companion object {
        private const val BASE_API_URL = "https://learn-to-cook-api.uwgroup15projectapp.workers.dev/api"
    }

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

        val url = recipe.imageUrls?.firstOrNull()
        if (url != null) {
            Picasso.get().load(url).into(binding.imageRecipe)
        } else {
            binding.imageRecipe.setImageDrawable(null)
        }

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
        val url = "$BASE_API_URL/recipes/$recipeId/reviews"
        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Log.e("RecipeDetailActivity", "Failed to fetch reviews", e)
                    // Don't show error toast - reviews are optional
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (response.isSuccessful) {
                        response.body?.string()?.let { responseBody ->
                            try {
                                val reviewListType = object : TypeToken<List<Review>>() {}.type
                                val reviews: List<Review> = gson.fromJson(responseBody, reviewListType)
                                runOnUiThread {
                                    reviewAdapter.updateReviews(reviews)
                                    Log.d("RecipeDetailActivity", "Loaded ${reviews.size} reviews")
                                }
                            } catch (e: Exception) {
                                Log.e("RecipeDetailActivity", "Failed to parse reviews", e)
                            }
                        }
                    } else {
                        Log.d("RecipeDetailActivity", "No reviews found for recipe")
                    }
                }
            }
        })
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

        val json = JSONObject()
        json.put("rating", rating)
        json.put("comment", comment)

        val requestBody = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("$BASE_API_URL/recipes/$recipeId/reviews")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    binding.buttonSubmitReview.isEnabled = true
                    binding.buttonSubmitReview.text = "Submit Review"
                    Toast.makeText(this@RecipeDetailActivity, "Failed to submit review", Toast.LENGTH_SHORT).show()
                    Log.e("RecipeDetailActivity", "Failed to submit review", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    binding.buttonSubmitReview.isEnabled = true
                    binding.buttonSubmitReview.text = "Submit Review"

                    if (response.isSuccessful) {
                        val newReview = Review(
                            id = "", // set in the backend later
                            recipeId = recipeId,
                            userId = "012a66f5-96d2-427b-859c-347c67cc35ee",
                            rating = rating,
                            comment = comment,
                            createdAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault()).format(java.util.Date()),
                            updatedAt = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", java.util.Locale.getDefault()).format(java.util.Date()),
                            user = Author(
                                id = "012a66f5-96d2-427b-859c-347c67cc35ee",
                                full_name = "Demo User",
                                profile_image_url = null,
                                specialty = "null"

                            )
                        )
                        
                        // add new review immediately
                        reviewAdapter.addReview(newReview)
                        
                        binding.editTextReviewComment.text.clear()
                        binding.ratingBarReview.rating = 5.0f

                        Toast.makeText(this@RecipeDetailActivity, "Review submitted successfully!", Toast.LENGTH_SHORT).show()
                        
                        fetchReviews(recipeId)
                    } else {
                        val errorMessage = try {
                            val errorBody = response.body?.string()
                            if (errorBody != null) {
                                val errorJson = JSONObject(errorBody)
                                errorJson.getString("error")
                            } else {
                                "Failed to submit review"
                            }
                        } catch (e: Exception) {
                            "Failed to submit review"
                        }
                        
                        Toast.makeText(this@RecipeDetailActivity, errorMessage, Toast.LENGTH_LONG).show()
                        Log.e("RecipeDetailActivity", "Review submission failed with code: ${response.code}")
                    }
                }
            }
        })
    }
}