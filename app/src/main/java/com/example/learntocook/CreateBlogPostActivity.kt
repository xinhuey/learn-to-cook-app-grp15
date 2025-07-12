package com.example.learntocook
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.net.Uri
import androidx.core.app.ActivityCompat
import androidx.activity.result.contract.ActivityResultContracts
import com.example.learntocook.databinding.ActivityCreateBlogPostBinding

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
            val ingredients = binding.editIngredients.text.toString().trim()
            val instructions = binding.editInstructions.text.toString().trim()

            if (title.isEmpty() || description.isEmpty() || ingredients.isEmpty()
                || instructions.isEmpty() ||selectedImageUri == null){

                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()

                return@setOnClickListener
            }

            Log.d("CreateBlogPost", "Creating post: $title")

            Toast.makeText(this, "Post created", Toast.LENGTH_SHORT).show()
            finish()

        }
    }
}