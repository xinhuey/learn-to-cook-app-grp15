package com.example.learntocook

import com.google.gson.annotations.SerializedName

data class Author(
    val id: String,
    @SerializedName("full_name")
    val full_name: String,
    @SerializedName("profile_image_url")
    val profile_image_url: String?
)