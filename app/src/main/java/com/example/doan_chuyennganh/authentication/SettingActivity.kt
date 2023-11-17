package com.example.doan_chuyennganh.authentication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.example.doan_chuyennganh.LoginActivity
import com.example.doan_chuyennganh.ProfileActivity
import com.example.doan_chuyennganh.R
import com.example.doan_chuyennganh.databinding.ActivityMainBinding
import com.example.doan_chuyennganh.databinding.ActivitySettingBinding
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class SettingActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingBinding
    private lateinit var auth: FirebaseAuth
    private lateinit var mGoogleSignInClient: GoogleSignInClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        binding.logoutButton.setOnClickListener{
            signOutAndStartSignInActivity()
        }
    }
    private fun signOutAndStartSignInActivity() {
        auth.signOut()
        startActivity(Intent(this, LoginActivity::class.java))


    }
}