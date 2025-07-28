package com.example.learntocook

import android.content.Context
import android.content.SharedPreferences
import java.security.MessageDigest
import java.util.*

object UserManager {
    private const val PREFS_NAME = "user_session"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_USER_EMAIL = "user_email"
    private const val KEY_USER_NAME = "user_name"
    private const val KEY_USER_IS_CHEF = "user_is_chef"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // gen a unique UID based on email
    fun generateUserId(email: String): String {
        val hash = MessageDigest.getInstance("SHA-256").digest(email.toByteArray())
        val hashString = hash.joinToString("") { "%02x".format(it) }.take(32)
        
        // format UUID with dashes: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx like supabase
        return "${hashString.substring(0, 8)}-${hashString.substring(8, 12)}-${hashString.substring(12, 16)}-${hashString.substring(16, 20)}-${hashString.substring(20, 32)}"
    }

    private fun normalizeUserId(userId: String): String {
        return if (userId.length == 32 && !userId.contains("-")) {
            "${userId.substring(0, 8)}-${userId.substring(8, 12)}-${userId.substring(12, 16)}-${userId.substring(16, 20)}-${userId.substring(20, 32)}"
        } else {
            userId
        }
    }

    fun saveUserSession(context: Context, email: String, name: String, isChef: Boolean = false) {
        val userId = generateUserId(email)
        val prefs = getSharedPreferences(context)
        prefs.edit().apply {
            putString(KEY_USER_ID, userId)
            putString(KEY_USER_EMAIL, email)
            putString(KEY_USER_NAME, name)
            putBoolean(KEY_USER_IS_CHEF, isChef)
            apply()
        }
    }

    fun getCurrentUserId(context: Context): String? {
        val userId = getSharedPreferences(context).getString(KEY_USER_ID, null)
        return userId?.let { normalizeUserId(it) }
    }

    fun getCurrentUserEmail(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_USER_EMAIL, null)
    }

    fun getCurrentUserName(context: Context): String? {
        return getSharedPreferences(context).getString(KEY_USER_NAME, null)
    }

    fun getCurrentUserIsChef(context: Context): Boolean {
        return getSharedPreferences(context).getBoolean(KEY_USER_IS_CHEF, false)
    }

    fun isLoggedIn(context: Context): Boolean {
        return getCurrentUserId(context) != null
    }

    fun clearUserSession(context: Context) {
        val prefs = getSharedPreferences(context)
        prefs.edit().clear().apply()
    }
} 