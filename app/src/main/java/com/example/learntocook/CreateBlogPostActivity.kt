package com.example.learntocook
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.net.Uri
import androidx.core.app.ActivityCompat
import com.google.android.material.chip.Chip
import android.util.Base64
import androidx.activity.result.contract.ActivityResultContracts
import com.example.learntocook.databinding.ActivityCreateBlogPostBinding
import org.json.JSONArray
import org.json.JSONObject

class CreateBlogPostActivity : AppCompatActivity() {
    private lateinit var binding : ActivityCreateBlogPostBinding

    private var selectedImageUri: Uri?= null

    private val imagePickerLauncher = registerForActivityResult(ActivityResultContracts.GetContent()){
        uri: Uri? ->
        uri?.let{
            selectedImageUri = it
            binding.imagePreview.setImageURI(it)
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        binding = ActivityCreateBlogPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonBack.setOnClickListener {
            finish()
        }

        binding.imagePreview.setOnClickListener{
            imagePickerLauncher.launch("image/*")
        }

        binding.buttonPost.setOnClickListener{
            val title = binding.editTitle.text.toString().trim()
            val description = binding.editDescription.text.toString().trim()
            
            // Parse ingredients as array - split by newlines and filter empty lines
            val ingredientsText = binding.editIngredients.text.toString().trim()
            val ingredients = ingredientsText.split('\n')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            
            // Parse instructions as array - split by newlines and filter empty lines
            val instructionsText = binding.editInstructions.text.toString().trim()
            val instructions = instructionsText.split('\n')
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            
            // Get selected cuisine
            val checkedChipId = binding.chipGroupCuisine.checkedChipId
            val cuisine = if (checkedChipId != -1) {
                val chip = binding.chipGroupCuisine.findViewById<Chip>(checkedChipId)
                chip.text.toString()
            } else {
                ""
            }
            
            // Get selected difficulty
            val checkedDifficultyId = binding.chipGroupDifficulty.checkedChipId
            val difficulty = if (checkedDifficultyId != -1) {
                val chip = binding.chipGroupDifficulty.findViewById<Chip>(checkedDifficultyId)
                chip.text.toString()
            } else {
                ""
            }
            
            // Validation
            if (title.isEmpty() || description.isEmpty() || ingredients.isEmpty() || instructions.isEmpty() || cuisine.isEmpty() || difficulty.isEmpty()) {
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            Log.d("CreateBlogPost", "Creating recipe: $title")
            Log.d("CreateBlogPost", "Ingredients: $ingredients")
            Log.d("CreateBlogPost", "Instructions: $instructions")
            Log.d("CreateBlogPost", "Cuisine: $cuisine")
            Log.d("CreateBlogPost", "Difficulty: $difficulty")

            // Create JSON payload
            val json = JSONObject().apply{
                put("title", title)
                put("description", description)
                put("cuisine", cuisine)
                put("difficulty", difficulty)
                put("ingredients", JSONArray(ingredients))
                put("instructions", JSONArray(instructions))
                // Add author_id from current user
                UserManager.getCurrentUserId(this@CreateBlogPostActivity)?.let { userId ->
                    put("author_id", userId)
                }
            }

            Log.d("CreateBlogPost", "JSON payload: $json")

            // Use ApiClient for authenticated request
            ApiClient.makeRequest(
                context = this,
                endpoint = "/recipes",
                method = "POST",
                body = json.toString(),
                onSuccess = { response ->
                    Log.d("CreateBlogPost", "Recipe created successfully: $response")
                    runOnUiThread {
                        Toast.makeText(this@CreateBlogPostActivity, "Recipe created successfully!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                },
                onError = { error ->
                    Log.e("CreateBlogPost", "Failed to create recipe: $error")
                    runOnUiThread {
                        binding.buttonPost.isEnabled = true
                        Toast.makeText(this@CreateBlogPostActivity, "Failed to create recipe: $error", Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
    }
}