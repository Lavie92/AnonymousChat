package com.example.doan_chuyennganh

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import com.example.doan_chuyennganh.databinding.ActivityMainBinding
import com.facebook.login.widget.LoginButton
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var mGoogleSignInClient: GoogleSignInClient
    private lateinit var mAuth: FirebaseAuth
    private var ivStartChat: ImageView? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        val textView = findViewById<TextView>(R.id.name)

        val auth = Firebase.auth
        val user = auth.currentUser
        ivStartChat = binding.ivStartChat
        ivStartChat?.setOnClickListener {
            val intent = Intent(this, ChatActivity::class.java)
            startActivity(intent)
        }
        mAuth = FirebaseAuth.getInstance()


        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id)).requestEmail().build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)





        if (user != null) {
            val userName = user.displayName
            textView.text = "Welcome, " + userName
        } else {
            // Handle the case where the user is not signed in
        }


// Inside onCreate() method
        val signout = findViewById<Button>(R.id.logout_button)
        signout.setOnClickListener {
            signOutAndStartSignInActivity()
        }


    }

    private fun signOutAndStartSignInActivity() {
        mAuth.signOut()
        Firebase.auth.signOut()
        mGoogleSignInClient.signOut().addOnCompleteListener(this) {
            // Optional: Update UI or show a message to the user
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }
}