package com.example.learntocook
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.learntocook.databinding.ActivityCreateBlogPostBinding

class CreateBlogPostActivity : AppCompatActivity() {
    private lateinit var binding : ActivityCreateBlogPostBinding

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        binding = ActivityCreateBlogPostBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonBack.setOnClickListener {
            finish()
        }

        binding.buttonPost.setOnClickListener{
            val title = binding.editTitle.text.toString().trim()
            val content = binding.editContent.text.toString().trim()

            if (title.isEmpty() || content.isEmpty()){
                Toast.makeText(this, "Please fill out all fields", Toast.LENGTH_SHORT).show()

                return@setOnClickListener
            }

            Log.d("CreateBlogPost", "Creating post: $title")

            Toast.makeText(this, "Post created", Toast.LENGTH_SHORT).show()
            finish()

        }
    }
}