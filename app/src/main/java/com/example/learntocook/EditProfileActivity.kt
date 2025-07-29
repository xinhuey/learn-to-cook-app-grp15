package com.example.learntocook

import android.app.Activity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.learntocook.databinding.ActivityEditProfileBinding
import org.json.JSONObject

class EditProfileActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditProfileBinding
    private val skillLevels = arrayOf("Easy", "Medium", "Hard")
    private val chefExpertiseLevels = arrayOf("Beginner", "Intermediate", "Expert")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEditProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupSkillLevelDropdown()
        setupChefExpertiseDropdown()
        populateInitialData()
        setupClickListeners()
    }

    private fun setupSkillLevelDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, skillLevels)
        binding.autoCompleteSkillLevel.setAdapter(adapter)
    }

    private fun setupChefExpertiseDropdown() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, chefExpertiseLevels)
        binding.autoCompleteChefExpertise.setAdapter(adapter)
    }

    private fun populateInitialData() {
        //get the data from ChefProfileActivity
        val currentName = intent.getStringExtra("USER_NAME")
        val currentBio = intent.getStringExtra("USER_BIO")
        val currentSkillLevel = intent.getStringExtra("USER_SKILL_LEVEL")
        val currentSpecialty = intent.getStringExtra("USER_SPECIALTY")
        val currentChefExpertise = intent.getStringExtra("USER_CHEF_EXPERTISE")
        
        binding.editTextFullName.setText(currentName)
        binding.editTextBio.setText(currentBio)
        binding.autoCompleteSkillLevel.setText(currentSkillLevel, false)
        binding.editTextSpecialty.setText(currentSpecialty)
        binding.autoCompleteChefExpertise.setText(currentChefExpertise, false)
    }

    private fun setupClickListeners() {
        binding.buttonBack.setOnClickListener {
            finish()
        }

        binding.buttonSave.setOnClickListener {
            val newName = binding.editTextFullName.text.toString().trim()
            val newBio = binding.editTextBio.text.toString().trim()
            val newSkillLevel = binding.autoCompleteSkillLevel.text.toString().trim()
            val newSpecialty = binding.editTextSpecialty.text.toString().trim()
            val newChefExpertise = binding.autoCompleteChefExpertise.text.toString().trim()

            if (newName.isEmpty()) {
                Toast.makeText(this, "Full name cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newSkillLevel.isNotEmpty() && !skillLevels.contains(newSkillLevel)) {
                Toast.makeText(this, "Please select a valid skill level", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newChefExpertise.isNotEmpty() && !chefExpertiseLevels.contains(newChefExpertise)) {
                Toast.makeText(this, "Please select a valid chef expertise level", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            saveProfileChanges(newName, newBio, newSkillLevel, newSpecialty, newChefExpertise)
        }
    }

    private fun saveProfileChanges(name: String, bio: String, skillLevel: String, specialty: String, chefExpertise: String) {
        setLoadingState(true)

        val userId = UserManager.getCurrentUserId(this)
        if (userId == null) {
            Toast.makeText(this, "Error: User not logged in", Toast.LENGTH_SHORT).show()
            setLoadingState(false)
            return
        }

        val updateData = JSONObject().apply {
            put("full_name", name)
            put("bio", bio)
            put("skill_level", skillLevel)
            put("specialty", specialty)
            put("chef_expertise", chefExpertise)
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
