package com.example.learntocook

import android.content.Context
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.learntocook.databinding.ActivityPreferencesBinding
import com.google.android.material.chip.Chip
import androidx.core.content.edit

class PreferencesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPreferencesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreferencesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        loadPreferences()

        // back button listener
        binding.buttonBack.setOnClickListener {
            Log.d("PreferencesActivity", "Back button clicked")
            finish()
        }

        // Save button listener
        binding.buttonSave.setOnClickListener {
            savePreferences()
        }
    }

    // TODO: Add api call to savePreferences since only saving local copy of preferences
    private fun savePreferences() {
        val sharedPref = getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
        sharedPref.edit {

            // Get alelrgy data
            val selectedAllergies = mutableSetOf<String>()
            if (binding.checkDairy.isChecked) selectedAllergies.add("dairy")
            if (binding.checkGluten.isChecked) selectedAllergies.add("gluten")
            if (binding.checkNuts.isChecked) selectedAllergies.add("nuts")
            if (binding.checkShellfish.isChecked) selectedAllergies.add("shellfish")
            if (binding.checkSoy.isChecked) selectedAllergies.add("soy")

            // Cuisine data
            val selectedCuisines = binding.chipGroupCuisine.checkedChipIds.map { chipId ->
                findViewById<Chip>(chipId).text.toString().lowercase()
            }.toSet()

            // Ingredients text
            val ingredientsText = binding.editTextIngredients.text.toString()

            putStringSet("allergies", selectedAllergies)
            putStringSet("cuisines", selectedCuisines)
            putString("ingredients", ingredientsText)
        }

        Toast.makeText(this, "Saved preferences", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun loadPreferences() {
        val sharedPref = getSharedPreferences("user_preferences", Context.MODE_PRIVATE)

        // Load and set allergies
        val savedAllergies = sharedPref.getStringSet("allergies", emptySet()) ?: emptySet()
        binding.checkDairy.isChecked = savedAllergies.contains("dairy")
        binding.checkGluten.isChecked = savedAllergies.contains("gluten")
        binding.checkNuts.isChecked = savedAllergies.contains("nuts")
        binding.checkShellfish.isChecked = savedAllergies.contains("shellfish")
        binding.checkSoy.isChecked = savedAllergies.contains("soy")

        // Load and set cuisines
        val savedCuisines = sharedPref.getStringSet("cuisines", emptySet()) ?: emptySet()
        for (i in 0 until binding.chipGroupCuisine.childCount) {
            val chip = binding.chipGroupCuisine.getChildAt(i) as Chip
            if (savedCuisines.contains(chip.text.toString().lowercase())) {
                chip.isChecked = true
            }
        }

        // Load and set ingredients
        val savedIngredients = sharedPref.getString("ingredients", "")
        binding.editTextIngredients.setText(savedIngredients)

    }
}