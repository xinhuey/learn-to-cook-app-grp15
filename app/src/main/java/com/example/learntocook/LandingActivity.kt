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

class LandingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityLandingBinding
    private val client = OkHttpClient()
    private var allRecipes = listOf<Recipe>()
    private lateinit var adapter: RecipeAdapter

    private val gson = Gson()
    private val recipeListType = object : TypeToken<List<Recipe>>() {}.type

    companion object {
        private const val RECIPES_API_URL = "https://2174-24-114-29-54.ngrok-free.app/api/recipes"
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
            startActivity(intent)
        }

        // fetch recipes from backend API
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

    private fun fetchRecipesFromApi() {
        val request = Request.Builder()
            .url(RECIPES_API_URL)
            .build()

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
}
