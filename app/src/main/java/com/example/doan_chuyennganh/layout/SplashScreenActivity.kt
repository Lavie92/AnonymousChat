package com.example.doan_chuyennganh.layout

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.constraintlayout.motion.widget.MotionLayout
import com.example.doan_chuyennganh.LoginActivity
import com.example.doan_chuyennganh.MainActivity
import com.example.doan_chuyennganh.R
import com.example.doan_chuyennganh.authentication.BannedActivity
import com.example.doan_chuyennganh.chat.ChatActivity
import com.example.doan_chuyennganh.chat.ChatNearestActivity
import com.example.doan_chuyennganh.chat.CountryMatchingActivity
import com.example.doan_chuyennganh.chatAI.ChatbotActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.util.UUID

class SplashScreenActivity : AppCompatActivity() {
    private  lateinit var databaseReferences: DatabaseReference
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash_screen)
        auth = FirebaseAuth.getInstance()


        Handler().postDelayed({
            val intent = intent
            if (intent != null) {
                val sourceActivity = intent.getStringExtra("source_activity")
                if (sourceActivity != null && sourceActivity == "toMain") {
                    databaseReferences = FirebaseDatabase.getInstance().getReference("users")


                    if (auth.currentUser != null) {
                        databaseReferences.child(auth.currentUser?.uid!!).get()
                            .addOnSuccessListener { snapshot ->
                                if (snapshot.exists()) {
                                    val point = snapshot.child("point").value
                                    if(point.toString().toInt() <= 0){
                                        startActivity(Intent(this@SplashScreenActivity,BannedActivity::class.java))
                                        finish()
                                    }else{
                                        val loginIntent = Intent(this@SplashScreenActivity, MainActivity::class.java)
                                        startActivity(loginIntent)
                                        finish()
                                    }
                                }
                            }
                    }


                } else if (sourceActivity != null && sourceActivity == "toChatRandom") {

                    val chatIntent = Intent(this@SplashScreenActivity, ChatActivity::class.java)
                    startActivity(chatIntent)
                    finish()
                } else if (sourceActivity != null && sourceActivity == "toChatAI") {

                    val chatIntent = Intent(this@SplashScreenActivity, ChatbotActivity::class.java)
                    startActivity(chatIntent)
                    finish()
                } else if (sourceActivity != null && sourceActivity == "toChatNearest") {

                    val chatIntent = Intent(this@SplashScreenActivity, ChatNearestActivity::class.java)
                    startActivity(chatIntent)
                    finish()
                }else if (sourceActivity != null && sourceActivity == "toCountryMatch") {

                    val chatIntent = Intent(this@SplashScreenActivity, CountryMatchingActivity::class.java)
                    startActivity(chatIntent)
                    finish()
                }
            }
        }, 1000)
    }




}