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
    @SerializedName("specialty")
    val specialty: String?,
    @SerializedName("is_chef")
    val isChef: Boolean = false // default false for reg users
)