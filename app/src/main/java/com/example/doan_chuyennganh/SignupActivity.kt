package com.example.doan_chuyennganh

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.doan_chuyennganh.databinding.ActivityForgotPassBinding
import com.example.doan_chuyennganh.databinding.ActivitySignupBinding
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SignupActivity : AppCompatActivity() {
    private lateinit var  binding: ActivitySignupBinding
    private  lateinit var firebaseDatabase: FirebaseDatabase
    private  lateinit var databaseReferences: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySignupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        firebaseDatabase = FirebaseDatabase.getInstance()
        databaseReferences = firebaseDatabase.reference.child("users")

        binding.btnSignup.setOnClickListener{
            val signupUsername = binding.edtUsername.text.toString()
            val signupEmail = binding.edtEmail.text.toString()
            val signupPassword = binding.edtPassword.text.toString()
            val signupConfirm = binding.edtConfirmPassword.text.toString()

            if(signupEmail.isNotEmpty() && signupPassword.isNotEmpty()){
                signupUser(signupEmail,signupPassword)
            }else{
                Toast.makeText(this,"All fields are mandatory", Toast.LENGTH_SHORT)

            }
        }

        binding.textLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }
    private fun signupUser(email: String, password: String){
        databaseReferences.orderByChild("username").equalTo(email).addListenerForSingleValueEvent(object : ValueEventListener{
            override fun onDataChange(snapshot: DataSnapshot) {
                if(!snapshot.exists()){
                    val id = databaseReferences.push().key
                    val userData = UserData(id, email, password)
                    databaseReferences.child(id!!).setValue(userData)
                    Toast.makeText(this@SignupActivity,"Signup Successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this@SignupActivity, LoginActivity::class.java))
                    finish()
                }else{
                    Toast.makeText(this@SignupActivity,"User already exist!", Toast.LENGTH_SHORT).show()

                }
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@SignupActivity,"Database Error: ${error.message}", Toast.LENGTH_SHORT).show()

            }
        })
    }
}
