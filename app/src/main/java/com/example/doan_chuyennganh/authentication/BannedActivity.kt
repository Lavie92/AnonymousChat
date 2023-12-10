package com.example.doan_chuyennganh.authentication

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.example.doan_chuyennganh.LoginActivity
import com.example.doan_chuyennganh.R
import com.example.doan_chuyennganh.databinding.ActivityBannedBinding
import com.example.doan_chuyennganh.databinding.ActivitySettingBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class BannedActivity : AppCompatActivity() {
    private lateinit var binding: ActivityBannedBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityBannedBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        binding.btnLogout.setOnClickListener{
            signOutAndStartSignInActivity()
        }
    }
    private fun signOutAndStartSignInActivity() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
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