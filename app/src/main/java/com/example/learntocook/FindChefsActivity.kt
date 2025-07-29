package com.example.learntocook

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.GridLayoutManager
import com.example.learntocook.databinding.ActivityFindChefsBinding
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class FindChefsActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFindChefsBinding
    private lateinit var chefAdapter: ChefAdapter
    private var allChefs = listOf<Author>()
    private val gson = Gson()
    private val chefListType = object : TypeToken<List<Author>>() {}.type

    companion object {
        private const val BASE_API_URL = "/chefs"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFindChefsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupRecyclerView()
        setupSearchView()
        setupButtonClickListeners()
        fetchChefsFromApi()
    }

    private fun setupRecyclerView() {
        chefAdapter = ChefAdapter(emptyList()) { chef ->
            Log.d("FindChefsActivity", "Chef clicked: ${chef.full_name}")
            val intent = Intent(this, ChefProfileActivity::class.java)
            intent.putExtra("CHEF_ID", chef.id)
            startActivity(intent)
        }

        binding.recyclerViewChefs.apply {
            layoutManager = GridLayoutManager(this@FindChefsActivity, 2)
            adapter = chefAdapter
        }
    }

    private fun setupSearchView() {
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchChefs(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) {
                    fetchChefsFromApi()
                } else {
                    searchChefs(newText)
                }
                return true
            }
        })
    }

    private fun setupButtonClickListeners() {
        binding.buttonBack.setOnClickListener {
            finish()
        }
    }

    private fun fetchChefsFromApi() {
        binding.progressBar.visibility = View.VISIBLE
        val endpoint = BASE_API_URL

        Log.d("FindChefsActivity", "Fetching chefs from endpoint: $endpoint")

        ApiClient.makeRequest(
            context = this,
            endpoint = endpoint,
            method = "GET",
            onSuccess = { responseBody ->
                try {
                    val chefs: List<Author> = gson.fromJson(responseBody, chefListType)
                    Log.d("FindChefsActivity", "Fetched ${chefs.size} chefs")
                    runOnUiThread {
                        allChefs = chefs
                        chefAdapter.updateChefs(allChefs)
                        binding.progressBar.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    Log.e("FindChefsActivity", "Failed to parse chefs JSON", e)
                    runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this, "Error loading chefs", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onError = { errorMessage ->
                Log.e("FindChefsActivity", "Failed to fetch chefs: $errorMessage")
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Could not load chefs: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun searchChefs(searchQuery: String?) {
        if (searchQuery.isNullOrBlank()) {
            fetchChefsFromApi()
            return
        }

        binding.progressBar.visibility = View.VISIBLE
        val endpoint = "/chefs/search?q=$searchQuery"

        Log.d("FindChefsActivity", "Searching chefs from endpoint: $endpoint")

        ApiClient.makeRequest(
            context = this,
            endpoint = endpoint,
            method = "GET",
            onSuccess = { responseBody ->
                try {
                    val chefs: List<Author> = gson.fromJson(responseBody, chefListType)
                    Log.d("FindChefsActivity", "Found ${chefs.size} chefs for query: $searchQuery")
                    runOnUiThread {
                        chefAdapter.updateChefs(chefs)
                        binding.progressBar.visibility = View.GONE
                    }
                } catch (e: Exception) {
                    Log.e("FindChefsActivity", "Failed to parse search results JSON", e)
                    runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this, "Error searching chefs", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onError = { errorMessage ->
                Log.e("FindChefsActivity", "Failed to search chefs: $errorMessage")
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Could not search chefs: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
        )
    }
} 