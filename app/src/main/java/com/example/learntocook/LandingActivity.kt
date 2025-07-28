package com.example.learntocook

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.learntocook.databinding.ActivityLandingBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.*
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts

class LandingActivity : AppCompatActivity() {
//    maybe structure these files better / cleaner modular code...
    private lateinit var binding: ActivityLandingBinding
    private var allRecipes = listOf<Recipe>()
    private lateinit var adapter: RecipeAdapter

    private val gson = Gson()
    private val recipeListType = object : TypeToken<List<Recipe>>() {}.type

    companion object {
        private const val BASE_API_URL = "/recipes"
    }
    // whenever preferences are updated, refetch recipes from API
    private val preferencesLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("LandingActivity", "Preferences updated, refetching recipes")
            fetchRecipesFromApi()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLandingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonGoToProfile.setOnClickListener {
            val intent = Intent(this, ChefProfileActivity::class.java)
            // tmp mock values
            intent.putExtra("CHEF_ID", "chef1")
            startActivity(intent)
        }

        adapter = RecipeAdapter(emptyList()) { recipe ->
            Log.d("LandingActivity", "Recipe clicked: ${recipe.title}")
            val intent = Intent(this, RecipeDetailActivity::class.java)
            intent.putExtra("recipe_json", Gson().toJson(recipe))
            Log.d("LandingActivity", "Starting RecipeDetailActivity")
            startActivity(intent)
        }
        binding.recyclerViewRecipes.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewRecipes.adapter = adapter

        binding.preferences.setOnClickListener {
            val intent = Intent(this, PreferencesActivity::class.java)
            preferencesLauncher.launch(intent)
        }

        // only show the create post button if user is a chef
        val isChef = UserManager.getCurrentUserIsChef(this)
        if (isChef) {
            binding.fabCreatePost.visibility = android.view.View.VISIBLE
            binding.fabCreatePost.setOnClickListener{
                startActivity(Intent(this, CreateBlogPostActivity::class.java))
            }
        } else {
            binding.fabCreatePost.visibility = android.view.View.GONE
        }

        // fetch recipes from backend API with preferences
        fetchRecipesFromApi()

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                fetchRecipesFromApi(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    fetchRecipesFromApi()
                } else {
                    fetchRecipesFromApi(newText)
                }
                return true
            }
        })
    }

    private fun getUserPreferences(): Map<String, String> {
        val sharedPref = getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
        val preferences = mutableMapOf<String, String>()

        val preferredCuisines = sharedPref.getString("preferred_cuisines", "")
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
        if (!preferredCuisines.isNullOrEmpty()) {
            preferences["cuisine"] = preferredCuisines.first()
        }
        val preferredIngredients = sharedPref.getString("preferred_ingredients", "")?.trim()
        if (!preferredIngredients.isNullOrEmpty()) {
            preferences["ingredients"] = preferredIngredients
        }

        val allergies = mutableListOf<String>()
        if (sharedPref.getBoolean("allergy_dairy", false)) allergies.add("dairy")
        if (sharedPref.getBoolean("allergy_gluten", false)) allergies.add("gluten")
        if (sharedPref.getBoolean("allergy_nuts", false)) allergies.add("nuts")
        if (sharedPref.getBoolean("allergy_shellfish", false)) allergies.add("shellfish")
        if (sharedPref.getBoolean("allergy_soy", false)) allergies.add("soy")
        
        if (allergies.isNotEmpty()) {
            preferences["exclude_ingredients"] = allergies.joinToString(",")
        }
        
        Log.d("LandingActivity", "User preferences: $preferences")
        return preferences
    }

    private fun fetchRecipesFromApi(searchQuery: String? = null) {
        val preferences = getUserPreferences()
        val queryParams = mutableListOf<String>()
        
        // add user prefs as query params
        preferences.forEach { (key, value) ->
            queryParams.add("$key=$value")
        }

        searchQuery?.takeIf { it.isNotBlank() }?.let {
            queryParams.add("search=$it")
        }

        val endpoint = if (queryParams.isNotEmpty()) {
            "$BASE_API_URL?${queryParams.joinToString("&")}"
        } else {
            BASE_API_URL
        }

        Log.d("LandingActivity", "Fetching recipes from endpoint: $endpoint")

        ApiClient.makeRequest(
            context = this,
            endpoint = endpoint,
            method = "GET",
            onSuccess = { responseBody ->
                try {
                    val recipes: List<Recipe> = gson.fromJson(responseBody, recipeListType)
                    Log.d("LandingActivity", "Fetched ${recipes.size} recipes")
                    runOnUiThread {
                        allRecipes = recipes
                        adapter.updateRecipes(allRecipes)
                    }
                } catch (e: Exception) {
                    Log.e("LandingActivity", "Failed to parse recipes JSON", e)
                }
            },
            onError = { errorMessage ->
                Log.e("LandingActivity", "Failed to fetch recipes: $errorMessage")
            }
        )
    }
}
