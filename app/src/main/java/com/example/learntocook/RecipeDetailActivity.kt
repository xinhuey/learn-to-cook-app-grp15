package com.example.learntocook

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.example.learntocook.databinding.RecipeDetailsBinding
import com.google.gson.Gson
import java.util.*

class RecipeDetailActivity : AppCompatActivity() {
    private lateinit var binding: RecipeDetailsBinding

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
        bindRecipe(recipe)
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
        binding.textCookTime.text = recipe.cookTime?.let { "Cook Time: $it min" } ?: ""
        binding.textServings.text = recipe.servings?.let { "Servings: $it" } ?: ""

        binding.textIngredients.text = recipe.ingredients.joinToString(separator = "\n• ", prefix = "• ")
        binding.textInstructions.text = recipe.instructions.mapIndexed { i, step -> "${i + 1}. $step" }.joinToString("\n")

        if (!recipe.tags.isNullOrEmpty()) {
            binding.textTags.text = "Tags: ${recipe.tags.joinToString(", ")}"
            binding.textTags.visibility = android.view.View.VISIBLE
        } else {
            binding.textTags.visibility = android.view.View.GONE
        }
    }
}
