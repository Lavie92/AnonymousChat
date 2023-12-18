package com.example.doan_chuyennganh.voice_chat

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.doan_chuyennganh.R
import com.google.firebase.auth.FirebaseAuth

class WelcomeActivity : AppCompatActivity() {
    var auth: FirebaseAuth? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_welcome)
        auth = FirebaseAuth.getInstance()
        if (auth!!.currentUser != null) {
            goToNextActivity()
        }
        findViewById<View>(R.id.getStarted).setOnClickListener { goToNextActivity() }
    }

    fun goToNextActivity() {
        startActivity(Intent(this@WelcomeActivity, MainActivity::class.java))
        finish()
    }
}