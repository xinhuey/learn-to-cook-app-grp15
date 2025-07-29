package com.example.learntocook

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.learntocook.databinding.ItemChefBinding
import com.squareup.picasso.Picasso

class ChefAdapter(
    private var chefs: List<Author>,
    private val onChefClick: (Author) -> Unit
) : RecyclerView.Adapter<ChefAdapter.ChefViewHolder>() {

    inner class ChefViewHolder(private val binding: ItemChefBinding) :
        RecyclerView.ViewHolder(binding.root) {
        
        fun bind(chef: Author) {
            binding.textChefName.text = chef.full_name
            val specialtyText = chef.specialty ?: chef.chefExpertise ?: chef.skillLevel ?: chef.bio ?: "Chef"
            binding.textChefSpecialty.text = specialtyText
            binding.textFollowerCount.text = "${chef.followerCount} followers"
            
            if (!chef.profile_image_url.isNullOrEmpty()) {
                Picasso.get().load(chef.profile_image_url).into(binding.imageChefProfile)
            } else {
                binding.imageChefProfile.setImageResource(R.drawable.ic_launcher_foreground)
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChefViewHolder {
        val binding = ItemChefBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChefViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChefViewHolder, position: Int) {
        val chef = chefs[position]
        holder.bind(chef)
        holder.itemView.setOnClickListener { onChefClick(chef) }
    }

    override fun getItemCount() = chefs.size

    fun updateChefs(newChefs: List<Author>) {
        chefs = newChefs
        notifyDataSetChanged()
    }
} 