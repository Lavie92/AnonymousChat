package com.example.anonymousChat.layout

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.anonymousChat.LoginActivity
import com.example.anonymousChat.databinding.ActivityStartBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

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

    //Session check
    override fun onDestroy() {
        super.onDestroy()
        removeSession()
    }

    private fun removeSession() {
        // Get the session ID
        val sessionId = getSessionId()

        // Remove from SharedPreferences
        val sharedPref = getSharedPreferences("PreSession", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            remove("sessionID")
            apply()
        }

        // Remove from Firebase
        val user = FirebaseAuth.getInstance().currentUser
        user?.let {
            val databaseReference = FirebaseDatabase.getInstance().getReference("users")
            databaseReference.child(it.uid).child("sessions").child(sessionId).removeValue()
        }
    }

    private fun getSessionId(): String {
        val sharedPref = getSharedPreferences("PreSession", Context.MODE_PRIVATE)
        return sharedPref.getString("sessionID", "") ?: ""
    }
    //end Session check
}