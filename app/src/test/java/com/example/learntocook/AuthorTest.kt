package com.example.learntocook

import org.junit.Assert.*
import org.junit.Test

class AuthorTest {

    @Test
    fun `creating author should correctly assign values`() {
        val author = Author(
            id = "123",
            full_name = "Ginni Tank",
            profile_image_url = "https://example.com/image.jpg",
            followerCount = 150,
            specialty = "Indian Cuisine",
            isChef = true
        )

        assertEquals("123", author.id)
        assertEquals("Ginni Tank", author.full_name)
        assertEquals("https://example.com/image.jpg", author.profile_image_url)
        assertEquals(150, author.followerCount)
        assertEquals("Indian Cuisine", author.specialty)
        assertTrue(author.isChef)
    }

    @Test
    fun `default values should be correctly set`() {
        val author = Author(
            id = "999",
            full_name = "John Doe",
            profile_image_url = null,
            specialty = null
        )

        assertEquals(0, author.followerCount)
        assertFalse(author.isChef)
    }

    @Test
    fun `equality check should compare all fields`() {
        val a1 = Author(
            id = "1",
            full_name = "Same Person",
            profile_image_url = null,
            followerCount = 10,
            specialty = "Fusion",
            isChef = true
        )
        val a2 = Author(
            id = "1",
            full_name = "Same Person",
            profile_image_url = null,
            followerCount = 10,
            specialty = "Fusion",
            isChef = true
        )

        assertEquals(a1, a2)
    }
}
