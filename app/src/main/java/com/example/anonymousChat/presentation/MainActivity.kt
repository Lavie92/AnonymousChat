package com.example.anonymousChat.presentation

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import com.example.anonymousChat.R
import com.example.anonymousChat.databinding.ActivityMainBinding
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var auth: FirebaseAuth
    private  lateinit var databaseReferences: DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        val auth = Firebase.auth
        val user = auth.currentUser

        binding.layoutRandomChat.setOnClickListener {
            val splashIntent = Intent(this@MainActivity, SplashScreenActivity::class.java)
            splashIntent.putExtra("source_activity", "toChatRandom")
            startActivity(splashIntent)
        }
        binding.layoutNearestChat.setOnClickListener {
            val splashIntent = Intent(this@MainActivity, SplashScreenActivity::class.java)
            splashIntent.putExtra("source_activity", "toChatNearest")
            startActivity(splashIntent)
        }
        binding.layoutTicTacToe.setOnClickListener{
            val splashIntent = Intent(this@MainActivity, SplashScreenActivity::class.java)
            splashIntent.putExtra("source_activity", "toGame")
            startActivity(splashIntent)
        }

        this.auth = FirebaseAuth.getInstance()

        databaseReferences = FirebaseDatabase.getInstance().getReference("users")
        databaseReferences.child(auth.currentUser!!.uid).get().addOnSuccessListener {
            if(it.exists()){
                val userCoins = it.child("coins").value
                binding.txtCoins.text = userCoins.toString()

            }else{
                Toast.makeText(this,"Error!",Toast.LENGTH_SHORT).show()

            }

        }
        checkActive()



        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)

        if (user != null) {


        } else {
        }
        binding.btnSetting.setOnClickListener{
            startActivity(Intent(this, ProfileActivity::class.java))
        }



    }

    override fun onResume() {
        super.onResume()
        if (auth.currentUser == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish() // Optional: Finish the current activity to prevent going back to it
        }
    }
    override fun onBackPressed() {
        // Kiểm tra xem người dùng đang ở MainActivity hay không
        if (isTaskRoot) {
            // Nếu là MainActivity, tắt ứng dụng khi ấn nút Back lần 2
            if (backPressedOnce) {
                super.onBackPressed()
                return
            }

            backPressedOnce = true
            Toast.makeText(this, "Press back again to exit", Toast.LENGTH_SHORT).show()

            // Thiết lập một đồng hồ đếm để đặt lại trạng thái backPressedOnce sau một khoảng thời gian
            Handler().postDelayed({ backPressedOnce = false }, BACK_PRESS_INTERVAL.toLong())
        } else {
            // Nếu không phải MainActivity, thực hiện hành động mặc định khi ấn nút Back
            super.onBackPressed()
        }
    }

    private fun checkActive(){
        databaseReferences = FirebaseDatabase.getInstance().getReference("users")
        databaseReferences.child(auth.currentUser!!.uid).get().addOnSuccessListener {
            if(it.exists()){
                val active = it.child("active").value
                if(active == false){
                    startActivity(Intent(this, ProfileActivity::class.java))
                }
            }else{
                Toast.makeText(this,"Error!",Toast.LENGTH_SHORT).show()

            }

        }

    }

    companion object {
        private const val BACK_PRESS_INTERVAL = 2000 // Thời gian giữa hai lần ấn nút Back để thoát (2 giây)
        private var backPressedOnce = false
    }
}