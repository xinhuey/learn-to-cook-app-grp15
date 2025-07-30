package com.example.learntocook

import org.junit.Assert.*
import org.junit.Test

class ReviewTest {

    private val testAuthor = Author(
        id = "user123",
        full_name = "Abhi Tank",
        profile_image_url = "https://example.com/profile.jpg",
        followerCount = 200,
        specialty = "Desserts",
        isChef = true
    )

    @Test
    fun `creating review should correctly assign values`() {
        val review = Review(
            id = "rev456",
            recipeId = "recipe789",
            userId = "user123",
            rating = 5,
            comment = "Absolutely loved it!",
            createdAt = "2025-07-28T00:00:00Z",
            updatedAt = "2025-07-28T01:00:00Z",
            user = testAuthor
        )

        assertEquals("rev456", review.id)
        assertEquals("recipe789", review.recipeId)
        assertEquals("user123", review.userId)
        assertEquals(5, review.rating)
        assertEquals("Absolutely loved it!", review.comment)
        assertEquals("2025-07-28T00:00:00Z", review.createdAt)
        assertEquals("2025-07-28T01:00:00Z", review.updatedAt)
        assertEquals(testAuthor, review.user)
    }

    @Test
    fun `equality check should compare all fields`() {
        val r1 = Review(
            id = "r1",
            recipeId = "rec1",
            userId = "u1",
            rating = 4,
            comment = "Great taste!",
            createdAt = "2025-07-27T14:00:00Z",
            updatedAt = "2025-07-27T15:00:00Z",
            user = testAuthor
        )

        val r2 = Review(
            id = "r1",
            recipeId = "rec1",
            userId = "u1",
            rating = 4,
            comment = "Great taste!",
            createdAt = "2025-07-27T14:00:00Z",
            updatedAt = "2025-07-27T15:00:00Z",
            user = testAuthor
        )

        assertEquals(r1, r2)
    }
}
