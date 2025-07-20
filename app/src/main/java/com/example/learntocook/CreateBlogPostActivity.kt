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
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import org.json.JSONArray
import org.json.JSONObject

class CreateBlogPostActivity : AppCompatActivity() {
    private lateinit var binding : ActivityCreateBlogPostBinding

    private var selectedImageUri: Uri?= null
    private val client = OkHttpClient()
    companion object{
        private const val BASE_API_URL = "https://learn-to-cook-api.uwgroup15projectapp.workers.dev/api"
    }

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
            val ingredients = binding.editIngredients.text.toString().trim()
            val instructions = binding.editInstructions.text.toString().trim()
            val checkedChipId = binding.chipGroupCuisine.checkedChipId
            val cuisine = if (checkedChipId != -1) {
                val chip = binding.chipGroupCuisine.findViewById<com.google.android.material.chip.Chip>(checkedChipId)
                chip.text.toString()
            } else {
                ""
            }
            if (title.isEmpty() || description.isEmpty() || ingredients.isEmpty()
                || cuisine.isEmpty() || instructions.isEmpty() ||selectedImageUri == null){

                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()

                return@setOnClickListener
            }

            Log.d("CreateBlogPost", "Creating post: $title")

            val imageBytes = contentResolver.openInputStream(selectedImageUri!!)?.use { it.readBytes() }
            val imageBase64 = imageBytes?.let { Base64.encodeToString(it, Base64.NO_WRAP) }
            val json = JSONObject().apply{
                put("title", title)
                put("description", description)
                put("cuisine", cuisine)
                put("ingredients", JSONArray(ingredients.split('\n').map { it.trim() }.filter { it.isNotEmpty() }))
                put("instructions", JSONArray(instructions.split('\n').map { it.trim() }.filter { it.isNotEmpty() }))
                imageBase64?.let {
                    put("image_urls", JSONArray().put(it))
                }
            }

            val requestBody = json.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_API_URL/recipes")
                .post(requestBody)
                .build()

            binding.buttonPost.isEnabled = false

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        binding.buttonPost.isEnabled = true
                        Toast.makeText(this@CreateBlogPostActivity, "Failed to create post", Toast.LENGTH_SHORT).show()
                        Log.e("CreateBlogPost", "Post failed", e)
                    }
                }

                override fun onResponse(call: Call, response: Response) {
                    runOnUiThread {
                        binding.buttonPost.isEnabled = true
                        if (response.isSuccessful) {
                            Toast.makeText(this@CreateBlogPostActivity, "Post created!", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this@CreateBlogPostActivity, "Error creating post", Toast.LENGTH_SHORT).show()
                            Log.e("CreateBlogPost", "Error: ${'$'}{response.code}")
                        }
                    }
                }
            })

        }
    }
}