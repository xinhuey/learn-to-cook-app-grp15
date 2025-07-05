package com.example.learntocook

import android.content.Context
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.learntocook.databinding.ActivityPreferencesBinding

class PreferencesActivity : AppCompatActivity() {
    private lateinit var binding: ActivityPreferencesBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPreferencesBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // back button listener
        binding.buttonBack.setOnClickListener {
            Log.d("PreferencesActivity", "Back button clicked")
            finish()
        }

        binding.buttonSave.setOnClickListener {
            savePreferences()
            finish()
        }

        loadPreferences()
    }

    private fun savePreferences() {
        val sharedPref = getSharedPreferences("user_preferences", Context.MODE_PRIVATE)
        val editor = sharedPref.edit()

        editor.putBoolean("allergy_dairy", binding.checkDairy.isChecked)
        editor.putBoolean("allergy_gluten", binding.checkGluten.isChecked)
        editor.putBoolean("allergy_nuts", binding.checkNuts.isChecked)
        editor.putBoolean("allergy_shellfish", binding.checkShellfish.isChecked)
        editor.putBoolean("allergy_soy", binding.checkSoy.isChecked)

        val selectedCuisines = mutableListOf<String>()
        for (i in 0 until binding.chipGroupCuisine.childCount) {
            val chip = binding.chipGroupCuisine.getChildAt(i)
            if (chip is com.google.android.material.chip.Chip && chip.isChecked) {
                selectedCuisines.add(chip.text.toString())
            }
        }
        editor.putString("preferred_cuisines", selectedCuisines.joinToString(","))
        editor.putString("preferred_ingredients", binding.editTextIngredients.text.toString())

        editor.apply()

        Log.d("PreferencesActivity", "Preferences saved: $selectedCuisines")
    }

    private fun loadPreferences() {
        val sharedPref = getSharedPreferences("user_preferences", Context.MODE_PRIVATE)

        binding.checkDairy.isChecked = sharedPref.getBoolean("allergy_dairy", false)
        binding.checkGluten.isChecked = sharedPref.getBoolean("allergy_gluten", false)
        binding.checkNuts.isChecked = sharedPref.getBoolean("allergy_nuts", false)
        binding.checkShellfish.isChecked = sharedPref.getBoolean("allergy_shellfish", false)
        binding.checkSoy.isChecked = sharedPref.getBoolean("allergy_soy", false)

        val preferredCuisines = sharedPref.getString("preferred_cuisines", "")?.split(",") ?: emptyList()
        for (i in 0 until binding.chipGroupCuisine.childCount) {
            val chip = binding.chipGroupCuisine.getChildAt(i)
            if (chip is com.google.android.material.chip.Chip) {
                chip.isChecked = preferredCuisines.contains(chip.text.toString())
            }
        }

        binding.editTextIngredients.setText(sharedPref.getString("preferred_ingredients", ""))
    }
}
