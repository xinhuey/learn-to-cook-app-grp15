package com.example.learntocook

import android.os.Bundle
import android.widget.Toast
import android.util.Log
import android.content.Intent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.learntocook.databinding.ActivitySignUpBinding
import org.json.JSONObject

class SignUpActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySignUpBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignUpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.buttonSignUp.setOnClickListener {
            val name = binding.editTextName.text.toString().trim()
            val email = binding.editTextUsername.text.toString().trim()
            val password = binding.editTextPassword.text.toString().trim()
            val isChef = binding.checkBoxChef.isChecked

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (password.length < 6) { //basic password strength check
                Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            // Perform sign-up
            performSignUp(name, email, password, isChef)
        }

        binding.textViewLoginLink.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun performSignUp(name: String, email: String, password: String, isChef: Boolean) {
        val signUpData = JSONObject().apply {
            put("full_name", name)
            put("email", email)
            put("password", password)
            put("is_chef", isChef)
        }

        ApiClient.makePublicRequest(
            endpoint = "/auth/register",
            method = "POST",
            body = signUpData.toString(),
            onSuccess = { response ->
                try {
                    val jsonResponse = JSONObject(response)
                    Log.d("SignUpActivity", "Sign up response: $response")
                    
                    // Check if response contains user data (successful registration)
                    if (jsonResponse.has("id") || jsonResponse.has("email")) {
                        val userName = jsonResponse.optString("full_name", name)
                        val userEmail = jsonResponse.optString("email", email)
                        val isChef = jsonResponse.optBoolean("is_chef", false)
                        UserManager.saveUserSession(this, userEmail, userName, isChef)
                        
                        Log.d("SignUpActivity", "Sign up successful for user: $userEmail, isChef: $isChef")
                        runOnUiThread {
                            Toast.makeText(this, "Account created successfully!", Toast.LENGTH_SHORT).show()
                            startActivity(Intent(this, LandingActivity::class.java))
                            finish()
                        }
                    } else {
                        Log.e("SignUpActivity", "Sign up failed - no user data in response")
                        runOnUiThread {
                            Toast.makeText(this, "Sign up failed - invalid response", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Log.e("SignUpActivity", "Error parsing sign up response: $response", e)
                    runOnUiThread {
                        Toast.makeText(this, "Sign up failed: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            },
            onError = { error ->
                Log.e("SignUpActivity", "Sign up error: $error")
                runOnUiThread {
                    Toast.makeText(this, "Sign up failed: $error", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }
}