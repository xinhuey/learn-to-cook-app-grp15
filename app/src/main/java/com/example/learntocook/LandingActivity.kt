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
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import java.util.*
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts

class LandingActivity : AppCompatActivity() {
//    maybe structure these files better / cleaner modular code...
    private lateinit var binding: ActivityLandingBinding
    private val client = OkHttpClient()
    private var allRecipes = listOf<Recipe>()
    private lateinit var adapter: RecipeAdapter

    private val gson = Gson()
    private val recipeListType = object : TypeToken<List<Recipe>>() {}.type

    companion object {
        private const val BASE_API_URL = "https://learn-to-cook-api.uwgroup15projectapp.workers.dev/api/recipes"
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
        val baseUrl = BASE_API_URL.toHttpUrlOrNull()
        if (baseUrl == null) {
            Log.e("LandingActivity", "Invalid base URL")
            return
        }

        val urlBuilder = baseUrl.newBuilder()
        
        // add user prefs as query prms
        val preferences = getUserPreferences()
        preferences.forEach { (key, value) ->
            urlBuilder.addQueryParameter(key, value)
        }

        searchQuery?.takeIf { it.isNotBlank() }?.let {
            urlBuilder.addQueryParameter("search", it)
        }

        val url = urlBuilder.build().toString()
        Log.d("LandingActivity", "Fetching recipes from: $url")

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Log.e("LandingActivity", "Failed to fetch recipes", e)
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) {
                        Log.e("LandingActivity", "api request failed: ${response.code}")
                        return
                    }

                    val body = response.body?.string()
                    if (body != null) {
                        try {
                            val recipes: List<Recipe> = gson.fromJson(body, recipeListType)
                            Log.d("LandingActivity", "Fetched ${recipes.size} recipes")
                            runOnUiThread {
                                allRecipes = recipes
                                adapter.updateRecipes(allRecipes)
                            }
                        } catch (e: Exception) {
                            Log.e("LandingActivity", "failed to parse recipes JSON", e)
                        }
                    }
                }
            }
        })
    }
}
