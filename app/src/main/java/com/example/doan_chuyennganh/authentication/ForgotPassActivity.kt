package com.example.doan_chuyennganh.authentication

import android.content.ContentValues.TAG
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import com.example.doan_chuyennganh.R
import com.example.doan_chuyennganh.databinding.ActivityForgotPassBinding
import com.example.doan_chuyennganh.databinding.ActivityLoginBinding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class ForgotPassActivity : AppCompatActivity() {
    private lateinit var  binding: ActivityForgotPassBinding
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPassBinding.inflate(layoutInflater)
        setContentView(binding.root)
        auth = FirebaseAuth.getInstance()

        binding.btnSubmit.setOnClickListener{
            val email = binding.forgotEmail.text.toString()
            if(email!= null){
                Firebase.auth.sendPasswordResetEmail(email)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Toast.makeText(this, "Email send!", Toast.LENGTH_SHORT).show()
                        }
                    }
            }
        }
        binding.btnBack.setOnClickListener{
            onBackPressed()
        }
    }
}