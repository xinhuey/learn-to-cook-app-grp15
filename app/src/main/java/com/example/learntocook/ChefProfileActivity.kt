package com.example.learntocook

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import android.content.Intent
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.google.gson.Gson
import com.example.learntocook.databinding.ActivityChefProfileBinding
import com.google.gson.reflect.TypeToken
import org.json.JSONArray

class ChefProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChefProfileBinding
    private lateinit var recipeAdapter: RecipeAdapter
    private val gson = Gson()
    private var currAuthor: Author? = null
    private var isFollowing = false

    // launcher to handle resutl from editing profile
    private val editProfileLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("ChefProfileActivity", "Profile updated, refreshing data.")
            currAuthor?.id?.let { fetchChefProfile(it) }
        }
    }

    private val addRecipeLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Log.d("ChefProfileActivity", "New recipe added, refreshing recipes.")
            currAuthor?.id?.let { fetchChefRecipes(it) }
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChefProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val profileIdToDisplay = intent.getStringExtra("CHEF_ID")
        if (profileIdToDisplay == null) {
            Toast.makeText(this, "Profile not found", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupRecyclerView()

        fetchChefProfile(profileIdToDisplay)

        setupButtonClickListeners()

    }

    private fun fetchChefProfile(profileId: String) {
        binding.progressBar.visibility = View.VISIBLE
        val endpoint = "/users/$profileId"
        Log.d("ChefProfileActivity", "Fetching profile from: $endpoint")

        ApiClient.makeRequest(
            context = this,
            endpoint = endpoint,
            method = "GET",
            onSuccess = { responseBody ->
                try {
                    val author = gson.fromJson(responseBody, Author::class.java)
                    this.currAuthor = author
                    runOnUiThread {
                        populateUi(author)
                        fetchChefRecipes(profileId)
                        checkFollowStatus(profileId)
                    }
                } catch (e: Exception) {
                    Log.e("ChefProfileActivity", "Failed to parse author JSON", e)
                    runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        Toast.makeText(this, "Error loading profile", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onError = { errorMessage ->
                Log.e("ChefProfileActivity", "API Error fetching profile: $errorMessage")
                runOnUiThread {
                    binding.progressBar.visibility = View.GONE
                    Toast.makeText(this, "Could not load profile: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
        )

    }

    private fun fetchChefRecipes(authorId: String) {
        val endpoint = "/users/$authorId/recipes"
        Log.d("ChefProfileActivity", "Fetching recipes from: $endpoint")

        ApiClient.makeRequest(
            context = this,
            endpoint = endpoint,
            method = "GET",
            onSuccess = { responseBody ->
                try {
                    val recipeListType = object : TypeToken<List<Recipe>>() {}.type
                    val recipes = gson.fromJson<List<Recipe>>(responseBody, recipeListType)
                    runOnUiThread {
                        binding.progressBar.visibility = View.GONE
                        recipeAdapter.updateRecipes(recipes)
                    }
                } catch (e: Exception) {
                    Log.e("ChefProfileActivity", "Failed to parse recipes JSON", e)
                    runOnUiThread { binding.progressBar.visibility = View.GONE }
                }
            },
            onError = { errorMessage ->
                Log.e("ChefProfileActivity", "API Error fetching recipes: $errorMessage")
                runOnUiThread { binding.progressBar.visibility = View.GONE }
            }
        )

    }

    private fun checkFollowStatus(profileId: String) {
        val loggedInUserId = UserManager.getCurrentUserId(this) ?: return

        val endpoint = "/users/$profileId/followers"

        ApiClient.makeRequest(
            context = this,
            endpoint = endpoint,
            method = "GET",
            onSuccess = { responseBody ->
                try {
                    val followers = JSONArray(responseBody)
                    for (i in 0 until followers.length()) {
                        val follower = followers.getJSONObject(i)
                        if (follower.getString("id") == loggedInUserId) {
                            isFollowing = true
                            break
                        }
                    }
                    updateFollowButtonState()
                } catch (e: Exception) {
                    Log.e("ChefProfileActivity", "Failed to parse followers list", e)
                }
            },
            onError = {
                isFollowing = false
                updateFollowButtonState()
            }
        )
    }

    private fun populateUi(author: Author) {
        binding.textChefName.text = author.full_name
        binding.imageProfilePicture.setImageResource(R.drawable.ic_launcher_foreground) // TODO replace w/ actual img
        binding.textFollowerCount.text = getString(R.string.follower_count_format, author.followerCount)
        binding.textChefSpecialty.text = author.specialty

        val loggedInUserId = UserManager.getCurrentUserId(this)

        // Check if current profile belongs to the logged-in user & if they are chef
        if (author.id == loggedInUserId && author.isChef) { // if its their own profile & they're chef they can edit
            binding.buttonFollowOrEdit.text = "Edit Profile"
            binding.buttonAddRecipe.visibility = View.VISIBLE
        } else {
            binding.buttonFollowOrEdit.text = "Follow"
            binding.buttonAddRecipe.visibility = View.GONE
        }
    }

    private fun updateFollowButtonState() {
        runOnUiThread {
            if (isFollowing) {
                binding.buttonFollowOrEdit.text = "Unfollow"
            } else {
                binding.buttonFollowOrEdit.text = "Follow"
            }
        }
    }

    private fun handleFollowClick() {
        val profileId = currAuthor?.id ?: return
        binding.buttonFollowOrEdit.isEnabled = false

        val endpoint = "/follow/$profileId"
        val method = if (isFollowing) "DELETE" else "POST"

        ApiClient.makeRequest(
            context = this,
            endpoint = endpoint,
            method = method,
            onSuccess = {
                runOnUiThread {
                    isFollowing = !isFollowing
                    var currentFollowers = currAuthor?.followerCount ?: 0
                    if (isFollowing) {
                        currentFollowers++
                    } else {
                        currentFollowers--
                    }
                    currAuthor = currAuthor?.copy(followerCount = currentFollowers)
                    binding.textFollowerCount.text = getString(R.string.follower_count_format, currentFollowers)

                    updateFollowButtonState()
                    binding.buttonFollowOrEdit.isEnabled = true
                }
            },
            onError = { errorMessage ->
                runOnUiThread {
                    Toast.makeText(this, "Action failed: $errorMessage", Toast.LENGTH_SHORT).show()
                    binding.buttonFollowOrEdit.isEnabled = true
                }
            }
        )
    }

    // to display list of recipes
    private fun setupRecyclerView() {
        recipeAdapter = RecipeAdapter(emptyList()) { clickedRecipe ->
            val intent = Intent(this, RecipeDetailActivity::class.java)
            val recipeJson = Gson().toJson(clickedRecipe)
            intent.putExtra("recipe_json", recipeJson)
            startActivity(intent)
        }

        binding.recyclerViewChefRecipes.apply {
            layoutManager = GridLayoutManager(this@ChefProfileActivity, 2)
            adapter = recipeAdapter
        }
    }

    private fun setupButtonClickListeners() {

        binding.buttonBack.setOnClickListener {
            finish()
        }


        binding.buttonFollowOrEdit.setOnClickListener {
            val buttonText = binding.buttonFollowOrEdit.text.toString()
            if (buttonText == "Edit Profile") {
                currAuthor?.let { author ->
                    val intent = Intent(this, EditProfileActivity::class.java).apply {
                        putExtra("USER_NAME", author.full_name)
//                        putExtra("USER_SPECIALTY", author.specialty)
                    }
                    editProfileLauncher.launch(intent)
                }
            } else {
                // TODO: handle follow logic
                handleFollowClick()
                Toast.makeText(this, "$buttonText button clicked!", Toast.LENGTH_SHORT).show()
            }
        }

        // Create new recipe post
        binding.buttonAddRecipe.setOnClickListener {
            val intent = Intent(this, CreateBlogPostActivity::class.java)
            addRecipeLauncher.launch(intent)
//            startActivity(intent)
        }
    }

    // tmp create fake Author obj for testing
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

}