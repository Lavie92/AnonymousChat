package com.example.doan_chuyennganh.voice_chat

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.example.doan_chuyennganh.R
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.textfield.TextInputLayout
import com.tencent.mmkv.MMKV
import java.util.Locale

class VoiceChatActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_voice_chat)
        MMKV.initialize(this)
        val userIDInput = findViewById<TextInputLayout>(R.id.user_id)
        val userNameInput = findViewById<TextInputLayout>(R.id.user_name)
        val userLogin = findViewById<Button>(R.id.user_login)
        userIDInput.editText!!.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable) {
                userNameInput.editText!!.setText(
                    s.toString() + "_" + Build.MANUFACTURER.lowercase(
                        Locale.getDefault()
                    )
                )
            }
        })
        userIDInput.editText!!.setText(Build.MANUFACTURER.lowercase(Locale.getDefault()))
        userLogin.setOnClickListener { v: View? ->
            val userID = userIDInput.editText!!.text.toString()
            val userName = userNameInput.editText!!.text.toString()
            signIn(userID, userName)
        }
    }

    private fun signIn(userID: String, userName: String) {
        if (TextUtils.isEmpty(userID) || TextUtils.isEmpty(userName)) {
            return
        }
        val progress = findViewById<CircularProgressIndicator>(R.id.progress_circular)
        progress.visibility = View.VISIBLE
        val fakeLoginProcess = Handler(Looper.getMainLooper())
        fakeLoginProcess.postDelayed(Runnable {
            progress.visibility = View.GONE
            // fake login success
            MMKV.defaultMMKV().putString("user_id", userID)
            MMKV.defaultMMKV().putString("user_name", userName)
            val intent = Intent(this@VoiceChatActivity, MainActivity::class.java)
            intent.putExtra("userID", userID)
            intent.putExtra("userName", userName)
            startActivity(intent)
        }, 1000)
    }
}