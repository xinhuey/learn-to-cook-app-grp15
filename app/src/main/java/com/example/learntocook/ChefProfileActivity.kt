package com.example.learntocook

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import android.content.Intent
import android.view.View
import android.widget.Toast
import com.google.gson.Gson
import java.util.Date
import java.util.Locale
import java.text.SimpleDateFormat
import com.example.learntocook.databinding.ActivityChefProfileBinding

class ChefProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChefProfileBinding
    private lateinit var recipeAdapter: RecipeAdapter

    private val LOGGED_IN_USER_ID = "chef1"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChefProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val profileIdToDisplay = "chef1"

        // tmp mock profile. TODO: replace w/ api call
        val mockAuthor = createMockAuthor(profileIdToDisplay)
        val mockRecipes = createMockRecipes(mockAuthor)

        populateUi(mockAuthor)
        setupRecyclerView(mockRecipes)
        setupButtonClickListeners()

    }

    private fun populateUi(author: Author) {
        binding.textChefName.text = author.full_name
        binding.imageProfilePicture.setImageResource(R.drawable.ic_launcher_foreground) // tmp placeholder TODO
        binding.textFollowerCount.text = getString(R.string.follower_count_format, author.followerCount)
        binding.textChefSpecialty.text = author.specialty

        // Check if current profile belongs to the logged-in user & if they are chef
        if (author.id == LOGGED_IN_USER_ID && author.isChef) { // if its their own profile & they're chef they can edit
            binding.buttonFollowOrEdit.text = "Edit Profile"
            binding.buttonAddRecipe.visibility = View.VISIBLE
        } else {
            binding.buttonFollowOrEdit.text = "Follow"
            binding.buttonAddRecipe.visibility = View.GONE
        }
    }

    // to display list of recipes
    private fun setupRecyclerView(recipes: List<Recipe>) {
        recipeAdapter = RecipeAdapter(recipes) { clickedRecipe ->
            val intent = Intent(this, RecipeDetailActivity::class.java)
            val recipeJson = Gson().toJson(clickedRecipe)
            intent.putExtra("recipe_json", recipeJson)
            startActivity(intent)
        }

        binding.recyclerViewChefRecipes.apply {
            adapter = recipeAdapter
        }
    }

    private fun setupButtonClickListeners() {
        binding.buttonFollowOrEdit.setOnClickListener {
            val buttonText = binding.buttonFollowOrEdit.text.toString()
            Toast.makeText(this, "$buttonText button clicked!", Toast.LENGTH_SHORT).show()
        }

        // create mew recipe post
        binding.buttonAddRecipe.setOnClickListener {
            val intent = Intent(this, CreateBlogPostActivity::class.java)
            startActivity(intent)
        }
    }

    // tmp create fake Author obj for testing (TODO: get author data from backend)
    private fun createMockAuthor(id: String) : Author {
        return Author(
            id = id,
            full_name = "Isabella Rossi",
            profile_image_url = null,
            followerCount = 125,
            specialty = "Italian, Mediterranean",
            isChef = true
        )
    }

    // Tmp create fake list of Recipe obj for testing
    private fun createMockRecipes(author: Author): List<Recipe> {
        val now = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.getDefault()).format(Date())
        return listOf(
            Recipe(
                id = "recipe_abc",
                title = "Mediterranean Quinoa Salad",
                description = "A refreshing and healthy salad packed with flavor.",
                ingredients = listOf("1 cup quinoa", "2 cups water", "1 cucumber", "1 tomato"),
                instructions = listOf("Rinse quinoa.", "Boil water and cook quinoa.", "Chop vegetables.", "Mix all ingredients."),
                cuisine = "Mediterranean",
                difficulty = "Easy",
                prepTime = 15,
                cookTime = 20,
                servings = 4,
                imageUrls = emptyList(),
                tags = listOf("healthy", "salad", "vegetarian"),
                author = author,
                authorId = author.id,
                createdAt = now,
                updatedAt = now
            ),
            Recipe(
                id = "recipe_def",
                title = "Spicy Thai Green Curry",
                description = "A fragrant and flavorful Thai green curry with tofu and vegetables.",
                ingredients = listOf("1 block tofu", "1 can coconut milk", "2 tbsp green curry paste"),
                instructions = listOf("Press tofu.", "Sauté curry paste.", "Add coconut milk and tofu."),
                cuisine = "Thai",
                difficulty = "Medium",
                prepTime = 20,
                cookTime = 25,
                servings = 3,
                imageUrls = emptyList(),
                tags = listOf("spicy", "vegan", "curry"),
                author = author,
                authorId = author.id,
                createdAt = now,
                updatedAt = now
            )
        )
    }
}