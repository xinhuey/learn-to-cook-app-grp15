package com.example.learntocook

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import android.util.Log
import android.content.Intent
import com.example.learntocook.databinding.ActivityLoginBinding
import com.google.gson.Gson
import org.json.JSONObject

class LoginActivity: AppCompatActivity() {
    private lateinit var binding: ActivityLoginBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Check if user is already logged in
        if (UserManager.isLoggedIn(this)) {
            startActivity(Intent(this, LandingActivity::class.java))
            finish()
            return
        }

        binding.buttonLogin.setOnClickListener{
            val email = binding.editTextUsername.text.toString().trim()
            val password = binding.editTextPassword.text.toString()

            if (email.isEmpty() || password.isEmpty()){
                Toast.makeText(this, "Please enter email and password", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Attempt login
            performLogin(email, password)
        }
        
        binding.textViewSignUp.setOnClickListener{
            Log.d("LoginActivity", "Navigate to Signup Screen")
            val intent = Intent(this, SignUpActivity::class.java)
            startActivity(intent)
        }
    }

    private fun performLogin(email: String, password: String) {
        val loginData = JSONObject().apply {
            put("email", email)
            put("password", password)
        }

        Log.d("LoginActivity", "Attempting login for email: $email")

        ApiClient.makePublicRequest(
            endpoint = "/auth/login",
            method = "POST",
            body = loginData.toString(),
            onSuccess = { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    Log.d("LoginActivity", "Login response: $response")
                    
                    if (jsonResponse.has("id") || jsonResponse.has("email")) {
                        // Login successful - save user session
                        val userName = jsonResponse.optString("full_name", "User")
                        val userEmail = jsonResponse.optString("email", email)
                        UserManager.saveUserSession(this, userEmail, userName)
                        
                        Log.d("LoginActivity", "Login successful for user: $userEmail")
                        runOnUiThread {
                            startActivity(Intent(this, LandingActivity::class.java))
                            finish()
                        }
                    } else {
                        Log.e("LoginActivity", "Login failed - no user data in response")
                        runOnUiThread {
                            Toast.makeText(this, "Login failed - invalid response", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("LoginActivity", "Error parsing login response: $response", e)
                    runOnUiThread {
                        Toast.makeText(this, "Login failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onError = { error ->
                Log.e("LoginActivity", "Login error: $error")
                runOnUiThread {
                    Toast.makeText(this, "Login failed: $error", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}