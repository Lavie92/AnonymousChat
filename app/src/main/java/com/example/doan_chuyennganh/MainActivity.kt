package com.example.doan_chuyennganh

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import com.example.doan_chuyennganh.databinding.ActivityMainBinding
import com.facebook.login.widget.LoginButton
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
    private lateinit var mAuth: FirebaseAuth
    private  lateinit var firebaseDatabase: FirebaseDatabase
    private  lateinit var databaseReferences: DatabaseReference
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val userID = Firebase.auth.currentUser
        userID?.let{
            val uid = it.uid
        }



        mAuth = FirebaseAuth.getInstance()

        //Load Screen to check Active
        checkActive()
        //
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso)



        val textView = findViewById<TextView>(R.id.name)

        val auth = Firebase.auth
        val user = auth.currentUser

        if (user != null) {
            val userName = user.displayName
            textView.text = "Welcome, " + userName
        } else {
            // Handle the case where the user is not signed in
        }
        binding.btnSetting.setOnClickListener{
            startActivity(Intent(this, ProfileActivity::class.java))
        }



// Inside onCreate() method
        val signout = findViewById<Button>(R.id.logout_button)
        signout.setOnClickListener {
            FirebaseAuth.getInstance().signOut();
            Firebase.auth.signOut()
            signOutAndStartSignInActivity()
        }



    }
    private fun signOutAndStartSignInActivity() {
        mAuth.signOut()
        FirebaseAuth.getInstance().signOut();
        Firebase.auth.signOut()
        mGoogleSignInClient.signOut().addOnCompleteListener(this) {
            // Optional: Update UI or show a message to the user
            val intent = Intent(this@MainActivity, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun checkActive(){
        //mAuth = FirebaseAuth.getInstance()
        databaseReferences = FirebaseDatabase.getInstance().getReference("users")
        databaseReferences.child(mAuth.currentUser!!.uid).get().addOnSuccessListener {
            if(it.exists()){
                val active = it.child("active").value
                if(active == false){
                    Toast.makeText(this,"Please complete some Information!",Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, ProfileActivity::class.java))
                }
            }else{
                Toast.makeText(this,"Error!",Toast.LENGTH_SHORT).show()

            }

        }

    }


}