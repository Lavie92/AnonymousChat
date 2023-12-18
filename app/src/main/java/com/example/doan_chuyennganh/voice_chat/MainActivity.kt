package com.example.doan_chuyennganh.voice_chat

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.Target
import com.example.doan_chuyennganh.R
import com.example.doan_chuyennganh.authentication.User
import com.example.doan_chuyennganh.databinding.ActivityMain2Binding
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class MainActivity : AppCompatActivity() {
    var binding: ActivityMain2Binding? = null
    var auth: FirebaseAuth? = null
    var database: FirebaseDatabase? = null
    private var coins: Long = 500
    private var permissions = arrayOf(Manifest.permission.CAMERA, Manifest.permission.RECORD_AUDIO)
    private val requestCode = 1
    var user: User? = null
//    var progress: KProgressHUD? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMain2Binding.inflate(layoutInflater)
        setContentView(binding!!.root)
//        MobileAds.initialize(this, object : OnInitializationCompleteListener() {
//            fun onInitializationComplete(initializationStatus: InitializationStatus?) {}
//        })
//        progress = KProgressHUD.create(this)
//        progress.setDimAmount(0.5f)
//        progress.show()
        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()
        val currentUser = auth!!.currentUser
        database!!.reference.child("users")
            .child(currentUser!!.uid)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
//                    progress.dismiss()
                    user = snapshot.getValue(User::class.java)
                    if (user != null) {
                        val coins = user!!.coins
                        binding!!.coins.text = getString(R.string.coins_display, coins)
//                        Glide.with(this@MainActivity)
//                            .load(user.getProfile())
//                            .into<Target<Drawable>>(binding!!.profilePicture)
                    } else {
                        // Handle the case where user is null
                        // For example, show an error message or log the error
                        Log.e("MainActivity", "User data is null.")
                        Toast.makeText(
                            this@MainActivity,
                            "Error: User data not found.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
        binding!!.findButton.setOnClickListener {
            if (isPermissionsGranted()) {
                database!!.reference.child("users")
                    .child(currentUser.uid)
                    .child("coins")
                    .setValue(coins)
                val intent = Intent(this@MainActivity, ConnectingActivity::class.java)
//                intent.putExtra("profile", user.getProfile())
                startActivity(intent)
                startActivity(Intent(this@MainActivity, ConnectingActivity::class.java))
            } else {
                Toast.makeText(this@MainActivity, "Insufficient Coins", Toast.LENGTH_SHORT).show()
            }
            askPermissions()
        }
        binding!!.rewardBtn.setOnClickListener {
//            startActivity(
//                Intent(
//                    this@MainActivity,
//                    RewardActivity::class.java
//                )
//            )
        }
    }

    private fun askPermissions() {
        ActivityCompat.requestPermissions(this, permissions, requestCode)
    }

    private fun isPermissionsGranted(): Boolean {
        for (permission in permissions) {
            if (ActivityCompat.checkSelfPermission(
                    this,
                    permission
                ) != PackageManager.PERMISSION_GRANTED
            ) return false
        }
        return true
    }

}