package com.example.learntocook

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.learntocook.databinding.ActivityLandingBinding


class LandingActivity : AppCompatActivity(){
    private lateinit var binding: ActivityLandingBinding

    override fun onCreate(savedInstanceState: Bundle?){
        super.onCreate(savedInstanceState)
        binding = ActivityLandingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val recipes = listOf(
            Recipe("Spaghetti Bolognese"),
            Recipe("Chicken Curry"),
            Recipe("Vegetable Stir Fry"),
            Recipe("Beef Tacos"),
            Recipe("Tomato Soup")
        )

        binding.recyclerViewRecipes.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewRecipes.adapter = RecipeAdapter(recipes) { recipe ->
            val intent = Intent(this, BlogActivity::class.java)
            intent.putExtra("recipe_title", recipe.title)
            startActivity(intent)
        }
        binding.preferences.setOnClickListener {
            val intent = Intent(this, PreferencesActivity::class.java)
            startActivity(intent)
        }

    }
}