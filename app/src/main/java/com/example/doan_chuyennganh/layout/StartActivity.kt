package com.example.doan_chuyennganh.layout

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.doan_chuyennganh.LoginActivity
import com.example.doan_chuyennganh.R
import com.example.doan_chuyennganh.databinding.ActivityMainBinding
import com.example.doan_chuyennganh.databinding.ActivityStartBinding
import com.google.firebase.database.DatabaseReference

class StartActivity : AppCompatActivity() {
    private  lateinit var databaseReferences: DatabaseReference
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var binding: ActivityStartBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStartBinding.inflate(layoutInflater)
        setContentView(binding.root)

        sharedPreferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val isFirstTime = sharedPreferences.getBoolean("isFirstTime", true)

        if (!isFirstTime) {
            startActivity(Intent(this,LoginActivity::class.java))
            finish()
        }
        val editor = sharedPreferences.edit()
        binding.btnStart.setOnClickListener{
            editor.putBoolean("isFirstTime", false)
            editor.apply()
            startActivity(Intent(this,LoginActivity::class.java))
            finish()
        }
    }
}