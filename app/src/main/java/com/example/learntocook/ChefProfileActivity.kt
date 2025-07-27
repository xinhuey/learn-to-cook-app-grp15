package com.example.learntocook

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.learntocook.databinding.ActivityChefProfileBinding

class ChefProfileActivity : AppCompatActivity() {
    private lateinit var binding: ActivityChefProfileBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChefProfileBinding.inflate(layoutInflater)
        setContentView(binding.root)



    }
}