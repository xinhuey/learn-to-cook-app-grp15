package com.example.learntocook

import java.util.UUID

data class Author(
    val id: UUID,
    val full_name: String,
    val profile_image_url: String?
)