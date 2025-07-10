package com.example.learntocook

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.learntocook.databinding.ItemRecipeBinding
import com.squareup.picasso.Picasso

class RecipeAdapter(
    private var recipes: List<Recipe>,
    private val onRecipeClick : (Recipe) -> Unit
) :
    RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder>() {

    inner class RecipeViewHolder(private val binding: ItemRecipeBinding) :
        RecyclerView.ViewHolder(binding.root) {
        fun bind(recipe: Recipe) {
            binding.textViewRecipeTitle.text = recipe.title
            binding.textViewRecipeDescription.text = recipe.description ?: ""
            binding.textViewRecipeCuisine.text = recipe.cuisine
            binding.textViewRecipeDifficulty.text = recipe.difficulty
            binding.textViewRecipeAuthor.text = recipe.author?.full_name?.let { "By $it" } ?: ""
            val url = recipe.imageUrls?.firstOrNull()
            if (url != null){
                Picasso.get().load(url).into(binding.imageViewRecipe)
            }
            else{
                binding.imageViewRecipe.setImageDrawable(null)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecipeViewHolder {
        val binding = ItemRecipeBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val params = binding.root.layoutParams
        params.height = (Resources.getSystem().displayMetrics.heightPixels * 0.5).toInt()
        binding.root.layoutParams = params
        return RecipeViewHolder(binding)
    }

    override fun onBindViewHolder(holder: RecipeViewHolder, position: Int) {
        val recipe = recipes[position]
        holder.bind(recipe)
        holder.itemView.setOnClickListener{ onRecipeClick(recipe) }
    }

    override fun getItemCount() = recipes.size

    fun updateRecipes(newRecipes: List<Recipe>) {
        recipes = newRecipes
        notifyDataSetChanged()
    }
}