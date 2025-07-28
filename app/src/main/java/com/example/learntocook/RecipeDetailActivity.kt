package com.example.learntocook

import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.learntocook.databinding.RecipeDetailsBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.picasso.Picasso
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

        reviewAdapter = ReviewAdapter(emptyList())
        binding.recyclerViewReviews.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewReviews.adapter = reviewAdapter

        // if the user is also author of this recipe, they shouldnt be able to post a review
        val currentUserId = UserManager.getCurrentUserId(this)
        Log.d("RecipeDetailActivity", "Current User ID: $currentUserId Recipe Author ID: ${recipe.authorId}")
        val isAuthor = currentUserId == recipe.authorId
        
        if (isAuthor) {
            // Hide the entire review submission card for recipe authors
            binding.cardAddReview.visibility = android.view.View.GONE
        } else {
            // Show review submission card for non-authors
            binding.cardAddReview.visibility = android.view.View.VISIBLE
            
            binding.buttonSubmitReview.setOnClickListener {
                submitReview(recipe.id)
            }
        }
    }

    private fun fetchReviews(recipeId: String) {
        val endpoint = "/recipes/$recipeId/reviews"
        
        ApiClient.makeRequest(
            context = this,
            endpoint = endpoint,
            method = "GET",
            onSuccess = { responseBody ->
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
            },
            onError = { errorMessage ->
                Log.d("RecipeDetailActivity", "No reviews found for recipe: $errorMessage")
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

        val json = JSONObject()
        json.put("rating", rating)
        json.put("comment", comment)

        val endpoint = "/recipes/$recipeId/reviews"
        
        ApiClient.makeRequest(
            context = this,
            endpoint = endpoint,
            method = "POST",
            body = json.toString(),
            onSuccess = { responseBody ->
                runOnUiThread {
                    binding.buttonSubmitReview.isEnabled = true
                    binding.buttonSubmitReview.text = "Submit Review"
                    
                    try {
                        val newReview = gson.fromJson(responseBody, Review::class.java)
                        reviewAdapter.addReview(newReview)
                        
                        binding.editTextReviewComment.text.clear()
                        binding.ratingBarReview.rating = 5.0f

                        Toast.makeText(this@RecipeDetailActivity, "Review submitted successfully!", Toast.LENGTH_SHORT).show()
                        
                        // Refresh reviews to get the updated list
                        fetchReviews(recipeId)
                    } catch (e: Exception) {
                        Log.e("RecipeDetailActivity", "Failed to parse review response", e)
                        Toast.makeText(this@RecipeDetailActivity, "Review submitted but failed to update UI", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onError = { errorMessage ->
                runOnUiThread {
                    binding.buttonSubmitReview.isEnabled = true
                    binding.buttonSubmitReview.text = "Submit Review"
                    Toast.makeText(this@RecipeDetailActivity, "Failed to submit review: $errorMessage", Toast.LENGTH_LONG).show()
                    Log.e("RecipeDetailActivity", "Review submission failed: $errorMessage")
                }
            }
        )
    }
}