package com.example.learntocook

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.widget.SearchView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.learntocook.databinding.ActivityLandingBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import java.io.IOException
import java.util.*
import android.util.Log
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

class LandingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLandingBinding
    private val client = OkHttpClient()
    private var allRecipes = listOf<Recipe>()
    private lateinit var adapter: RecipeAdapter
    private val gson = Gson()
    private val recipeListType = object : TypeToken<List<Recipe>>() {}.type

    companion object {
        private const val RECIPES_API_URL = "https://d887-142-114-225-136.ngrok-free.app/api/recipes"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLandingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = RecipeAdapter(emptyList()) { recipe ->
            val intent = Intent(this, RecipeDetailActivity::class.java)
            intent.putExtra("recipe_json", Gson().toJson(recipe))
            startActivity(intent)
        }
        binding.recyclerViewRecipes.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewRecipes.adapter = adapter

        binding.preferences.setOnClickListener {
            val intent = Intent(this, PreferencesActivity::class.java)
            startActivityForResult(intent, 123)
        }

        fetchRecipesFromApi()

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterRecipes(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterRecipes(newText)
                return true
            }
        })
    }

    private fun getUserPreferences(): Map<String, String> {
        val sharedPref = getSharedPreferences("user_preferences", MODE_PRIVATE)
        val cuisines = sharedPref.getString("preferred_cuisines", "")?.split(",")?.filter { it.isNotBlank() } ?: emptyList()
        val cuisineFilter = cuisines.firstOrNull() ?: ""
        val ingredients = sharedPref.getString("preferred_ingredients", "")
        return mapOf(
            "cuisine" to cuisineFilter,
            "search" to (ingredients ?: "")
        )
    }

    private fun fetchRecipesFromApi() {
        val prefs = getUserPreferences()
        val urlBuilder = RECIPES_API_URL.toHttpUrlOrNull()?.newBuilder()
        prefs.forEach { (key, value) ->
            if (value.isNotEmpty()) {
                urlBuilder?.addQueryParameter(key, value)
            }
        }
        urlBuilder?.addQueryParameter("page", "1")
        urlBuilder?.addQueryParameter("limit", "20")
        val url = urlBuilder?.build().toString()
        Log.d("LandingActivity", "Requesting recipes with URL: $url")

        val request = Request.Builder().url(url).build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) return
                    val body = response.body?.string()
                    if (body != null) {
                        val recipes: List<Recipe> = gson.fromJson(body, recipeListType)
                        Log.d("LandingActivity", "Fetched recipes: $recipes")
                        runOnUiThread {
                            allRecipes = recipes
                            adapter.updateRecipes(allRecipes)
                        }
                    }
                }
            }
        })
    }

    private fun filterRecipes(query: String?) {
        val filtered = if (query.isNullOrBlank()) {
            allRecipes
        } else {
            val lowerQuery = query.lowercase(Locale.getDefault())
            allRecipes.filter {
                it.title.lowercase(Locale.getDefault()).contains(lowerQuery) ||
                        it.description.orEmpty().lowercase(Locale.getDefault()).contains(lowerQuery)
            }
        }
        adapter.updateRecipes(filtered)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 123 && resultCode == RESULT_OK) {
            fetchRecipesFromApi()
        }
    }
}
