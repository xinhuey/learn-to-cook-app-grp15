package com.example.learntocook

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.text.SimpleDateFormat
import java.util.*

class ReviewAdapter(private var reviews: List<Review>) : 
    RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder>() {

    class ReviewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textReviewerName: TextView = itemView.findViewById(R.id.textReviewerName)
        val textReviewRating: TextView = itemView.findViewById(R.id.textReviewRating)
        val textReviewComment: TextView = itemView.findViewById(R.id.textReviewComment)
        val textReviewDate: TextView = itemView.findViewById(R.id.textReviewDate)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReviewViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_review, parent, false)
        return ReviewViewHolder(view)
    }

    override fun onBindViewHolder(holder: ReviewViewHolder, position: Int) {
        val review = reviews[position]
        
        holder.textReviewerName.text = review.user.full_name
        holder.textReviewRating.text = "★".repeat(review.rating) + "☆".repeat(5 - review.rating)
        holder.textReviewComment.text = review.comment
        
        // Format the date
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            val date = inputFormat.parse(review.createdAt.substring(0, 19))
            holder.textReviewDate.text = date?.let { outputFormat.format(it) } ?: "Unknown date"
        } catch (e: Exception) {
            holder.textReviewDate.text = "Unknown date"
        }
    }

    override fun getItemCount(): Int = reviews.size

    fun updateReviews(newReviews: List<Review>) {
        reviews = newReviews
        notifyDataSetChanged()
    }
    
    fun addReview(newReview: Review) {
        reviews = listOf(newReview) + reviews
        notifyItemInserted(0)
    }
} 