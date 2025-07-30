package com.example.learntocook

import org.junit.Assert.*
import org.junit.Test

class RecipeTest {

    private val testAuthor = Author(
        id = "author01",
        full_name = "Chef Ayesha",
        profile_image_url = "https://example.com/ayesha.jpg",
        followerCount = 350,
        specialty = "Mediterranean",
        isChef = true
    )

    private val testReview = Review(
        id = "review01",
        recipeId = "recipe01",
        userId = "user01",
        rating = 4,
        comment = "Really tasty and easy to follow.",
        createdAt = "2025-07-25T10:00:00Z",
        updatedAt = "2025-07-25T11:00:00Z",
        user = testAuthor
    )

    @Test
    fun `creating recipe with full data should assign values correctly`() {
        val recipe = Recipe(
            id = "recipe01",
            title = "Chickpea Curry",
            description = "A warm and flavorful Indian curry.",
            ingredients = listOf("Chickpeas", "Onions", "Garlic", "Spices"),
            instructions = listOf("Soak chickpeas", "Saute onions", "Add spices", "Simmer"),
            cuisine = "Indian",
            difficulty = "Easy",
            prepTime = 20,
            cookTime = 40,
            servings = 4,
            imageUrls = listOf("https://example.com/img1.jpg"),
            tags = listOf("vegan", "gluten-free"),
            author = testAuthor,
            authorId = "author01",
            isPublic = true,
            createdAt = "2025-07-27T12:00:00Z",
            updatedAt = "2025-07-27T13:00:00Z",
            averageRating = 4.5,
            reviewCount = 1,
            recipeReviews = listOf(testReview)
        )

        assertEquals("recipe01", recipe.id)
        assertEquals("Chickpea Curry", recipe.title)
        assertEquals("Indian", recipe.cuisine)
        assertEquals(4, recipe.servings)
        assertEquals("https://example.com/img1.jpg", recipe.imageUrls?.first())
        assertEquals(testAuthor, recipe.author)
        assertEquals(1, recipe.recipeReviews?.size)
    }

    @Test
    fun `optional and default values should be handled correctly`() {
        val recipe = Recipe(
            id = "r2",
            title = "Plain Rice",
            description = null,
            ingredients = listOf("Rice", "Water"),
            instructions = listOf("Boil water", "Add rice", "Simmer"),
            cuisine = "Asian",
            difficulty = "Easy",
            prepTime = null,
            cookTime = null,
            servings = null,
            imageUrls = null,
            tags = null,
            author = null,
            authorId = null,
            createdAt = "2025-07-28T00:00:00Z",
            updatedAt = "2025-07-28T01:00:00Z"
            // isPublic, averageRating, reviewCount, recipeReviews omitted
        )

        assertTrue(recipe.isPublic) // default
        assertNull(recipe.description)
        assertNull(recipe.averageRating)
        assertNull(recipe.reviewCount)
        assertNull(recipe.recipeReviews)
    }

    @Test
    fun `equality check should pass for identical recipes`() {
        val r1 = Recipe(
            id = "rX",
            title = "Test Dish",
            description = null,
            ingredients = listOf("Test"),
            instructions = listOf("Step 1"),
            cuisine = "Fusion",
            difficulty = "Medium",
            prepTime = 5,
            cookTime = 10,
            servings = 1,
            imageUrls = null,
            tags = null,
            author = testAuthor,
            authorId = "author01",
            isPublic = true,
            createdAt = "2025-07-28T00:00:00Z",
            updatedAt = "2025-07-28T00:10:00Z"
        )

        val r2 = r1.copy()

        assertEquals(r1, r2)
    }
}
