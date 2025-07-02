package com.example.learntocook

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.example.learntocook.databinding.ActivityBlogBinding


class BlogActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBlogBinding

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        binding = ActivityBlogBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val title = intent.getStringExtra("recipe_title") ?: "Simple Spaghetti"
        binding.textTitle.text = title
        binding.imageRecipe.setImageResource(R.drawable.ic_launcher_foreground)
        binding.textIngredients.text = "- 200g spaghetti\n- 1 cup tomato sauce\n- 2 cloves garlic"
        binding.textSteps.text = "1. Boil pasta until tender.\n2. Heat sauce with garlic.\n3. Combine and serve."
        binding.textReviews.text = "\"Great recipe!\" - Alice\n\"My kids loved it.\" - Bob"


        binding.buttonBack.setOnClickListener {
            Log.d("BlogActivity", "Back button clicked")
            finish()
        }
    }
}