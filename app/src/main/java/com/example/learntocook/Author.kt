package com.example.learntocook

import com.google.gson.annotations.SerializedName

data class Author(
    val id: String,
    @SerializedName("full_name")
    val full_name: String,
    @SerializedName("profile_image_url")
    val profile_image_url: String?,
    @SerializedName("follower_count")
    val followerCount: Int = 0,
    @SerializedName("bio")
    val bio: String?,
    @SerializedName("skill_level")
    val skillLevel: String?,
    @SerializedName("specialty")
    val specialty: String?,
    @SerializedName("chef_expertise")
    val chefExpertise: String?,
    @SerializedName("is_chef")
    val isChef: Boolean = false
)