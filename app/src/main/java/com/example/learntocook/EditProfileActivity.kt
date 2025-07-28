package com.example.learntocook

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.learntocook.databinding.ActivityEditProfileBinding
import org.json.JSONObject

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        populateInitialData()
        setupClickListeners()
    }

    private fun populateInitialData() {
        //get the data from ChefProfileActivity
        val currentName = intent.getStringExtra("USER_NAME")
        binding.editTextFullName.setText(currentName)
    }

    private fun setupClickListeners() {
        binding.buttonBack.setOnClickListener {
            finish()
        }

        binding.buttonSave.setOnClickListener {
            val newName = binding.editTextFullName.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(this, "Full name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveProfileChanges(newName)
        }
    }

    private fun saveProfileChanges(name: String) {
        setLoadingState(true)

        val userId = UserManager.getCurrentUserId(this)
        if (userId == null) {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show()
            setLoadingState(false)
            return
        }

        val updateData = JSONObject().apply {
            put("full_name", name)
        }

        val endpoint = "/users/$userId"
        Log.d("EditProfileActivity", "Updating profile for user $userId at endpoint $endpoint")

        ApiClient.makeRequest(
            context = this,
            endpoint = endpoint,
            method = "PUT",
            body = updateData.toString(),
            onSuccess = { response ->
                Log.d("EditProfileActivity", "Profile updated successfully: $response")
                runOnUiThread {
                    setLoadingState(false)
                    Toast.makeText(this, "Profile updated!", Toast.LENGTH_SHORT).show()
                    setResult(Activity.RESULT_OK)
                    finish()
                }
            },
            onError = { errorMessage ->
                Log.e("EditProfileActivity", "Failed to update profile: $errorMessage")
                runOnUiThread {
                    setLoadingState(false)
                    Toast.makeText(this, "Failed to update: $errorMessage", Toast.LENGTH_LONG).show()
                }
            }
        )
    }

    private fun setLoadingState(isLoading: Boolean) {
        if (isLoading) {
            binding.buttonSave.text = ""
            binding.progressBarSave.visibility = View.VISIBLE
            binding.buttonSave.isEnabled = false
        } else {
            binding.buttonSave.text = "Save Changes"
            binding.progressBarSave.visibility = View.GONE
            binding.buttonSave.isEnabled = true
        }
    }
}
