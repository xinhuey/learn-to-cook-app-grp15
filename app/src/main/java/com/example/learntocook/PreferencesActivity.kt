package com.example.learntocook

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
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
        
        editor.putBoolean("following_only", binding.checkFollowingOnly.isChecked)
        
        val ingredients = binding.editTextIngredients.text.toString().trim()
        editor.putString("preferred_ingredients", ingredients)

        editor.apply()

        Toast.makeText(this, "Preferences saved successfully!", Toast.LENGTH_SHORT).show()

        // return result to indicate preferences were updated
        // that way we can refetch with updated preferences on landing page
        setResult(Activity.RESULT_OK)
        finish()
    }

    private fun loadPreferences() {
        // load previously saved prefs
        val sharedPref = getSharedPreferences("user_preferences", Context.MODE_PRIVATE)

        binding.checkDairy.isChecked = sharedPref.getBoolean("allergy_dairy", false)
        binding.checkGluten.isChecked = sharedPref.getBoolean("allergy_gluten", false)
        binding.checkNuts.isChecked = sharedPref.getBoolean("allergy_nuts", false)
        binding.checkShellfish.isChecked = sharedPref.getBoolean("allergy_shellfish", false)
        binding.checkSoy.isChecked = sharedPref.getBoolean("allergy_soy", false)

        val preferredCuisines = sharedPref.getString("preferred_cuisines", "")
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() } 
            ?: emptyList()
            
        for (i in 0 until binding.chipGroupCuisine.childCount) {
            val chip = binding.chipGroupCuisine.getChildAt(i)
            if (chip is com.google.android.material.chip.Chip) {
                chip.isChecked = preferredCuisines.contains(chip.text.toString())
            }
        }

        binding.checkFollowingOnly.isChecked = sharedPref.getBoolean("following_only", false)

        binding.editTextIngredients.setText(sharedPref.getString("preferred_ingredients", ""))
        
        Log.d("PreferencesActivity", "Preferences loaded - Cuisines: $preferredCuisines, Following Only: ${binding.checkFollowingOnly.isChecked}")
    }
}
