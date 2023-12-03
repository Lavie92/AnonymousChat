package com.example.doan_chuyennganh.authentication

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.example.doan_chuyennganh.LoginActivity
import com.example.doan_chuyennganh.R
import com.example.doan_chuyennganh.databinding.ActivityBannedBinding
import com.example.doan_chuyennganh.databinding.ActivitySettingBinding
import com.google.firebase.auth.FirebaseAuth

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
}